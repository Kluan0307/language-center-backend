package com.example.academic.enrollment;

import com.example.academic.branch.BranchService;
import com.example.academic.common.*;
import com.example.academic.schedule.*;
import com.example.academic.schoolclass.*;
import com.example.academic.security.*;
import com.example.academic.student.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class EnrollmentService {
  private final com.example.academic.results.ResultBarrierService resultBarrier;
  private final EnrollmentRepository repository;
  private final com.example.academic.placement.PlacementService placement;
  private final com.example.academic.course.CourseService courseService;
  private final com.example.academic.lifecycle.LearningHistory history;
  private final BranchScope scope;
  private final BranchService branches;
  private final SchoolClassService classes;
  private final StudentService students;
  private final ScheduleRepository schedules;
  private final ApplicationEventPublisher events;
  private final EntityManager entityManager;
  private final EnrollmentPeriodRepository periods;
  private final RemoteApi remote;
  private final com.example.academic.results.CompletionBridge completion;
  private final com.example.academic.session.SessionPlanner sessionPlanner;

  public Enrollment require(Long id) {
    Enrollment e = repository.findById(id).orElseThrow(ApiException::missing);
    scope.check(e.getSchoolClass().getBranchId());
    return e;
  }

  @Transactional(readOnly = true)
  public PageResponse<EnrollmentDto> list(
      int page,
      int size,
      String keyword,
      Enrollment.StudyStatus studyStatus,
      Long classId,
      Long studentId,
      Long branchId) {
    if (classId != null) classes.require(classId);
    if (studentId != null) students.require(studentId);
    var pageData =
        repository.findAll(
            Query.<Enrollment>eq("schoolClass.branchId", scope.filter(branchId))
                .and(
                    Query.keyword(
                        keyword,
                        "student.fullName",
                        "student.phone",
                        "schoolClass.code",
                        "schoolClass.name"))
                .and(Query.eq("studyStatus", studyStatus))
                .and(Query.eq("schoolClass.id", classId))
                .and(Query.eq("student.id", studentId)),
            Query.page(page, size));
    Set<Long> rootIds = new HashSet<>();
    for (Enrollment e : pageData.getContent())
      if (e.getBillingEnrollmentId() != null && !e.getBillingEnrollmentId().equals(e.getId()))
        rootIds.add(e.getBillingEnrollmentId());
    Map<Long, Enrollment> roots = new HashMap<>();
    repository.findAllById(rootIds).forEach(e -> roots.put(e.getId(), e));
    return PageResponse.of(
        pageData.map(e -> EnrollmentDto.of(e, roots.getOrDefault(e.getBillingEnrollmentId(), e))));
  }

  @Transactional(readOnly = true)
  public EnrollmentDto get(Long id) {
    return dto(require(id));
  }

  private EnrollmentDto dto(Enrollment e) {
    Enrollment root =
        e.getBillingEnrollmentId() != null && !e.getBillingEnrollmentId().equals(e.getId())
            ? require(e.getBillingEnrollmentId())
            : e;
    return EnrollmentDto.of(e, root);
  }

  private Enrollment create(
      Student student, SchoolClass target, Enrollment previous, String reason) {
    Enrollment e = new Enrollment();
    e.setStudent(student);
    e.setSchoolClass(target);
    e.setTuitionFee(
        previous == null ? target.getCourse().getTuitionFee() : previous.getTuitionFee());
    e.setRegisteredBy(Users.current().accountId());
    e.setRegisteredByName(Users.current().username());
    repository.saveAndFlush(e);
    e.setBillingEnrollmentId(
        previous == null
            ? e.getId()
            : (previous.getBillingEnrollmentId() == null
                ? previous.getId()
                : previous.getBillingEnrollmentId()));
    openPeriod(e);
    history.record(
        student,
        e,
        previous == null ? null : previous.getId(),
        previous == null ? "ENROLLED" : "TRANSFER_IN",
        null,
        "STUDYING",
        reason,
        java.time.LocalDate.now(BusinessTime.ZONE).isBefore(target.getStartDate())
            ? target.getStartDate()
            : java.time.LocalDate.now(BusinessTime.ZONE),
        null);
    refresh(target);
    if (previous == null) events.publishEvent(new EnrollmentCreated(e.getId(), remote.token()));
    return e;
  }

  private void refresh(SchoolClass c) {
    repository.flush();
    c.setCurrentSize(
        Math.toIntExact(
            repository.countBySchoolClassIdAndStudyStatus(
                c.getId(), Enrollment.StudyStatus.STUDYING)));
    if (c.effectiveStatus() == SchoolClass.Status.FINISHED) return;
    if (c.getCurrentSize() >= c.getMaxSize()) c.setStatus(SchoolClass.Status.FULL);
    else if (c.getStatus() == SchoolClass.Status.FULL) {
      c.setStatus(c.dateStatus());
      c.setStatusManuallySet(false);
    }
  }

  public EnrollmentDto enroll(EnrollmentDto.Request r) {
    SchoolClass initial = classes.require(r.classId());
    branches.lock(initial.getBranchId());
    Student student = students.lock(r.studentId());
    SchoolClass target = classes.lock(r.classId());
    var context =
        placement.context(
            student.getId(),
            target.getId(),
            com.example.academic.placement.PlacementDto.Intent.ENROLL,
            null);
    var replay = placement.replay(r.approvalId(), context);
    if (replay != null) return dto(require(replay));
    courseService.lockForBranch(target.getCourse().getId(), target.getBranchId());
    var preview = placement.authorize(student, target, context, r.approvalId());
    resultBarrier.mutable(target.getId(), target.getBranchId());
    var created = create(student, target, null, "Ghi danh lớp học");
    placement.capture(created, preview, r.approvalId());
    return dto(created);
  }

  public EnrollmentDto status(Long id, EnrollmentDto.StatusRequest request) {
    var status = request.studyStatus();
    var reason = com.example.academic.lifecycle.LearningHistory.reason(request.reason());
    Enrollment e = require(id);
    branches.lock(e.getSchoolClass().getBranchId());
    students.lock(e.getStudent().getId());
    entityManager.refresh(e, LockModeType.PESSIMISTIC_WRITE);
    SchoolClass c = classes.lock(e.getSchoolClass().getId());
    if (status == Enrollment.StudyStatus.STUDYING && request.approvalId() != null) {
      var replay =
          placement.replay(
              request.approvalId(),
              placement.context(
                  e.getStudent().getId(),
                  c.getId(),
                  com.example.academic.placement.PlacementDto.Intent.RESUME,
                  e.getId()));
      if (replay != null) return dto(require(replay));
    }
    if (status != Enrollment.StudyStatus.STUDYING && request.approvalId() != null)
      throw ApiException.bad("Ngoại lệ chỉ dùng khi học lại");
    if (request.version() == null || request.version() != e.getVersion())
      throw ApiException.conflict("Đăng ký đã thay đổi; hãy tải lại");
    if (status == Enrollment.StudyStatus.TRANSFERRED)
      throw ApiException.bad("Use transfer endpoint");
    if (status == Enrollment.StudyStatus.COMPLETED) {
      completion.validate(e, request.finalResultId(), request.finalResultRevision());
      if (e.getStudyStatus() != Enrollment.StudyStatus.STUDYING
          && e.getStudyStatus() != Enrollment.StudyStatus.COMPLETED)
        throw ApiException.conflict("Chỉ hoàn thành đăng ký đang học");
      boolean changed =
          !Objects.equals(e.getCompletionResultId(), request.finalResultId())
              || !Objects.equals(e.getCompletionResultRevision(), request.finalResultRevision());
      e.setCompletionResultId(request.finalResultId());
      e.setCompletionResultRevision(request.finalResultRevision());
      if (changed && e.getStudyStatus() == Enrollment.StudyStatus.COMPLETED) {
        history.record(
            e.getStudent(),
            e,
            null,
            "COMPLETION_RESULT_REFRESHED",
            "COMPLETED",
            "COMPLETED",
            reason,
            java.time.LocalDate.now(BusinessTime.ZONE),
            null);
        repository.flush();
      }
    }
    if (e.getStudyStatus() == status) {
      if (status == Enrollment.StudyStatus.RESERVED) {
        if (request.expectedReturnDate() == null
            || !request.expectedReturnDate().isAfter(java.time.LocalDate.now(BusinessTime.ZONE)))
          throw ApiException.bad("Chọn ngày dự kiến học lại sau hôm nay");
        if (!java.util.Objects.equals(e.getExpectedReturnDate(), request.expectedReturnDate())) {
          e.setExpectedReturnDate(request.expectedReturnDate());
          history.record(
              e.getStudent(),
              e,
              null,
              "RESERVATION_UPDATED",
              "RESERVED",
              "RESERVED",
              reason,
              java.time.LocalDate.now(BusinessTime.ZONE),
              request.expectedReturnDate());
          repository.flush();
        }
      }
      return dto(e);
    }
    if (e.getStudyStatus() != Enrollment.StudyStatus.STUDYING
        && !(e.getStudyStatus() == Enrollment.StudyStatus.RESERVED
            && (status == Enrollment.StudyStatus.STUDYING
                || status == Enrollment.StudyStatus.DROPPED)))
      throw ApiException.conflict("Terminal enrollment cannot be reopened");
    com.example.academic.placement.PlacementDto.Preview accepted = null;
    if (e.getStudyStatus() != status && status == Enrollment.StudyStatus.STUDYING) {
      courseService.lockForBranch(c.getCourse().getId(), c.getBranchId());
      accepted =
          placement.authorize(
              e.getStudent(),
              c,
              placement.context(
                  e.getStudent().getId(),
                  c.getId(),
                  com.example.academic.placement.PlacementDto.Intent.RESUME,
                  e.getId()),
              request.approvalId());
    }
    if (status == Enrollment.StudyStatus.RESERVED
        && (request.expectedReturnDate() == null
            || !request.expectedReturnDate().isAfter(java.time.LocalDate.now(BusinessTime.ZONE))))
      throw ApiException.bad("Chọn ngày dự kiến học lại sau hôm nay");
    if (status == Enrollment.StudyStatus.COMPLETED
        && java.time.LocalDate.now(BusinessTime.ZONE).isBefore(c.getStartDate()))
      throw ApiException.bad("Không hoàn thành đăng ký trước ngày bắt đầu lớp");
    if (status != Enrollment.StudyStatus.COMPLETED)
      resultBarrier.mutable(c.getId(), c.getBranchId());
    var old = e.getStudyStatus();
    if (status == Enrollment.StudyStatus.STUDYING) openPeriod(e);
    else closePeriod(e, status == Enrollment.StudyStatus.COMPLETED);
    e.setStudyStatus(status);
    e.setReservedSince(
        status == Enrollment.StudyStatus.RESERVED
            ? java.time.LocalDate.now(BusinessTime.ZONE)
            : null);
    e.setExpectedReturnDate(
        status == Enrollment.StudyStatus.RESERVED ? request.expectedReturnDate() : null);
    history.record(
        e.getStudent(),
        e,
        null,
        status == Enrollment.StudyStatus.RESERVED
            ? "RESERVED"
            : status == Enrollment.StudyStatus.STUDYING ? "RESUMED" : status.name(),
        old.name(),
        status.name(),
        reason,
        java.time.LocalDate.now(BusinessTime.ZONE),
        e.getExpectedReturnDate());
    repository.flush();
    refresh(c);
    if (accepted != null) placement.capture(e, accepted, request.approvalId());
    return dto(e);
  }

  public EnrollmentDto transfer(Long id, EnrollmentDto.TransferRequest request) {
    var newClassId = request.newClassId();
    var reason = com.example.academic.lifecycle.LearningHistory.reason(request.reason());
    Enrollment e = require(id);
    SchoolClass target = classes.require(newClassId);
    if (!e.getSchoolClass().getBranchId().equals(target.getBranchId()))
      throw ApiException.bad("Không thể chuyển lớp khác chi nhánh");
    branches.lock(target.getBranchId());
    Student student = students.lock(e.getStudent().getId());
    entityManager.refresh(e, LockModeType.PESSIMISTIC_WRITE);
    var context =
        placement.context(
            student.getId(),
            target.getId(),
            com.example.academic.placement.PlacementDto.Intent.TRANSFER,
            e.getId());
    var replay = placement.replay(request.approvalId(), context);
    if (replay != null) return dto(require(replay));
    if (request.version() == null || request.version() != e.getVersion())
      throw ApiException.conflict("Đăng ký đã thay đổi; hãy tải lại");
    if (e.getStudyStatus() != Enrollment.StudyStatus.STUDYING
        && e.getStudyStatus() != Enrollment.StudyStatus.RESERVED)
      throw ApiException.bad("Chỉ chuyển đăng ký đang học hoặc bảo lưu");
    if (e.getSchoolClass().getId().equals(newClassId))
      throw ApiException.bad("Lớp đích phải khác lớp hiện tại");
    SchoolClass old = classes.lock(e.getSchoolClass().getId());
    target = classes.lock(newClassId);
    if (!old.getCourse().getId().equals(target.getCourse().getId()))
      throw ApiException.conflict("Cross-course transfer requires a tuition adjustment workflow");
    courseService.lockForBranch(target.getCourse().getId(), target.getBranchId());
    var accepted = placement.authorize(student, target, context, request.approvalId());
    resultBarrier.mutable(old.getId(), old.getBranchId());
    resultBarrier.mutable(target.getId(), target.getBranchId());
    var oldStatus = e.getStudyStatus();
    e.setStudyStatus(Enrollment.StudyStatus.TRANSFERRED);
    e.setReservedSince(null);
    e.setExpectedReturnDate(null);
    closePeriod(e);
    repository.flush();
    Enrollment created = create(student, target, e, reason);
    placement.capture(created, accepted, request.approvalId());
    history.record(
        student,
        e,
        created.getId(),
        "TRANSFER_OUT",
        oldStatus.name(),
        "TRANSFERRED",
        reason,
        java.time.LocalDate.now(BusinessTime.ZONE),
        null);
    refresh(old);
    return dto(created);
  }

  private void openPeriod(Enrollment e) {
    EnrollmentPeriod period = new EnrollmentPeriod();
    period.setEnrollment(e);
    java.time.LocalDate today =
        java.time.LocalDate.now(com.example.academic.common.BusinessTime.ZONE);
    period.setActiveFrom(
        today.isBefore(e.getSchoolClass().getStartDate())
            ? e.getSchoolClass().getStartDate()
            : today);
    periods.save(period);
  }

  private void closePeriod(Enrollment e) {
    closePeriod(e, false);
  }

  private void closePeriod(Enrollment e, boolean includeToday) {
    for (EnrollmentPeriod period : periods.findByEnrollmentId(e.getId()))
      if (period.getActiveUntil() == null)
        period.setActiveUntil(
            includeToday
                ? laterOfTodayAnd(period.getActiveFrom()).plusDays(1)
                : laterOfTodayAnd(period.getActiveFrom()));
  }

  static java.time.LocalDate laterOfTodayAnd(java.time.LocalDate activeFrom) {
    var today = java.time.LocalDate.now(com.example.academic.common.BusinessTime.ZONE);
    return today.isBefore(activeFrom) ? activeFrom : today;
  }

  @Transactional(readOnly = true)
  public EnrollmentBilling billing(Long id) {
    Enrollment e = require(id);
    if (e.getBillingEnrollmentId() != null && !e.getBillingEnrollmentId().equals(e.getId()))
      e = require(e.getBillingEnrollmentId());
    return EnrollmentBilling.of(e);
  }
}
