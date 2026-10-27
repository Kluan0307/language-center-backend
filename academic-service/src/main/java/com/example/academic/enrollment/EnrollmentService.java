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
  private final EnrollmentRepository repository;
  private final BranchScope scope;
  private final BranchService branches;
  private final SchoolClassService classes;
  private final StudentService students;
  private final ScheduleRepository schedules;
  private final ApplicationEventPublisher events;
  private final EntityManager entityManager;

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
    return PageResponse.of(
        repository
            .findAll(
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
                Query.page(page, size))
            .map(EnrollmentDto::of));
  }

  @Transactional(readOnly = true)
  public EnrollmentDto get(Long id) {
    return EnrollmentDto.of(require(id));
  }

  private void check(Student student, SchoolClass target, Long excluded) {
    if (!student.getBranchId().equals(target.getBranchId()))
      throw ApiException.bad("Học viên và lớp phải cùng chi nhánh");
    if (target.effectiveStatus() == SchoolClass.Status.FINISHED)
      throw ApiException.bad("Lớp đã kết thúc");
    if (repository.existsByStudentIdAndSchoolClassIdAndStudyStatusAndIdNot(
        student.getId(),
        target.getId(),
        Enrollment.StudyStatus.STUDYING,
        excluded == null ? -1L : excluded))
      throw ApiException.conflict("Học viên đã có đăng ký STUDYING ở lớp này");
    long count =
        repository.countBySchoolClassIdAndStudyStatus(
            target.getId(), Enrollment.StudyStatus.STUDYING);
    if (excluded != null) {
      Enrollment old = repository.findById(excluded).orElseThrow(ApiException::missing);
      if (old.getStudyStatus() == Enrollment.StudyStatus.STUDYING
          && old.getSchoolClass().getId().equals(target.getId())) count--;
    }
    // TODO: bổ sung luồng duyệt vượt sĩ số trong tương lai; hiện tại chặn cứng.
    if (count >= target.getMaxSize()) throw ApiException.bad("Lớp đã đủ sĩ số");
    List<Schedule> candidate = schedules.findBySchoolClassId(target.getId());
    for (Enrollment active :
        repository.findByStudentIdAndStudyStatus(
            student.getId(), Enrollment.StudyStatus.STUDYING)) {
      if (Objects.equals(active.getId(), excluded)
          || active.getSchoolClass().getId().equals(target.getId())) continue;
      SchoolClass other = active.getSchoolClass();
      if (other.effectiveStatus() == SchoolClass.Status.FINISHED
          || !ScheduleConflictService.datesOverlap(target, other)) continue;
      for (Schedule a : candidate)
        for (Schedule b : schedules.findBySchoolClassId(other.getId()))
          if (ScheduleConflictService.overlaps(a, b))
            throw ApiException.conflict(
                "Trùng lịch học viên với lớp " + other.getCode() + " (" + other.getName() + ")");
    }
  }

  private Enrollment create(Student student, SchoolClass target) {
    Enrollment e = new Enrollment();
    e.setStudent(student);
    e.setSchoolClass(target);
    repository.saveAndFlush(e);
    refresh(target);
    events.publishEvent(
        new EnrollmentCreated(
            e.getId(), student.getId(), target.getId(), target.getCourse().getTuitionFee()));
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
    check(student, target, null);
    return EnrollmentDto.of(create(student, target));
  }

  public EnrollmentDto status(Long id, Enrollment.StudyStatus status) {
    Enrollment e = require(id);
    branches.lock(e.getSchoolClass().getBranchId());
    students.lock(e.getStudent().getId());
    entityManager.refresh(e, LockModeType.PESSIMISTIC_WRITE);
    SchoolClass c = classes.lock(e.getSchoolClass().getId());
    if (e.getStudyStatus() != status && status == Enrollment.StudyStatus.STUDYING)
      check(e.getStudent(), c, e.getId());
    e.setStudyStatus(status);
    refresh(c);
    return EnrollmentDto.of(e);
  }

  public EnrollmentDto transfer(Long id, Long newClassId) {
    Enrollment e = require(id);
    SchoolClass target = classes.require(newClassId);
    if (!e.getSchoolClass().getBranchId().equals(target.getBranchId()))
      throw ApiException.bad("Không thể chuyển lớp khác chi nhánh");
    branches.lock(target.getBranchId());
    Student student = students.lock(e.getStudent().getId());
    entityManager.refresh(e, LockModeType.PESSIMISTIC_WRITE);
    if (e.getStudyStatus() != Enrollment.StudyStatus.STUDYING)
      throw ApiException.bad("Chỉ chuyển đăng ký đang STUDYING");
    if (e.getSchoolClass().getId().equals(newClassId))
      throw ApiException.bad("Lớp đích phải khác lớp hiện tại");
    SchoolClass old = classes.lock(e.getSchoolClass().getId());
    target = classes.lock(newClassId);
    check(student, target, e.getId());
    e.setStudyStatus(Enrollment.StudyStatus.TRANSFERRED);
    repository.flush();
    Enrollment created = create(student, target);
    refresh(old);
    return EnrollmentDto.of(created);
  }
}
