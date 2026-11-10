package com.example.academic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.academic.attendance.*;
import com.example.academic.common.ApiException;
import com.example.academic.enrollment.*;
import com.example.academic.results.*;
import com.example.academic.schoolclass.*;
import com.example.academic.security.*;
import com.example.academic.student.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class LearningProfileScopeTest {
  StudentService students = mock(StudentService.class);
  EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
  EnrollmentPeriodRepository periods = mock(EnrollmentPeriodRepository.class);
  AttendanceRepository attendance = mock(AttendanceRepository.class);
  LearningProfileService service =
      new LearningProfileService(students, enrollments, periods, attendance);

  void actor(Role role) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, 1L, null, role, "operator", List.of(1L)), null, List.of()));
  }

  Enrollment enrollment(long id, long branch, Student student) {
    var c = new SchoolClass();
    c.setId(id);
    c.setBranchId(branch);
    var e = new Enrollment();
    e.setId(id);
    e.setStudent(student);
    e.setSchoolClass(c);
    return e;
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void legacyForeignBranchEnrollmentDoesNotLeakThroughStudentProfile() {
    actor(Role.ADMIN);
    var s = new Student();
    s.setId(1L);
    s.setBranchId(1L);
    when(students.require(1L)).thenReturn(s);
    when(enrollments.findByStudentId(1L))
        .thenReturn(List.of(enrollment(1, 1, s), enrollment(2, 2, s)));
    var p = service.get(1L);
    assertEquals(1, p.studies().size());
    assertEquals(1L, p.studies().getFirst().enrollment().branchId());
  }

  @Test
  void teacherCannotUseCrmProfileService() {
    actor(Role.TEACHER);
    assertThrows(ApiException.class, () -> service.get(1L));
    verifyNoInteractions(students, enrollments);
  }
}
