package com.example.academic.admission;

import com.example.academic.branch.BranchService;
import com.example.academic.common.*;
import com.example.academic.course.*;
import com.example.academic.schoolclass.*;
import com.example.academic.security.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class AdmissionPolicyService {
  private final CourseService courses;
  private final BranchService branches;
  private final ProgramCatalogService catalog;
  private final CourseAdmissionPolicyRepository policies;
  private final SchoolClassRepository classes;
  private final ClassAdmissionEventRepository changes;
  private final EntityManager em;

  private boolean manager() {
    return Users.current().role() == Role.SUPER_ADMIN || Users.current().role() == Role.ADMIN;
  }

  private boolean canManage(Course c) {
    return manager() && (c.getBranchId() != null || Users.current().role() == Role.SUPER_ADMIN);
  }

  private void manage(Course c) {
    if (!canManage(c)) throw ApiException.forbidden();
  }

  private CourseAdmissionPolicy latest(Long course) {
    return policies.findFirstByCourseIdOrderByRevisionDesc(course).orElse(null);
  }

  public AdmissionDto.Policy dto(CourseAdmissionPolicy p, Long courseId) {
    if (p == null)
      return new AdmissionDto.Policy(
          null,
          courseId,
          0,
          CourseAdmissionPolicy.Mode.UNVERIFIED,
          null,
          null,
          null,
          null,
          List.of(),
          null,
          null,
          null,
          null);
    return new AdmissionDto.Policy(
        p.getId(),
        p.getCourseId(),
        p.getRevision(),
        p.getMode(),
        p.getProgramId(),
        p.getProgramCode(),
        p.getProgramName(),
        new AdmissionDto.Level(
            p.getOutputLevelId(), p.getOutputLevelCode(), p.getOutputLevelName()),
        p.getAcceptedLevels().stream()
            .map(l -> new AdmissionDto.Level(l.getLevelId(), l.getCode(), l.getName()))
            .toList(),
        p.getConfirmedBy(),
        p.getConfirmedByName(),
        p.getConfirmedAt(),
        p.getReason());
  }

  private AdmissionDto.CourseView view(Course c) {
    return new AdmissionDto.CourseView(
        dto(latest(c.getId()), c.getId()),
        canManage(c),
        c.getType(),
        c.getLevel(),
        c.getEntryRequirement());
  }

  @Transactional(readOnly = true)
  public AdmissionDto.CourseView getCourse(Long id) {
    return view(courses.require(id));
  }

  @Transactional(readOnly = true)
  public List<AdmissionDto.Policy> history(Long id) {
    courses.require(id);
    return policies.findByCourseIdOrderByRevisionDesc(id).stream().map(p -> dto(p, id)).toList();
  }

  public AdmissionDto.CourseView confirm(Long id, AdmissionDto.Confirm r) {
    var initial = courses.require(id);
    manage(initial);
    if (initial.getBranchId() != null) branches.lock(initial.getBranchId());
    var c = courses.lockForBranch(id, initial.getBranchId());
    manage(c);
    var old = latest(id);
    ProgramCatalogService.version(r.expectedRevision(), old == null ? 0 : old.getRevision());
    if (r.mode() == null || r.mode() == CourseAdmissionPolicy.Mode.UNVERIFIED)
      throw ApiException.bad("Chọn điều kiện đầu vào đã xác minh");
    var ids = r.acceptedLevelIds();
    if (ids == null || ids.size() > 200 || new HashSet<>(ids).size() != ids.size())
      throw ApiException.bad("Danh sách đầu vào không hợp lệ hoặc trùng trình độ");
    if (r.mode() == CourseAdmissionPolicy.Mode.OPEN_ENTRY && !ids.isEmpty()
        || r.mode() == CourseAdmissionPolicy.Mode.REQUIRES_ASSESSMENT && ids.isEmpty())
      throw ApiException.bad(
          "Khóa mở không chọn đầu vào; khóa cần đánh giá phải chọn ít nhất một trình độ");
    var program = catalog.lock(r.programId());
    catalog.active(program);
    var output = catalog.level(r.outputLevelId(), program.getId(), true);
    var p = new CourseAdmissionPolicy();
    p.setCourseId(id);
    p.setRevision(old == null ? 1 : Math.addExact(old.getRevision(), 1));
    p.setMode(r.mode());
    p.setProgramId(program.getId());
    p.setProgramCode(program.getCode());
    p.setProgramName(program.getName());
    p.setOutputLevelId(output.getId());
    p.setOutputLevelCode(output.getCode());
    p.setOutputLevelName(output.getName());
    for (Long levelId : ids.stream().sorted().toList()) {
      var l = catalog.level(levelId, program.getId(), true);
      p.getAcceptedLevels()
          .add(new CourseAdmissionPolicy.LevelSnapshot(l.getId(), l.getCode(), l.getName()));
    }
    p.setConfirmedBy(Users.current().accountId());
    p.setConfirmedByName(Users.current().username());
    p.setConfirmedAt(LocalDateTime.now(BusinessTime.ZONE));
    p.setReason(reason(r.reason()));
    policies.saveAndFlush(p);
    return view(c);
  }

  private String reason(String reason) {
    if (reason == null || reason.trim().length() < 3 || reason.trim().length() > 500)
      throw ApiException.bad("Nhập lý do từ 3 đến 500 ký tự");
    return reason.trim();
  }

  private SchoolClass requireClass(Long id) {
    var c = classes.findById(id).orElseThrow(ApiException::missing);
    scope(c);
    return c;
  }

  private void scope(SchoolClass c) {
    var u = Users.current();
    if (u.role() != Role.SUPER_ADMIN && !u.branchIds().contains(c.getBranchId()))
      throw ApiException.missing();
    if (u.role() == Role.TEACHER && !Objects.equals(u.profileId(), c.getTeacherId()))
      throw ApiException.missing();
  }

  private AdmissionDto.ClassView classView(SchoolClass c) {
    var p =
        c.getAdmissionPolicyId() == null
            ? null
            : policies.findById(c.getAdmissionPolicyId()).orElseThrow(ApiException::missing);
    return new AdmissionDto.ClassView(
        c.getId(), c.getAdmissionRevision(), dto(p, c.getCourse().getId()), manager());
  }

  @Transactional(readOnly = true)
  public AdmissionDto.ClassView getClass(Long id) {
    return classView(requireClass(id));
  }

  @Transactional(readOnly = true)
  public List<AdmissionDto.Change> classHistory(Long id) {
    requireClass(id);
    return changes.findByClassIdOrderByIdDesc(id).stream()
        .map(
            e ->
                new AdmissionDto.Change(
                    e.getId(),
                    e.getRevision(),
                    e.getOldPolicyId(),
                    e.getNewPolicyId(),
                    e.getActorName(),
                    e.getOccurredAt(),
                    e.getReason()))
        .toList();
  }

  public AdmissionDto.ClassView apply(Long id, AdmissionDto.Apply r) {
    if (!manager()) throw ApiException.forbidden();
    var initial = requireClass(id);
    branches.lock(initial.getBranchId());
    var c = classes.lock(id).orElseThrow(ApiException::missing);
    em.refresh(c, LockModeType.PESSIMISTIC_WRITE);
    scope(c);
    ProgramCatalogService.version(r.expectedRevision(), c.getAdmissionRevision());
    courses.lockForBranch(c.getCourse().getId(), c.getBranchId());
    var p = latest(c.getCourse().getId());
    if (p == null) throw ApiException.bad("Xác minh điều kiện khóa học trước khi áp dụng cho lớp");
    ProgramCatalogService.version(r.expectedCourseRevision(), p.getRevision());
    if (Objects.equals(c.getAdmissionPolicyId(), p.getId())) return classView(c);
    bind(c, p, reason(r.reason()));
    classes.saveAndFlush(c);
    return classView(c);
  }

  // Called only by class creation/course change while branch/class/course locks are already held.
  public void initializeClass(SchoolClass c) {
    bind(c, latest(c.getCourse().getId()), "Lưu điều kiện tại lúc mở lớp hoặc thay khóa học");
  }

  private void bind(SchoolClass c, CourseAdmissionPolicy p, String reason) {
    var old = c.getAdmissionPolicyId();
    c.setAdmissionPolicyId(p == null ? null : p.getId());
    c.setAdmissionRevision(Math.addExact(c.getAdmissionRevision(), 1));
    var e = new ClassAdmissionEvent();
    e.setClassId(c.getId());
    e.setOldPolicyId(old);
    e.setNewPolicyId(c.getAdmissionPolicyId());
    e.setRevision(c.getAdmissionRevision());
    e.setActorId(Users.current().accountId());
    e.setActorName(Users.current().username());
    e.setOccurredAt(LocalDateTime.now(BusinessTime.ZONE));
    e.setReason(reason);
    changes.save(e);
  }

  public void discardDraft(SchoolClass c) {
    changes.deleteAll(changes.findByClassIdOrderByIdDesc(c.getId()));
    changes.flush();
  }
}
