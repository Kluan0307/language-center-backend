package com.example.academic.duration;

import com.example.academic.branch.BranchService;
import com.example.academic.common.*;
import com.example.academic.course.*;
import com.example.academic.schoolclass.*;
import com.example.academic.security.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class DurationPlanService {
  private final com.example.academic.results.ResultBarrierService resultBarrier;
  private final CourseRepository courses;
  private final SchoolClassRepository classes;
  private final CourseDurationPlanRepository coursePlans;
  private final ClassDurationPlanRepository classPlans;
  private final BranchService branches;
  private final BranchScope scope;
  private final EntityManager em;

  private boolean manager() {
    return Set.of(Role.SUPER_ADMIN, Role.ADMIN).contains(Users.current().role());
  }

  private boolean canManage(Course c) {
    return manager() && (c.getBranchId() != null || Users.current().role() == Role.SUPER_ADMIN);
  }

  private Course course(Long id) {
    var c = courses.findById(id).orElseThrow(ApiException::missing);
    scope.course(c.getBranchId());
    return c;
  }

  public SchoolClass school(Long id) {
    var c = classes.findById(id).orElseThrow(ApiException::missing);
    scope.check(c.getBranchId());
    scope.teacher(c.getTeacherId(), false);
    return c;
  }

  private void version(Long expected, long actual) {
    if (expected == null) throw ApiException.bad("Thiếu phiên bản kế hoạch");
    if (expected != actual)
      throw ApiException.conflict("Kế hoạch đã đổi; tải lại trước khi xác nhận");
  }

  private String reason(String s) {
    if (s == null || s.trim().length() < 3 || s.trim().length() > 500)
      throw ApiException.bad("Nhập lý do/căn cứ từ 3 đến 500 ký tự");
    return s.trim();
  }

  public static void minutes(Integer m) {
    if (m == null || m < 1 || m > DurationDto.MAX_MINUTES)
      throw ApiException.bad("Thời lượng phải là số phút nguyên từ 1 đến 600000");
  }

  private DurationDto.CourseView courseView(Course c) {
    return new DurationDto.CourseView(
        c.getId(),
        c.getDurationRevision(),
        c.getExpectedMinutes(),
        c.getDuration(),
        c.getExpectedMinutes() != null,
        canManage(c));
  }

  @Transactional(readOnly = true)
  public DurationDto.CourseView getCourse(Long id) {
    return courseView(course(id));
  }

  @Transactional(readOnly = true)
  public List<CourseDurationPlan> courseHistory(Long id) {
    course(id);
    return coursePlans.findByCourseIdOrderByRevisionDesc(id);
  }

  public DurationDto.CourseView confirmCourse(Long id, DurationDto.CourseRequest r) {
    var c = course(id);
    if (!canManage(c)) throw ApiException.forbidden();
    if (c.getBranchId() != null) branches.lock(c.getBranchId());
    c = courses.lock(id).orElseThrow(ApiException::missing);
    em.refresh(c, LockModeType.PESSIMISTIC_WRITE);
    scope.course(c.getBranchId());
    version(r.expectedRevision(), c.getDurationRevision());
    recordCourse(c, r.expectedMinutes(), reason(r.reason()));
    return courseView(c);
  }

  // Called on newly created course under the existing course-write authorization/transaction.
  public void initializeCourse(Course c, Integer expectedMinutes) {
    if (!canManage(c)) throw ApiException.forbidden();
    recordCourse(c, expectedMinutes, "Kế hoạch phút xác nhận khi tạo khóa học");
  }

  private void recordCourse(Course c, Integer expected, String why) {
    minutes(expected);
    c.setExpectedMinutes(expected);
    c.setDurationRevision(Math.addExact(c.getDurationRevision(), 1));
    var p = new CourseDurationPlan();
    p.setCourseId(c.getId());
    p.setRevision(c.getDurationRevision());
    p.setExpectedMinutes(expected);
    p.setActorId(Users.current().accountId());
    p.setActorName(Users.current().username());
    p.setOccurredAt(LocalDateTime.now(BusinessTime.ZONE));
    p.setReason(why);
    coursePlans.saveAndFlush(p);
  }

  private DurationDto.ClassView classView(SchoolClass c) {
    return new DurationDto.ClassView(
        c.getId(),
        c.getDurationRevision(),
        c.getExpectedMinutes(),
        c.getExpectedMinutes() != null,
        manager() && c.getStatus() != SchoolClass.Status.FINISHED,
        courseView(c.getCourse()));
  }

  @Transactional(readOnly = true)
  public DurationDto.ClassView getClass(Long id) {
    return classView(school(id));
  }

  @Transactional(readOnly = true)
  public List<ClassDurationPlan> classHistory(Long id) {
    school(id);
    return classPlans.findByClassIdOrderByRevisionDesc(id);
  }

  public DurationDto.ClassView confirmClass(Long id, DurationDto.ClassRequest r) {
    if (!manager()) throw ApiException.forbidden();
    var c = school(id);
    branches.lock(c.getBranchId());
    c = classes.lock(id).orElseThrow(ApiException::missing);
    em.refresh(c, LockModeType.PESSIMISTIC_WRITE);
    scope.check(c.getBranchId());
    version(r.expectedRevision(), c.getDurationRevision());
    if (c.getStatus() == SchoolClass.Status.FINISHED)
      throw ApiException.conflict("Lớp đã đóng; mở lại và đối soát trước khi đổi kế hoạch");
    resultBarrier.mutable(c.getId(),c.getBranchId());
    if (r.source() == ClassDurationPlan.Source.COURSE) {
      var co = courses.lock(c.getCourse().getId()).orElseThrow(ApiException::missing);
      em.refresh(co, LockModeType.PESSIMISTIC_WRITE);
      version(r.expectedCourseRevision(), co.getDurationRevision());
      minutes(co.getExpectedMinutes());
      if (r.expectedMinutes() != null)
        throw ApiException.bad("Áp dụng khóa không nhận số phút ghi đè");
      recordClass(
          c,
          co.getExpectedMinutes(),
          ClassDurationPlan.Source.COURSE,
          co.getDurationRevision(),
          reason(r.reason()));
    } else if (r.source() == ClassDurationPlan.Source.CUSTOM) {
      if (r.expectedCourseRevision() != null)
        throw ApiException.bad("Kế hoạch riêng không dùng phiên bản khóa");
      minutes(r.expectedMinutes());
      recordClass(
          c, r.expectedMinutes(), ClassDurationPlan.Source.CUSTOM, null, reason(r.reason()));
    } else throw ApiException.bad("Không xác nhận kế hoạch chưa rõ thời lượng");
    classes.flush();
    return classView(c);
  }

  // Class create/course-change caller already holds branch/class/course locks.
  public void initializeClass(SchoolClass c) {
    var co = c.getCourse();
    recordClass(
        c,
        co.getExpectedMinutes(),
        co.getExpectedMinutes() == null
            ? ClassDurationPlan.Source.UNVERIFIED
            : ClassDurationPlan.Source.COURSE,
        co.getExpectedMinutes() == null ? null : co.getDurationRevision(),
        "Lưu kế hoạch khi mở lớp hoặc thay khóa");
  }

  private void recordClass(
      SchoolClass c,
      Integer expected,
      ClassDurationPlan.Source source,
      Long courseRevision,
      String why) {
    c.setExpectedMinutes(expected);
    c.setDurationRevision(Math.addExact(c.getDurationRevision(), 1));
    var p = new ClassDurationPlan();
    p.setClassId(c.getId());
    p.setRevision(c.getDurationRevision());
    p.setExpectedMinutes(expected);
    p.setCourseId(c.getCourse().getId());
    p.setCourseRevision(courseRevision);
    p.setSource(source);
    p.setActorId(Users.current().accountId());
    p.setActorName(Users.current().username());
    p.setOccurredAt(LocalDateTime.now(BusinessTime.ZONE));
    p.setReason(why);
    classPlans.saveAndFlush(p);
  }

  public void discardDraft(SchoolClass c) {
    classPlans.deleteAll(classPlans.findByClassIdOrderByRevisionDesc(c.getId()));
    classPlans.flush();
  }
}
