package com.example.academic;

import static org.junit.jupiter.api.Assertions.*;

import com.example.academic.branch.*;
import com.example.academic.course.*;
import com.example.academic.room.*;
import com.example.academic.schoolclass.*;
import com.example.academic.security.*;
import com.example.academic.session.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:teacher-responsibility-tests;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class TeacherResponsibilitiesTest {
  @Autowired TeacherResponsibilitiesController controller;
  @Autowired SchoolClassRepository classes;
  @Autowired ClassSessionRepository sessions;
  @Autowired BranchRepository branches;
  @Autowired CourseRepository courses;
  @Autowired RoomRepository rooms;

  void actor(Role role, Long branch) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(
                    99L,
                    null,
                    branch,
                    role,
                    "tester",
                    branch == null ? List.of() : List.of(branch)),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  Long branch() {
    var b = new Branch();
    b.setCode(UUID.randomUUID().toString());
    b.setName("Branch");
    b.setPhone("0900000000");
    return branches.saveAndFlush(b).getId();
  }

  SchoolClass school(Long branch, boolean past) {
    var course = new Course();
    course.setCode(UUID.randomUUID().toString());
    course.setName("Course");
    course.setDuration(10);
    course.setTuitionFee(java.math.BigDecimal.TEN);
    course.setMaxStudents(10);
    course.setBranchId(branch);
    courses.saveAndFlush(course);
    var room = new Room();
    room.setCode(UUID.randomUUID().toString());
    room.setName("Room");
    room.setBranchId(branch);
    room.setCapacity(10);
    rooms.saveAndFlush(room);
    var c = new SchoolClass();
    c.setCode(UUID.randomUUID().toString());
    c.setName("Class");
    c.setBranchId(branch);
    c.setCourse(course);
    c.setRoom(room);
    c.setTeacherId(7L);
    c.setTeacherName("Teacher");
    c.setStartDate(LocalDate.now().minusDays(30));
    c.setEndDate(past ? LocalDate.now().minusDays(10) : LocalDate.now().plusDays(30));
    c.setMaxSize(10);
    return classes.saveAndFlush(c);
  }

  ClassSession session(SchoolClass c, ClassSession.Status state) {
    var s = new ClassSession();
    s.setSchoolClass(c);
    s.setSessionDate(LocalDate.now().minusDays(20));
    s.setTeacherId(7L);
    s.setStatus(state);
    return sessions.saveAndFlush(s);
  }

  @Test
  void includesClassWithoutSessionsAndOverdueSubstituteButNotHistory() {
    var b = branch();
    var ongoing = school(b, false);
    var past = school(b, true);
    past.setTeacherId(8L);
    classes.saveAndFlush(past);
    var unresolved = session(past, ClassSession.Status.SCHEDULED);
    session(past, ClassSession.Status.COMPLETED);
    session(past, ClassSession.Status.CANCELLED);
    actor(Role.ADMIN, b);
    var result = controller.list(7L, b);
    assertEquals(2, result.size());
    assertTrue(
        result.stream().anyMatch(r -> r.kind().equals("CLASS") && r.id().equals(ongoing.getId())));
    assertTrue(
        result.stream()
            .anyMatch(r -> r.kind().equals("SESSION") && r.id().equals(unresolved.getId())));
  }

  @Test
  void superCanFindOrphanAcrossBranchesAndAdminCannotReadForeignBranch() {
    var a = branch();
    var b = branch();
    school(a, false);
    school(b, false);
    actor(Role.SUPER_ADMIN, null);
    assertEquals(2, controller.list(7L, null).size());
    actor(Role.ADMIN, a);
    assertEquals(1, controller.list(7L, a).size());
    assertThrows(com.example.academic.common.ApiException.class, () -> controller.list(7L, b));
    assertThrows(com.example.academic.common.ApiException.class, () -> controller.list(7L, null));
  }

  @Test
  void staffAndTeacherCannotUseManagementEndpoint() {
    var b = branch();
    for (var role : List.of(Role.STAFF, Role.TEACHER)) {
      actor(role, b);
      assertThrows(
          org.springframework.security.access.AccessDeniedException.class,
          () -> controller.list(7L, b));
    }
  }
}
