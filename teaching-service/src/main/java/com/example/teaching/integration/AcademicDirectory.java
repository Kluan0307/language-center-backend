package com.example.teaching.integration;

import com.example.teaching.common.*;
import com.example.teaching.security.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AcademicDirectory {
  private final RemoteApi api;
  private final BranchScope scope;

  @Value("${services.academic.url}")
  private String base;

  public record SchoolClass(
      Long id, Long branchId, Long teacherId, Long courseId, String name, Boolean closed) {
    public SchoolClass(Long id, Long branchId, Long teacherId, Long courseId, String name) {
      this(id, branchId, teacherId, courseId, name, false);
    }
  }

  public record Detail(SchoolClass schoolClass) {}

  public record Student(Long id, String fullName, Long enrollmentId, String source) {
    public Student(Long id, String name) {
      this(id, name, null, "UNRESOLVED");
    }
  }

  public record Course(Long id, Long branchId) {}

  public record Classes(List<SchoolClass> content) {}

  public SchoolClass schoolClass(Long id) {
    SchoolClass c = api.get(base + "/api/v1/academic/classes/" + id, Detail.class).schoolClass();
    if (c == null) throw ApiException.missing();
    scope.check(c.branchId());
    scope.teacher(c.teacherId(), false);
    return c;
  }

  public com.example.teaching.evaluation.AcademicEvaluationContext evaluationContext(
      Long classId, boolean attendance) {
    return api.get(
        base
            + "/api/v1/academic/classes/"
            + classId
            + "/evaluation-context?includeAttendance="
            + attendance,
        com.example.teaching.evaluation.AcademicEvaluationContext.class);
  }

  public List<Student> students(Long classId) {
    schoolClass(classId);
    return Arrays.asList(
        api.get(base + "/api/v1/academic/classes/" + classId + "/students", Student[].class));
  }

  public List<Student> studentsAt(Long classId, java.time.LocalDate date) {
    schoolClass(classId);
    return Arrays.asList(
        api.get(
            base + "/api/v1/academic/classes/" + classId + "/roster?date=" + date,
            Student[].class));
  }

  public void activeStudent(Long classId, Long studentId) {
    if (students(classId).stream().noneMatch(s -> s.id().equals(studentId)))
      throw ApiException.bad("Student is not studying in this class");
  }

  public Course course(Long id, Long branch) {
    Course c = api.get(base + "/api/v1/academic/courses/" + id, Course.class);
    if (c.branchId() != null && !c.branchId().equals(branch)) throw ApiException.missing();
    if (Users.current().role() == Role.TEACHER) {
      Classes classes =
          api.get(base + "/api/v1/academic/classes?size=1&courseId=" + id, Classes.class);
      if (classes.content() == null || classes.content().isEmpty()) throw ApiException.forbidden();
    }
    return c;
  }
}
