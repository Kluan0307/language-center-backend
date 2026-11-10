package com.example.academic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.academic.branch.*;
import com.example.academic.common.*;
import com.example.academic.course.*;
import com.example.academic.enrollment.*;
import com.example.academic.lead.*;
import com.example.academic.room.*;
import com.example.academic.schedule.ScheduleConflictService;
import com.example.academic.schoolclass.*;
import com.example.academic.security.*;
import com.example.academic.student.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
    properties = "spring.datasource.url=jdbc:h2:mem:admission-tests;MODE=MySQL;DB_CLOSE_DELAY=-1")
class AdmissionTransactionTest {
  @Autowired PlacementTestFixtures placementFixtures;
  @Autowired EnrollmentService service;
  @Autowired EnrollmentRepository enrollments;
  @Autowired CourseRepository courses;
  @Autowired RoomRepository rooms;
  @Autowired SchoolClassRepository classes;
  @Autowired StudentRepository students;
  @MockitoBean RemoteApi remote;
  @Autowired StudentService studentService;
  @Autowired TuitionDeliveryService delivery;
  @Autowired EnrollmentPeriodRepository periods;
  @Autowired LeadService leadService;
  @Autowired BranchRepository branches;

  @Test
  void leadConversionIsIdempotentAndDoesNotInventBirthDate() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, 1L, 1L, Role.ADMIN, "admin"), null, List.of()));
    try {
      if (!branches.existsById(1L)) {
        Branch branch = new Branch();
        branch.setCode("ADMISSION");
        branch.setName("Admission");
        branch.setPhone("0900000000");
        branches.saveAndFlush(branch);
      }
      var lead =
          leadService.save(
              null,
              new LeadDto.Request("Learner", "0901234567", null, "WEB", null, "A1", "First", 1L));
      assertThrows(
          ApiException.class,
          () -> leadService.convert(lead.id(), leadService.get(lead.id()).version()));
      leadService.status(
          lead.id(),
          new LeadDto.StatusRequest(
              Lead.Status.CONTACTED, "Called", leadService.get(lead.id()).version()));
      var converted = leadService.convert(lead.id(), leadService.get(lead.id()).version());
      assertNotNull(converted.convertedStudent().studentCode());
      assertNull(converted.convertedStudent().dateOfBirth());
      assertEquals(
          converted.convertedStudent().id(),
          leadService
              .convert(lead.id(), leadService.get(lead.id()).version())
              .convertedStudent()
              .id());
      assertThrows(
          ApiException.class, () -> studentService.delete(converted.convertedStudent().id()));
      assertThrows(
          ApiException.class,
          () ->
              leadService.status(
                  lead.id(),
                  new LeadDto.StatusRequest(
                      Lead.Status.NEW, null, leadService.get(lead.id()).version())));
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void sharedWeekdayMustActuallyOccurDuringDateIntersection() {
    SchoolClass a = new SchoolClass();
    SchoolClass b = new SchoolClass();
    a.setStartDate(LocalDate.of(2027, 1, 4));
    a.setEndDate(LocalDate.of(2027, 1, 10));
    b.setStartDate(LocalDate.of(2027, 1, 5));
    b.setEndDate(LocalDate.of(2027, 1, 10));
    assertFalse(ScheduleConflictService.occursDuringOverlap(a, b, java.time.DayOfWeek.MONDAY));
    assertTrue(ScheduleConflictService.occursDuringOverlap(a, b, java.time.DayOfWeek.TUESDAY));
  }

  @Test
  void financeFailureDoesNotRollbackEnrollmentAndTransferKeepsBilling() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, 1L, 1L, Role.ADMIN, "admin"), null, List.of()));
    when(remote.token()).thenReturn("Bearer test");
    when(remote.post(anyString(), any(), any(), anyString()))
        .thenThrow(
            new ApiException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "finance offline"));
    try {
      Course course = new Course();
      course.setBranchId(1L);
      course.setCode("TEST-" + UUID.randomUUID());
      course.setName("Course");
      course.setDuration(10);
      course.setTuitionFee(new BigDecimal("100"));
      course.setMaxStudents(10);
      course = courses.saveAndFlush(course);
      Room room = new Room();
      room.setBranchId(1L);
      room.setCode("R-" + UUID.randomUUID());
      room.setName("Room");
      room.setCapacity(10);
      room = rooms.saveAndFlush(room);
      SchoolClass a = schoolClass(course, room);
      SchoolClass b = schoolClass(course, room);
      Student student = new Student();
      student.setBranchId(1L);
      student.setFullName("One");
      student.setPhone("shared-parent");
      student = students.saveAndFlush(student);
      Student sibling = new Student();
      sibling.setBranchId(1L);
      sibling.setFullName("Two");
      sibling.setPhone("shared-parent");
      students.saveAndFlush(sibling);
      var created = service.enroll(new EnrollmentDto.Request(student.getId(), a.getId()));
      Enrollment persisted = enrollments.findById(created.id()).orElseThrow();
      assertEquals(Enrollment.StudyStatus.STUDYING, persisted.getStudyStatus());
      assertEquals(Enrollment.TuitionDelivery.FAILED, persisted.getTuitionDelivery());
      var transferred =
          service.transfer(
              created.id(),
              new EnrollmentDto.TransferRequest(
                  b.getId(), "Move to another cohort", service.get(created.id()).version()));
      assertEquals(created.id(), transferred.billingEnrollmentId());
      assertEquals(created.id(), service.billing(transferred.id()).enrollmentId());
      assertEquals(Enrollment.TuitionDelivery.FAILED, transferred.tuitionDelivery());
      assertEquals(
          Enrollment.TuitionDelivery.FAILED, service.get(transferred.id()).tuitionDelivery());
      assertEquals(
          Enrollment.TuitionDelivery.FAILED,
          service.list(0, 20, null, null, null, student.getId(), null).content().stream()
              .filter(e -> e.id().equals(transferred.id()))
              .findFirst()
              .orElseThrow()
              .tuitionDelivery());
      assertThrows(
          ApiException.class, () -> profileStatus(created.studentId(), Student.Status.PAUSED));
      changeStatus(transferred.id(), Enrollment.StudyStatus.RESERVED);
      assertEquals(0, classes.findById(b.getId()).orElseThrow().getCurrentSize());
      assertTrue(
          periods.findByEnrollmentId(transferred.id()).stream()
              .filter(p -> p.getActiveUntil() != null)
              .allMatch(p -> !p.getActiveUntil().isBefore(p.getActiveFrom())));
      assertThrows(
          ApiException.class,
          () ->
              service.enroll(
                  new EnrollmentDto.Request(created.studentId(), transferred.classId())));
      profileStatus(created.studentId(), Student.Status.PAUSED);
      assertThrows(
          ApiException.class,
          () -> changeStatus(transferred.id(), Enrollment.StudyStatus.STUDYING));
      profileStatus(created.studentId(), Student.Status.STUDYING);
      changeStatus(transferred.id(), Enrollment.StudyStatus.STUDYING);
      changeStatus(transferred.id(), Enrollment.StudyStatus.RESERVED);
      changeStatus(transferred.id(), Enrollment.StudyStatus.DROPPED);
      assertThrows(
          ApiException.class,
          () -> changeStatus(transferred.id(), Enrollment.StudyStatus.STUDYING));
      verify(remote, times(1)).post(anyString(), any(), any(), anyString());
      doReturn(new TuitionDeliveryService.Receipt(100L))
          .when(remote)
          .post(anyString(), any(), any(), anyString());
      delivery.deliver(transferred.id(), "Bearer test");
      assertEquals(
          Enrollment.TuitionDelivery.CREATED, service.get(transferred.id()).tuitionDelivery());
      delivery.deliver(transferred.id(), "Bearer test");
      verify(remote, times(2)).post(anyString(), any(), any(), anyString());
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private EnrollmentDto changeStatus(Long id, Enrollment.StudyStatus state) {
    return service.status(
        id,
        new EnrollmentDto.StatusRequest(
            state,
            "Documented test change",
            service.get(id).version(),
            state == Enrollment.StudyStatus.RESERVED
                ? LocalDate.now(com.example.academic.common.BusinessTime.ZONE).plusDays(30)
                : null));
  }

  private StudentDto profileStatus(Long id, Student.Status state) {
    return studentService.status(
        id,
        new StudentDto.StatusRequest(
            state, "Documented profile change", studentService.get(id).student().version()));
  }

  private SchoolClass schoolClass(Course course, Room room) {
    SchoolClass c = new SchoolClass();
    c.setBranchId(1L);
    c.setCode("C-" + UUID.randomUUID());
    c.setName("Class");
    c.setCourse(course);
    c.setRoom(room);
    c.setTeacherId(9L);
    c.setTeacherName("Teacher");
    c.setStartDate(LocalDate.now().plusDays(7));
    c.setEndDate(LocalDate.now().plusMonths(1));
    c.setMaxSize(10);
    var result=classes.saveAndFlush(c);placementFixtures.open(result.getId());return result;
  }
}
