package com.example.academic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.academic.branch.*;
import com.example.academic.common.*;
import com.example.academic.course.*;
import com.example.academic.enrollment.*;
import com.example.academic.lifecycle.*;
import com.example.academic.room.*;
import com.example.academic.schoolclass.*;
import com.example.academic.security.*;
import com.example.academic.student.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = "spring.datasource.url=jdbc:h2:mem:lifecycle-tests;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class LearningLifecycleTest {
  @Autowired PlacementTestFixtures placementFixtures;
  @Autowired EnrollmentService service;
  @Autowired StudentService studentService;
  @Autowired LearningHistory history;
  @Autowired DatedRoster roster;
  @Autowired EnrollmentRepository enrollments;
  @Autowired EnrollmentPeriodRepository periods;
  @Autowired StudentRepository students;
  @Autowired SchoolClassRepository classes;
  @Autowired CourseRepository courses;
  @Autowired RoomRepository rooms;
  @Autowired BranchRepository branches;
  @MockitoBean RemoteApi remote;
  @MockitoBean com.example.academic.results.CompletionBridge completion;
  LocalDate today;
  SchoolClass a, b;
  Student student;

  @BeforeEach
  void setup() {
    today = LearningHistory.today();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(99L, 9L, null, Role.SUPER_ADMIN, "operator", List.of()),
                null,
                List.of()));
    var branch = new Branch();
    branch.setCode(UUID.randomUUID().toString());
    branch.setName("Branch");
    branch.setPhone("0900000000");
    branches.saveAndFlush(branch);
    var co = new Course();
    co.setCode(UUID.randomUUID().toString());
    co.setName("Course");
    co.setDuration(10);
    co.setTuitionFee(BigDecimal.TEN);
    co.setMaxStudents(10);
    co.setBranchId(branch.getId());
    courses.saveAndFlush(co);
    var r = new Room();
    r.setCode(UUID.randomUUID().toString());
    r.setName("Room");
    r.setCapacity(10);
    r.setBranchId(branch.getId());
    rooms.saveAndFlush(r);
    a = school(branch.getId(), co, r);
    b = school(branch.getId(), co, r);
    student = new Student();
    student.setFullName("Learner");
    student.setPhone("0900000000");
    student.setBranchId(branch.getId());
    students.saveAndFlush(student);
  }

  SchoolClass school(Long branch, Course co, Room room) {
    var c = new SchoolClass();
    c.setCode(UUID.randomUUID().toString());
    c.setName("Class");
    c.setCourse(co);
    c.setRoom(room);
    c.setTeacherId(9L);
    c.setTeacherName("Teacher");
    c.setBranchId(branch);
    c.setStartDate(today.minusDays(20));
    c.setEndDate(today.plusDays(20));
    c.setMaxSize(10);
    var result=classes.saveAndFlush(c);placementFixtures.open(result.getId());return result;
  }

  Enrollment enrollment() {
    var e = new Enrollment();
    e.setStudent(student);
    e.setSchoolClass(a);
    e.setTuitionFee(BigDecimal.TEN);
    e.setEnrolledAt(today.minusDays(15).atStartOfDay());
    enrollments.saveAndFlush(e);
    e.setBillingEnrollmentId(e.getId());
    enrollments.flush();
    var p = new EnrollmentPeriod();
    p.setEnrollment(e);
    p.setActiveFrom(today.minusDays(15));
    periods.saveAndFlush(p);
    return e;
  }

  EnrollmentDto change(Enrollment e, Enrollment.StudyStatus state) {
    return service.status(
        e.getId(),
        new EnrollmentDto.StatusRequest(
            state,
            "A documented decision",
            service.get(e.getId()).version(),
            state == Enrollment.StudyStatus.RESERVED ? today.plusDays(7) : null));
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void reservationClosesPeriodAndAuditsExpectedReturn() {
    var e = enrollment();
    var saved = change(e, Enrollment.StudyStatus.RESERVED);
    assertEquals(today, saved.reservedSince());
    assertEquals(today.plusDays(7), saved.expectedReturnDate());
    assertEquals(0, classes.findById(a.getId()).orElseThrow().getCurrentSize());
    var h = history.enrollment(e.getId());
    assertEquals(today, h.periods().getFirst().activeUntil());
    assertEquals("operator", h.events().getFirst().actorName());
    assertEquals("RESERVED", h.events().getFirst().action());
  }

  @Test
  void reservationRequiresFutureExpectedDate() {
    var e = enrollment();
    assertThrows(
        ApiException.class,
        () ->
            service.status(
                e.getId(),
                new EnrollmentDto.StatusRequest(
                    Enrollment.StudyStatus.RESERVED, "Medical leave", e.getVersion(), today)));
  }

  @Test
  void staleStatusCannotOverwriteReservation() {
    var e = enrollment();
    var v = e.getVersion();
    change(e, Enrollment.StudyStatus.RESERVED);
    assertThrows(
        ApiException.class,
        () ->
            service.status(
                e.getId(),
                new EnrollmentDto.StatusRequest(
                    Enrollment.StudyStatus.DROPPED, "Old browser tab", v, null)));
    assertEquals(Enrollment.StudyStatus.RESERVED, service.get(e.getId()).studyStatus());
  }

  @Test
  void returnIsManualAndDoesNotCreateAnotherBillingRoot() {
    var e = enrollment();
    change(e, Enrollment.StudyStatus.RESERVED);
    var saved = change(e, Enrollment.StudyStatus.STUDYING);
    assertEquals(e.getId(), saved.billingEnrollmentId());
    assertNull(saved.expectedReturnDate());
    assertEquals(2, history.enrollment(e.getId()).periods().size());
    assertEquals("RESUMED", history.enrollment(e.getId()).events().getFirst().action());
  }

  @Test
  void reservedStudentCanMoveFromFinishedClassWithoutNewTuition() {
    var e = enrollment();
    change(e, Enrollment.StudyStatus.RESERVED);
    a.setStatus(SchoolClass.Status.FINISHED);
    classes.flush();
    var moved =
        service.transfer(
            e.getId(),
            new EnrollmentDto.TransferRequest(
                b.getId(), "Old class ended", service.get(e.getId()).version()));
    assertEquals(e.getId(), moved.billingEnrollmentId());
    assertEquals(Enrollment.StudyStatus.STUDYING, moved.studyStatus());
    assertEquals(Enrollment.StudyStatus.TRANSFERRED, service.get(e.getId()).studyStatus());
    assertEquals(
        moved.id(), history.enrollment(e.getId()).events().getFirst().relatedEnrollmentId());
  }

  @Test
  void rosterUsesDateInsteadOfCurrentStatus() {
    var e = enrollment();
    change(e, Enrollment.StudyStatus.RESERVED);
    assertEquals(student.getId(), roster.students(a.getId(), today.minusDays(1)).getFirst().id());
    assertTrue(roster.students(a.getId(), today).isEmpty());
    change(e, Enrollment.StudyStatus.STUDYING);
    assertEquals(1, roster.students(a.getId(), today).size());
    assertTrue(roster.students(a.getId(), today.minusDays(16)).isEmpty());
  }

  @Test
  void completionIncludesLastStudyDayButNeverReopens() {
    var e = enrollment();
    change(e, Enrollment.StudyStatus.COMPLETED);
    assertEquals(1, roster.students(a.getId(), today).size());
    assertTrue(roster.students(a.getId(), today.plusDays(1)).isEmpty());
    assertThrows(ApiException.class, () -> change(e, Enrollment.StudyStatus.STUDYING));
  }

  @Test
  void inactiveProfilePreventsReturnAndGraduationCannotHideReservedSeat() {
    var e = enrollment();
    change(e, Enrollment.StudyStatus.RESERVED);
    assertThrows(
        ApiException.class,
        () ->
            studentService.status(
                student.getId(),
                new StudentDto.StatusRequest(
                    Student.Status.GRADUATED, "Finish entire profile", student.getVersion())));
    studentService.status(
        student.getId(),
        new StudentDto.StatusRequest(
            Student.Status.PAUSED, "Family requests pause", student.getVersion()));
    assertThrows(ApiException.class, () -> change(e, Enrollment.StudyStatus.STUDYING));
    assertTrue(
        history.student(student.getId()).stream()
            .anyMatch(x -> x.action().equals("PROFILE_STATUS")));
  }

  @Test
  void returnStillChecksCapacity() {
    var e = enrollment();
    change(e, Enrollment.StudyStatus.RESERVED);
    a.setMaxSize(0);
    classes.flush();
    assertThrows(ApiException.class, () -> change(e, Enrollment.StudyStatus.STUDYING));
  }

  @Test
  void historyOutsideAssignedBranchIsHidden() {
    var e = enrollment();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(100L, 10L, 9999L, Role.STAFF, "outsider", List.of(9999L)),
                null,
                List.of()));
    assertThrows(ApiException.class, () -> history.enrollment(e.getId()));
    assertThrows(ApiException.class, () -> history.student(student.getId()));
  }

  @Test
  void reservationExpectedDateCanBeUpdatedWithoutOpeningAnotherPeriod() {
    var e = enrollment();
    change(e, Enrollment.StudyStatus.RESERVED);
    var saved =
        service.status(
            e.getId(),
            new EnrollmentDto.StatusRequest(
                Enrollment.StudyStatus.RESERVED,
                "Recovery takes longer",
                service.get(e.getId()).version(),
                today.plusDays(30)));
    assertEquals(today.plusDays(30), saved.expectedReturnDate());
    assertEquals(today, saved.reservedSince());
    assertEquals(1, history.enrollment(e.getId()).periods().size());
    assertEquals("RESERVATION_UPDATED", history.enrollment(e.getId()).events().getFirst().action());
  }
}
