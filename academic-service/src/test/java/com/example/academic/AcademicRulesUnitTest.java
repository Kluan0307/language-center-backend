package com.example.academic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.academic.attendance.*;
import com.example.academic.branch.*;
import com.example.academic.enrollment.*;
import com.example.academic.room.*;
import com.example.academic.schedule.*;
import com.example.academic.schoolclass.*;
import com.example.academic.student.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AcademicRulesUnitTest {
  SchoolClass schoolClass(long id) {
    SchoolClass c = new SchoolClass();
    c.setId(id);
    c.setBranchId(1L);
    c.setTeacherId(id);
    c.setName("Class");
    c.setCode("C" + id);
    c.setStartDate(LocalDate.now().minusMonths(1));
    c.setEndDate(LocalDate.now().plusMonths(1));
    Room r = new Room();
    r.setId(id);
    c.setRoom(r);
    c.setMaxSize(10);
    return c;
  }

  @Test
  void aFullClassStillFinishesByDate() {
    var c = schoolClass(1);
    c.setStatus(SchoolClass.Status.FULL);
    c.setCurrentSize(10);
    c.setEndDate(LocalDate.now().minusDays(1));
    assertEquals(SchoolClass.Status.FINISHED, c.effectiveStatus());
  }

  @Test
  void changingScheduleDetectsSharedStudents() {
    var schedules = mock(ScheduleRepository.class);
    var enrollments = mock(EnrollmentRepository.class);
    var candidate = schoolClass(1);
    var other = schoolClass(2);
    var student = new Student();
    student.setId(5L);
    var a = new Enrollment();
    a.setStudent(student);
    var b = new Enrollment();
    b.setStudent(student);
    when(enrollments.findBySchoolClassIdAndStudyStatus(1L, Enrollment.StudyStatus.STUDYING))
        .thenReturn(List.of(a));
    when(enrollments.findBySchoolClassIdAndStudyStatus(2L, Enrollment.StudyStatus.STUDYING))
        .thenReturn(List.of(b));
    var slot = new Schedule();
    slot.setId(3L);
    slot.setSchoolClass(other);
    slot.setDayOfWeek(DayOfWeek.MONDAY);
    slot.setStartTime(LocalTime.of(18, 0));
    slot.setEndTime(LocalTime.of(20, 0));
    when(schedules.conflictCandidates(1L, 1L, DayOfWeek.MONDAY)).thenReturn(List.of(slot));
    var conflicts =
        new ScheduleConflictService(schedules, enrollments)
            .conflicts(
                candidate,
                DayOfWeek.MONDAY,
                LocalTime.of(18, 30),
                LocalTime.of(19, 30),
                null,
                false);
    assertEquals(1, conflicts.size());
    assertTrue(conflicts.getFirst().reason().contains("Student"));
  }

  @Test
  void historicalAttendanceKeepsTransferredStudents() {
    var attendance = mock(AttendanceRepository.class);
    var enrollment = mock(EnrollmentRepository.class);
    var classes = mock(SchoolClassService.class);
    var branches = mock(BranchService.class);
    var periods = mock(EnrollmentPeriodRepository.class);
    var planner = mock(com.example.academic.session.SessionPlanner.class);
    var c = schoolClass(1);
    var student = new Student();
    student.setId(2L);
    student.setFullName("Student");
    var e = new Enrollment();
    e.setId(4L);
    e.setStudent(student);
    e.setSchoolClass(c);
    e.setStudyStatus(Enrollment.StudyStatus.TRANSFERRED);
    var saved = new Attendance();
    saved.setEnrollment(e);
    saved.setStatus(Attendance.Status.ABSENT);
    LocalDate date = LocalDate.now().minusDays(5);
    var session=new com.example.academic.session.ClassSession();session.setId(5L);session.setSchoolClass(c);session.setSessionDate(date);session.setStatus(com.example.academic.session.ClassSession.Status.COMPLETED);
    when(planner.resolve(null,1L,date,false)).thenReturn(session);
    org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(new com.example.academic.security.CurrentUser(1L,1L,1L,com.example.academic.security.Role.ADMIN,"admin"),null,List.of()));
    when(classes.attendance(1L, false)).thenReturn(c);
    when(attendance.findByEnrollmentSchoolClassIdAndSessionDate(1L, date))
        .thenReturn(List.of(saved));
    when(enrollment.findBySchoolClassId(1L)).thenReturn(List.of(e));
    var sheet =
        new AttendanceService(attendance, enrollment, classes, branches, periods, planner, mock(AttendanceChangeRepository.class))
            .sheet(1L, date);
    assertEquals(1, sheet.records().size());
    assertEquals(Attendance.Status.ABSENT, sheet.records().getFirst().status());
    org.springframework.security.core.context.SecurityContextHolder.clearContext();
  }

  @Test
  void activePeriodUsesExclusiveEndDate() {
    var period = new EnrollmentPeriod();
    period.setActiveFrom(LocalDate.of(2026, 1, 1));
    period.setActiveUntil(LocalDate.of(2026, 2, 1));
    assertTrue(period.includes(LocalDate.of(2026, 1, 31)));
    assertFalse(period.includes(LocalDate.of(2026, 2, 1)));
  }
}
