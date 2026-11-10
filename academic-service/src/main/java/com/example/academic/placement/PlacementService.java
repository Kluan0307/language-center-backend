package com.example.academic.placement;

import com.example.academic.admission.*;
import com.example.academic.assessment.*;
import com.example.academic.branch.*;
import com.example.academic.common.ApiException;
import com.example.academic.common.BusinessTime;
import com.example.academic.common.PageResponse;
import com.example.academic.common.Query;
import com.example.academic.course.*;
import com.example.academic.enrollment.*;
import com.example.academic.schoolclass.*;
import com.example.academic.security.*;
import com.example.academic.session.SessionPlanner;
import com.example.academic.student.*;
import jakarta.persistence.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class PlacementService {
  private final StudentService students;
  private final SchoolClassService classes;
  private final SchoolClassRepository classRepository;
  private final EnrollmentRepository enrollments;
  private final BranchService branches;
  private final CourseService courses;
  private final AdmissionPolicyService policies;
  private final CourseAdmissionPolicyRepository policyRepository;
  private final PlacementAssessmentRepository assessments;
  private final SessionPlanner planner;
  private final PlacementExceptionRepository exceptions;
  private final EnrollmentAdmissionRepository admissions;
  private final EntityManager em;

  private void operations() {
    if (!Set.of(Role.SUPER_ADMIN, Role.ADMIN, Role.STAFF).contains(Users.current().role()))
      throw ApiException.forbidden();
  }

  private void manager() {
    operations();
    if (Users.current().role() == Role.STAFF) throw ApiException.forbidden();
  }

  private String reason(String s) {
    if (s == null || s.trim().length() < 3 || s.trim().length() > 500)
      throw ApiException.bad("Nhập lý do từ 3 đến 500 ký tự");
    return s.trim();
  }

  public PlacementDto.Context context(
      Long student, Long target, PlacementDto.Intent intent, Long source) {
    return new PlacementDto.Context(student, target, intent, source);
  }

  @Transactional(readOnly = true)
  public PlacementDto.Preview preview(PlacementDto.Context context) {
    operations();
    var s = students.require(context.studentId());
    var c = classes.require(context.classId());
    return evaluate(s, c, context);
  }

  private void issue(List<PlacementDto.Issue> out, String code, String message) {
    out.add(new PlacementDto.Issue(code, message));
  }

  private Enrollment source(PlacementDto.Context r) {
    if (r.intent() == PlacementDto.Intent.ENROLL) {
      if (r.sourceEnrollmentId() != null)
        throw ApiException.bad("Ghi danh mới không có đăng ký nguồn");
      return null;
    }
    if (r.sourceEnrollmentId() == null) throw ApiException.bad("Thiếu đăng ký nguồn");
    var e = enrollments.findById(r.sourceEnrollmentId()).orElseThrow(ApiException::missing);
    students.require(e.getStudent().getId());
    classes.require(e.getSchoolClass().getId());
    if (!e.getStudent().getId().equals(r.studentId()))
      throw ApiException.bad("Đăng ký nguồn không thuộc học viên");
    return e;
  }

  public PlacementDto.Preview evaluate(Student s, SchoolClass c, PlacementDto.Context r) {
    operations();
    if (!Objects.equals(s.getId(), r.studentId()) || !Objects.equals(c.getId(), r.classId()))
      throw ApiException.bad("Sai ngữ cảnh phân lớp");
    students.require(s.getId());
    classes.require(c.getId());
    var from = source(r);
    var blockers = new ArrayList<PlacementDto.Issue>();
    if (!s.getBranchId().equals(c.getBranchId()))
      issue(blockers, "BRANCH_MISMATCH", "Học viên và lớp phải cùng chi nhánh");
    if (branches.require(c.getBranchId()).getStatus() != Branch.Status.ACTIVE)
      issue(blockers, "BRANCH_INACTIVE", "Chi nhánh ngừng hoạt động");
    if (s.getStatus() != Student.Status.STUDYING)
      issue(blockers, "STUDENT_INACTIVE", "Hồ sơ học viên phải Đang học");
    if (c.getCourse().getStatus() != Course.Status.OPEN)
      issue(blockers, "COURSE_CLOSED", "Khóa học đã đóng");
    if (c.effectiveStatus() == SchoolClass.Status.FINISHED)
      issue(blockers, "CLASS_FINISHED", "Lớp đã kết thúc");
    if (from != null) {
      if (!from.getSchoolClass().getBranchId().equals(c.getBranchId()))
        issue(blockers, "SOURCE_BRANCH", "Không chuyển hoặc học lại khác chi nhánh");
      if (r.intent() == PlacementDto.Intent.TRANSFER) {
        if (from.getSchoolClass().getId().equals(c.getId())
            || !from.getSchoolClass().getCourse().getId().equals(c.getCourse().getId()))
          issue(blockers, "TRANSFER_COURSE", "Lớp đích phải khác và cùng khóa");
        if (!Set.of(Enrollment.StudyStatus.STUDYING, Enrollment.StudyStatus.RESERVED)
            .contains(from.getStudyStatus()))
          issue(blockers, "SOURCE_STATUS", "Chỉ chuyển đăng ký đang học hoặc bảo lưu");
      } else if (!from.getSchoolClass().getId().equals(c.getId())
          || from.getStudyStatus() != Enrollment.StudyStatus.RESERVED)
        issue(blockers, "SOURCE_STATUS", "Chỉ học lại đăng ký đang bảo lưu tại chính lớp này");
    }
    Long excluded = from == null ? -1L : from.getId();
    for (var status : List.of(Enrollment.StudyStatus.STUDYING, Enrollment.StudyStatus.RESERVED))
      if (enrollments.existsByStudentIdAndSchoolClassIdAndStudyStatusAndIdNot(
          s.getId(), c.getId(), status, excluded))
        issue(blockers, "DUPLICATE_ENROLLMENT", "Đã có đăng ký đang học/bảo lưu ở lớp này");
    long count =
        enrollments.countBySchoolClassIdAndStudyStatus(c.getId(), Enrollment.StudyStatus.STUDYING);
    if (from != null
        && from.getStudyStatus() == Enrollment.StudyStatus.STUDYING
        && from.getSchoolClass().getId().equals(c.getId())) count--;
    if (count >= c.getMaxSize()) issue(blockers, "CLASS_FULL", "Lớp đã đủ sĩ số");
    planner
        .enrollmentConflict(s.getId(), c, from == null ? null : from.getId())
        .ifPresent(message -> issue(blockers, "SCHEDULE_CONFLICT", message));
    var policy = policies.getClass(c.getId()).policy();
    var all =
        assessments.findByStudentIdAndStatusOrderByProgramIdAsc(
            s.getId(), PlacementAssessment.Status.CONFIRMED);
    var a =
        all.stream()
            .filter(x -> Objects.equals(x.getProgramId(), policy.programId()))
            .findFirst()
            .orElse(null);
    String academic =
        policy.mode() == CourseAdmissionPolicy.Mode.UNVERIFIED
            ? "UNVERIFIED_POLICY"
            : policy.mode() == CourseAdmissionPolicy.Mode.OPEN_ENTRY
                ? "ELIGIBLE"
                : a == null
                    ? (all.isEmpty() ? "MISSING_ASSESSMENT" : "PROGRAM_MISMATCH")
                    : policy.acceptedLevels().stream()
                            .anyMatch(l -> l.levelId().equals(a.getLevelId()))
                        ? "ELIGIBLE"
                        : "LEVEL_MISMATCH";
    var prior =
        from == null
            ? null
            : admissions.findFirstByEnrollmentIdOrderByIdDesc(from.getId()).orElse(null);
    Long reused = null;
    if (r.intent() == PlacementDto.Intent.RESUME
        && prior != null
        && prior.getClassId().equals(c.getId())
        && prior.getClassRevision() == c.getAdmissionRevision()
        && Objects.equals(prior.getPolicyId(), c.getAdmissionPolicyId())) {
      academic = "REUSED_ACCEPTANCE";
      reused = prior.getId();
    }
    // Stable semantic context; capacity/schedules are always rechecked and never waived.
    String assessmentKey =
        all.stream().map(x -> x.getId() + ":" + x.getVersion()).sorted().toList().toString();
    String fingerprint =
        hash(
            s.getId()
                + "|"
                + s.getVersion()
                + "|"
                + s.getBranchId()
                + "|"
                + s.getStatus()
                + "|"
                + c.getId()
                + "|"
                + c.getBranchId()
                + "|"
                + c.getCourse().getId()
                + "|"
                + c.getStartDate()
                + "|"
                + c.effectiveEndDate()
                + "|"
                + c.getAdmissionRevision()
                + "|"
                + policy.id()
                + "|"
                + assessmentKey
                + "|"
                + r.intent()
                + "|"
                + (from == null
                    ? "none"
                    : from.getId() + ":" + from.getVersion() + ":" + from.getStudyStatus())
                + "|"
                + reused);
    boolean academicOk = Set.of("ELIGIBLE", "REUSED_ACCEPTANCE").contains(academic);
    return new PlacementDto.Preview(
        r,
        c.getCode(),
        c.getName(),
        c.getBranchId(),
        s.getFullName(),
        s.getCurrentLevel(),
        c.getAdmissionRevision(),
        policy,
        a == null ? null : AssessmentDto.of(a),
        academic,
        List.copyOf(blockers),
        blockers.isEmpty() && academicOk,
        blockers.isEmpty() && !academicOk,
        fingerprint,
        from == null ? null : from.getVersion(),
        reused,
        count,
        c.getMaxSize());
  }

  private String hash(String s) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Transactional(readOnly = true)
  public PageResponse<PlacementDto.Preview> suggestions(Long studentId, int page, int size) {
    operations();
    var s = students.require(studentId);
    return PageResponse.of(
        classRepository
            .findAll(Query.<SchoolClass>eq("branchId", s.getBranchId()), Query.page(page, size))
            .map(
                c ->
                    evaluate(
                        s, c, context(studentId, c.getId(), PlacementDto.Intent.ENROLL, null))));
  }

  private void lockedContext(PlacementDto.Context r) {
    operations();
    var c = classes.require(r.classId());
    branches.lock(c.getBranchId());
    students.lock(r.studentId());
    classes.lock(c.getId());
    courses.lockForBranch(c.getCourse().getId(), c.getBranchId());
    if (r.sourceEnrollmentId() != null) {
      var source = source(r);
      em.refresh(source, LockModeType.PESSIMISTIC_WRITE);
    }
  }

  public PlacementDto.ExceptionView request(PlacementDto.ExceptionRequest request) {
    lockedContext(request.context());
    var p = preview(request.context());
    if (!p.fingerprint().equals(request.fingerprint()))
      throw ApiException.conflict("Căn cứ đã đổi; tải lại kiểm tra phù hợp");
    if (!p.canRequestException())
      throw ApiException.conflict("Chỉ đề nghị ngoại lệ học thuật khi không có ràng buộc cứng");
    var e = new PlacementException();
    e.setStudentId(p.context().studentId());
    e.setClassId(p.context().classId());
    e.setBranchId(p.branchId());
    e.setIntent(p.context().intent());
    e.setSourceEnrollmentId(p.context().sourceEnrollmentId());
    e.setFingerprint(p.fingerprint());
    e.setSourceVersion(p.sourceVersion());
    e.setPolicyId(p.policy().id());
    e.setClassRevision(p.classRevision());
    e.setAssessmentId(p.assessment() == null ? null : p.assessment().id());
    e.setAssessmentVersion(p.assessment() == null ? null : p.assessment().version());
    e.setContextSnapshot(p.toString());
    e.setAcademicStatus(p.academicStatus());
    e.setRequestedReason(reason(request.reason()));
    e.setRequestedBy(Users.current().accountId());
    e.setRequestedByName(Users.current().username());
    e.setRequestedAt(LocalDateTime.now(BusinessTime.ZONE));
    exceptions.saveAndFlush(e);
    return view(e);
  }

  private PlacementDto.Context context(PlacementException e) {
    return context(e.getStudentId(), e.getClassId(), e.getIntent(), e.getSourceEnrollmentId());
  }

  private PlacementException require(Long id) {
    operations();
    var e = exceptions.findById(id).orElseThrow(ApiException::missing);
    students.require(e.getStudentId());
    classes.require(e.getClassId());
    return e;
  }

  private boolean stale(PlacementException e) {
    if (e.getStatus() != PlacementException.Status.REQUESTED
        && e.getStatus() != PlacementException.Status.APPROVED) return false;
    return !e.getFingerprint().equals(preview(context(e)).fingerprint());
  }

  private PlacementDto.ExceptionView view(PlacementException e) {
    var p = preview(context(e));
    String status = stale(e) ? "INVALIDATED" : e.getStatus().name();
    return new PlacementDto.ExceptionView(
        e.getId(),
        e.getVersion(),
        context(e),
        e.getBranchId(),
        p.studentName(),
        p.classCode(),
        status,
        e.getStatus().name(),
        e.getFingerprint(),
        e.getAcademicStatus(),
        e.getRequestedReason(),
        e.getRequestedBy(),
        e.getRequestedByName(),
        e.getRequestedAt(),
        e.getDecisionReason(),
        e.getDecidedByName(),
        e.getDecidedAt(),
        e.getConsumedEnrollmentId(),
        e.getConsumedAt(),
        e.getPolicyId(),
        e.getClassRevision(),
        e.getAssessmentId(),
        e.getAssessmentVersion(),
        e.getSourceVersion(),
        policies.dto(
            e.getPolicyId() == null
                ? null
                : policyRepository.findById(e.getPolicyId()).orElseThrow(ApiException::missing),
            classes.require(e.getClassId()).getCourse().getId()),
        e.getAssessmentId() == null
            ? null
            : assessments.findById(e.getAssessmentId()).map(AssessmentDto::of).orElse(null));
  }

  @Transactional(readOnly = true)
  public PlacementDto.ExceptionView get(Long id) {
    return view(require(id));
  }

  @Transactional(readOnly = true)
  public PageResponse<PlacementDto.ExceptionView> list(
      Long studentId, Long classId, Long branchId, int page, int size) {
    operations();
    if (studentId != null) students.require(studentId);
    if (classId != null) classes.require(classId);
    var u = Users.current();
    var spec =
        Query.<PlacementException>eq("studentId", studentId)
            .and(Query.eq("classId", classId))
            .and(Query.eq("branchId", branchId));
    if (branchId != null) new BranchScope().check(branchId);
    if (u.role() != Role.SUPER_ADMIN)
      spec = spec.and((r, q, cb) -> r.get("branchId").in(u.branchIds()));
    return PageResponse.of(exceptions.findAll(spec, Query.page(page, size)).map(this::view));
  }

  public PlacementDto.ExceptionView decide(Long id, PlacementDto.Decision request) {
    manager();
    var e = require(id);
    lockedContext(context(e));
    em.refresh(e, LockModeType.PESSIMISTIC_WRITE);
    if (request.version() == null || request.version() != e.getVersion())
      throw ApiException.conflict("Đề nghị đã thay đổi; tải lại");
    if (e.getStatus() != PlacementException.Status.REQUESTED)
      throw ApiException.conflict("Đề nghị đã được quyết định");
    var why = reason(request.reason());
    if (stale(e)) {
      e.setStatus(PlacementException.Status.INVALIDATED);
      e.setDecisionReason(why);
    } else {
      var p = preview(context(e));
      if (request.approve() && !p.blockers().isEmpty())
        throw ApiException.conflict(
            "Không thể duyệt khi có ràng buộc cứng: " + p.blockers().getFirst().message());
      e.setStatus(
          request.approve()
              ? PlacementException.Status.APPROVED
              : PlacementException.Status.REJECTED);
      e.setDecisionReason(why);
    }
    e.setDecidedBy(Users.current().accountId());
    e.setDecidedByName(Users.current().username());
    e.setDecidedAt(LocalDateTime.now(BusinessTime.ZONE));
    exceptions.saveAndFlush(e);
    return view(e);
  }

  private PlacementException approval(Long id, PlacementDto.Context r) {
    var a = require(id);
    if (!context(a).equals(r))
      throw ApiException.conflict("Xác nhận không thuộc đúng học viên/lớp/thao tác/đăng ký nguồn");
    em.refresh(a, LockModeType.PESSIMISTIC_WRITE);
    return a;
  }

  // Called under enrollment's branch/student locks, before source version and capacity checks for
  // safe retry.
  public Long replay(Long approvalId, PlacementDto.Context r) {
    if (approvalId == null) return null;
    var a = approval(approvalId, r);
    return a.getStatus() == PlacementException.Status.CONSUMED ? a.getConsumedEnrollmentId() : null;
  }

  // Caller holds branch, student, class and course locks. No tuition is emitted before this
  // succeeds.
  public PlacementDto.Preview authorize(
      Student s, SchoolClass c, PlacementDto.Context r, Long approvalId) {
    var p = evaluate(s, c, r);
    if (!p.blockers().isEmpty())
      throw ApiException.conflict(
          p.blockers().getFirst().code() + ": " + p.blockers().getFirst().message());
    if (approvalId != null) {
      var a = approval(approvalId, r);
      if (a.getStatus() != PlacementException.Status.APPROVED
          || !a.getFingerprint().equals(p.fingerprint()))
        throw ApiException.conflict(
            "Xác nhận ngoại lệ chưa duyệt, đã dùng hoặc hết hiệu lực; kiểm tra lại");
    } else if (!p.eligible())
      throw ApiException.conflict(
          p.academicStatus() + ": cần bổ sung đánh giá/điều kiện hoặc quản lý duyệt ngoại lệ");
    return p;
  }

  public void capture(Enrollment e, PlacementDto.Preview p, Long approvalId) {
    var d = new EnrollmentAdmission();
    d.setEnrollmentId(e.getId());
    d.setStudentId(e.getStudent().getId());
    d.setClassId(e.getSchoolClass().getId());
    d.setPolicyId(p.policy().id());
    d.setClassRevision(p.classRevision());
    d.setAssessmentId(p.assessment() == null ? null : p.assessment().id());
    d.setAssessmentVersion(p.assessment() == null ? null : p.assessment().version());
    d.setApprovalId(approvalId);
    d.setReusedDecisionId(p.reusedDecisionId());
    d.setIntent(p.context().intent());
    d.setAcademicStatus(approvalId != null ? "EXCEPTION" : p.academicStatus());
    d.setFingerprint(p.fingerprint());
    d.setPolicySnapshot(p.policy().toString());
    d.setAssessmentSnapshot(p.assessment() == null ? "NONE" : p.assessment().toString());
    d.setActorId(Users.current().accountId());
    d.setActorName(Users.current().username());
    d.setAcceptedAt(LocalDateTime.now(BusinessTime.ZONE));
    if (p.reusedDecisionId() != null) {
      var prior = admissions.findById(p.reusedDecisionId()).orElseThrow(ApiException::missing);
      d.setPolicySnapshot(prior.getPolicySnapshot());
      d.setAssessmentSnapshot(prior.getAssessmentSnapshot());
      d.setAssessmentId(prior.getAssessmentId());
      d.setAssessmentVersion(prior.getAssessmentVersion());
    }
    admissions.saveAndFlush(d);
    if (approvalId != null) {
      var a = approval(approvalId, p.context());
      a.setStatus(PlacementException.Status.CONSUMED);
      a.setConsumedEnrollmentId(e.getId());
      a.setConsumedAt(LocalDateTime.now(BusinessTime.ZONE));
      exceptions.saveAndFlush(a);
    }
  }

  @Transactional(readOnly = true)
  public List<EnrollmentAdmission> history(Long id) {
    operations();
    var e = enrollments.findById(id).orElseThrow(ApiException::missing);
    students.require(e.getStudent().getId());
    classes.require(e.getSchoolClass().getId());
    return admissions.findByEnrollmentIdOrderByIdDesc(id);
  }
}
