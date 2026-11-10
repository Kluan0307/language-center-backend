package com.example.academic;

import static org.junit.jupiter.api.Assertions.*;

import com.example.academic.branch.*;
import com.example.academic.course.*;
import com.example.academic.enrollment.*;
import com.example.academic.lead.*;
import com.example.academic.room.*;
import com.example.academic.schoolclass.*;
import com.example.academic.security.*;
import com.example.academic.student.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:list-query-budget;MODE=MySQL;DB_CLOSE_DELAY=-1",
      "spring.jpa.properties.hibernate.generate_statistics=true"
    })
@Transactional
class ListQueryBudgetTest {
  @Autowired LeadService leadService;
  @Autowired LeadRepository leads;
  @Autowired EnrollmentService enrollmentService;
  @Autowired EnrollmentRepository enrollments;
  @Autowired StudentRepository students;
  @Autowired SchoolClassRepository classes;
  @Autowired RoomRepository rooms;
  @Autowired CourseRepository courses;
  @Autowired BranchRepository branches;
  @Autowired EntityManager em;
  @Autowired EntityManagerFactory emf;
  Long branch;
  String prefix;

  @BeforeEach
  void setup() {
    prefix = "Perf-" + UUID.randomUUID();
    var b = new Branch();
    b.setCode(prefix);
    b.setName("Branch");
    b.setPhone("0900000000");
    branches.saveAndFlush(b);
    branch = b.getId();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, 1L, null, Role.SUPER_ADMIN, "tester", List.of()),
                null,
                List.of()));
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  Course course() {
    var c = new Course();
    c.setCode(UUID.randomUUID().toString());
    c.setName("Course");
    c.setDuration(10);
    c.setTuitionFee(BigDecimal.TEN);
    c.setMaxStudents(20);
    c.setBranchId(branch);
    return courses.saveAndFlush(c);
  }

  void reset() {
    em.flush();
    em.clear();
    emf.unwrap(SessionFactory.class).getStatistics().clear();
  }

  long queries() {
    return emf.unwrap(SessionFactory.class).getStatistics().getPrepareStatementCount();
  }

  @Test
  void leadPageHasBoundedQueryCount() {
    for (int i = 0; i < 20; i++) {
      var l = new Lead();
      l.setFullName(prefix + i);
      l.setPhone("0900000000");
      l.setAssignedTo(1L);
      l.setAssignedToName("tester");
      l.setBranchId(branch);
      l.setInterestedCourse(course());
      leads.saveAndFlush(l);
    }
    reset();
    assertEquals(20, leadService.list(0, 20, prefix, null, null, branch).content().size());
    long count = queries();
    System.out.println("PERF_LEAD_LIST_STATEMENTS=" + count);
    assertTrue(count <= 3, "Lead query budget: " + count);
  }

  @Test
  void enrollmentPageHasBoundedQueryCount() {
    var room = new Room();
    room.setCode(UUID.randomUUID().toString());
    room.setName("Room");
    room.setCapacity(20);
    room.setBranchId(branch);
    rooms.saveAndFlush(room);
    for (int i = 0; i < 20; i++) {
      var s = new Student();
      s.setFullName(prefix + i);
      s.setPhone("0900000000");
      s.setBranchId(branch);
      students.saveAndFlush(s);
      var c = new SchoolClass();
      c.setCode(UUID.randomUUID().toString());
      c.setName("Class");
      c.setBranchId(branch);
      c.setCourse(course());
      c.setRoom(room);
      c.setTeacherId(1L);
      c.setTeacherName("Teacher");
      c.setStartDate(LocalDate.now());
      c.setEndDate(LocalDate.now().plusMonths(1));
      c.setMaxSize(20);
      classes.saveAndFlush(c);
      var e = new Enrollment();
      e.setSchoolClass(c);
      e.setStudent(s);
      enrollments.saveAndFlush(e);
    }
    reset();
    assertEquals(
        20, enrollmentService.list(0, 20, prefix, null, null, null, branch).content().size());
    long count = queries();
    System.out.println("PERF_ENROLLMENT_LIST_STATEMENTS=" + count);
    assertTrue(count <= 3, "Enrollment query budget: " + count);
  }
}
