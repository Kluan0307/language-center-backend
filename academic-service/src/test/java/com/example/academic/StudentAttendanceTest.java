package com.example.academic;
import static org.junit.jupiter.api.Assertions.*;
import com.example.academic.student.StudentProgressService;
import com.example.academic.enrollment.*;
import com.example.academic.attendance.*;
import com.example.academic.session.*;
import com.example.academic.schoolclass.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
class StudentAttendanceTest {
 Enrollment e;EnrollmentPeriod period;LocalDate date=LocalDate.of(2026,10,1);
 @BeforeEach void setup(){e=new Enrollment();var c=new SchoolClass();c.setStartDate(date);e.setSchoolClass(c);e.setEnrolledAt(date.atStartOfDay());e.setStudyStatus(Enrollment.StudyStatus.STUDYING);period=new EnrollmentPeriod();period.setActiveFrom(date);}
 ClassSession session(long id,int offset){var s=new ClassSession();s.setId(id);s.setSessionDate(date.plusDays(offset));return s;}
 Attendance record(ClassSession s,Attendance.Status status){var a=new Attendance();a.setId(s.getId());a.setClassSession(s);a.setSessionDate(s.getSessionDate());a.setStatus(status);return a;}
 StudentProgressService.AttendancePage read(List<ClassSession> ss,List<Attendance> aa){return StudentProgressService.assemble(e,List.of(period),ss,aa,0,20,null,null);}
 @Test void lateCountsAttendedAndPermittedAbsenceRemainsAbsent(){var s1=session(1,0);var s2=session(2,1);var a=record(s2,Attendance.Status.ABSENT);a.setHasPermission(true);var r=read(List.of(s1,s2),List.of(record(s1,Attendance.Status.LATE),a));assertEquals(1,r.summary().late());assertEquals(1,r.summary().absent());assertEquals(0,new BigDecimal("50").compareTo(r.summary().attendancePercent()));assertTrue(r.content().getFirst().hasPermission());}
 @Test void unrecordedNeverMeansAbsent(){var r=read(List.of(session(1,0)),List.of());assertEquals("NOT_RECORDED",r.content().getFirst().status());assertEquals(0,r.summary().absent());assertNull(r.summary().attendancePercent());assertFalse(r.summary().coverageComplete());}
 @Test void legacyDateMatchIsNotAJoin(){var s=session(1,0);var a=record(s,Attendance.Status.PRESENT);a.setClassSession(null);var r=read(List.of(s),List.of(a));assertEquals(2,r.content().size());assertEquals(1,r.summary().notRecorded());assertEquals(1,r.summary().legacyUnverified());assertNull(r.summary().attendancePercent());}
 @Test void exclusivePeriodEndExcluded(){period.setActiveUntil(date.plusDays(1));var r=read(List.of(session(1,0),session(2,1)),List.of());assertEquals(1,r.summary().eligibleCompleted());assertEquals(1,r.content().size());}
 @Test void legacyEnrollmentNeverProducesOfficialRate(){var s=session(1,0);var r=StudentProgressService.assemble(e,List.of(),List.of(s),List.of(record(s,Attendance.Status.PRESENT)),0,20,null,null);assertEquals(0,r.summary().eligibleCompleted());assertEquals("LEGACY_UNVERIFIED",r.content().getFirst().source());assertNull(r.summary().attendancePercent());}
 @Test void emptyDenominatorIsNull(){var r=read(List.of(),List.of());assertTrue(r.summary().coverageComplete());assertNull(r.summary().attendancePercent());}
 @Test void summaryDoesNotDependOnCurrentPageOrDateFilter(){var s1=session(1,0);var s2=session(2,1);var r=StudentProgressService.assemble(e,List.of(period),List.of(s1,s2),List.of(record(s1,Attendance.Status.PRESENT),record(s2,Attendance.Status.ABSENT)),0,1,date.plusDays(1),null);assertEquals(1,r.content().size());assertEquals(2,r.summary().eligibleCompleted());assertEquals(0,new BigDecimal("50").compareTo(r.summary().attendancePercent()));}
}
