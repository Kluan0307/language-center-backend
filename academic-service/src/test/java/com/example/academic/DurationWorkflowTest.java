package com.example.academic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.academic.branch.*;
import com.example.academic.common.*;
import com.example.academic.course.*;
import com.example.academic.duration.*;
import com.example.academic.room.*;
import com.example.academic.schoolclass.*;
import com.example.academic.security.*;
import com.example.academic.session.*;
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
    properties =
        "spring.datasource.url=jdbc:h2:mem:duration-workflows;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class DurationWorkflowTest {
  @Autowired DurationPlanService plans;
  @Autowired DurationProgressService progress;
  @Autowired CourseService courseService;
  @Autowired CourseRepository courses;
  @Autowired SchoolClassService classService;
  @Autowired SchoolClassRepository classes;
  @Autowired BranchRepository branches;
  @Autowired RoomRepository rooms;
  @Autowired ClassSessionRepository sessions;
  @Autowired SessionPlanner planner;
  @MockitoBean TeacherDirectory teachers;
  Long branch;
  Course course;
  SchoolClass school;
  Room room;

  void actor(Role role, Long profile) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(
                    99L,
                    profile,
                    branch,
                    role,
                    "actor",
                    branch == null ? List.of() : List.of(branch)),
                null,
                List.of()));
  }

  @BeforeEach
  void setup() {
    var b = new Branch();
    b.setCode(UUID.randomUUID().toString());
    b.setName("Branch");
    b.setPhone("0900000000");
    branch = branches.saveAndFlush(b).getId();
    actor(Role.ADMIN, 7L);
    when(teachers.require(anyLong(), anyLong()))
        .thenAnswer(
            i ->
                new TeacherDirectory.Teacher(
                    i.getArgument(0), 100L, "Teacher", branch, List.of(branch)));
    course = new Course();
    course.setCode(UUID.randomUUID().toString());
    course.setName("Course");
    course.setBranchId(branch);
    course.setDuration(24);
    course.setTuitionFee(BigDecimal.TEN);
    course.setMaxStudents(10);
    courses.saveAndFlush(course);
    room = new Room();
    room.setCode(UUID.randomUUID().toString());
    room.setName("Room");
    room.setBranchId(branch);
    room.setCapacity(10);
    rooms.saveAndFlush(room);
    school = openClass();
  }

  SchoolClass openClass() {
    var dto =
        classService.save(
            null,
            new SchoolClassDto.Request(
                UUID.randomUUID().toString(),
                "Class",
                course.getId(),
                7L,
                "Teacher",
                room.getId(),
                LocalDate.now(BusinessTime.ZONE).minusDays(7),
                LocalDate.now(BusinessTime.ZONE).plusDays(30),
                10,
                branch,
                List.of()));
    return classes.findById(dto.id()).orElseThrow();
  }

  void custom(int minutes) {
    plans.confirmClass(
        school.getId(),
        new DurationDto.ClassRequest(
            school.getDurationRevision(),
            ClassDurationPlan.Source.CUSTOM,
            null,
            minutes,
            "Verified lesson plan"));
  }

  ClassSession session(int start, int end, ClassSession.Status status) {
    var s = new ClassSession();
    s.setSchoolClass(school);
    s.setSessionDate(
        LocalDate.now(BusinessTime.ZONE)
            .minusDays(2)
            .plusDays(sessions.findBySchoolClassIdOrderBySessionDateAsc(school.getId()).size()));
    s.setStartTime(LocalTime.of(start / 60, start % 60));
    s.setEndTime(LocalTime.of(end / 60, end % 60));
    s.setStatus(status);
    s.setTeacherId(7L);
    s.setTeacherName("Teacher");
    s.setRoomId(room.getId());
    s.setRoomName("Room");
    return sessions.saveAndFlush(s);
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void legacy24DoesNotBecomeMinutesOrHours() {
    var p = plans.getCourse(course.getId());
    assertEquals(24, p.legacyDuration());
    assertNull(p.expectedMinutes());
    assertFalse(p.verified());
    assertNull(progress.get(school.getId()).expectedMinutes());
  }

  @Test
  void ninetyMinutesRoundTripsAndKeepsLegacy() {
    var p =
        plans.confirmCourse(
            course.getId(), new DurationDto.CourseRequest(0L, 90, "Verified syllabus"));
    assertEquals(90, p.expectedMinutes());
    assertEquals(24, p.legacyDuration());
    assertEquals(1, p.revision());
  }

  @Test
  void newCourseCanHaveMinutesWithoutInventingLegacyValue() {
    var c =
        courseService.save(
            null,
            new CourseDto.Request(
                UUID.randomUUID().toString(),
                "New",
                null,
                null,
                null,
                BigDecimal.TEN,
                null,
                null,
                10,
                branch,
                90));
    assertNull(c.duration());
    assertEquals(90, c.expectedMinutes());
    assertEquals(1, c.durationRevision());
  }

  @Test
  void classCopiesCourseAtCreationOnly() {
    plans.confirmCourse(course.getId(), new DurationDto.CourseRequest(0L, 180, "Original plan"));
    var c = openClass();
    plans.confirmCourse(course.getId(), new DurationDto.CourseRequest(1L, 240, "Revised syllabus"));
    assertEquals(180, plans.getClass(c.getId()).expectedMinutes());
    assertEquals(240, plans.getClass(c.getId()).course().expectedMinutes());
  }

  @Test
  void oldClassNeedsExplicitConfirmation() {
    plans.confirmCourse(
        course.getId(), new DurationDto.CourseRequest(0L, 180, "Verified syllabus"));
    assertNull(plans.getClass(school.getId()).expectedMinutes());
    var p =
        plans.confirmClass(
            school.getId(),
            new DurationDto.ClassRequest(
                school.getDurationRevision(),
                ClassDurationPlan.Source.COURSE,
                1L,
                null,
                "Apply reviewed syllabus"));
    assertEquals(180, p.expectedMinutes());
  }

  @Test
  void staleClassOrCourseRevisionCannotBeApplied() {
    plans.confirmCourse(course.getId(), new DurationDto.CourseRequest(0L, 90, "Verified syllabus"));
    assertThrows(
        ApiException.class,
        () ->
            plans.confirmClass(
                school.getId(),
                new DurationDto.ClassRequest(
                    0L, ClassDurationPlan.Source.COURSE, 1L, null, "Stale editor")));
    assertThrows(
        ApiException.class,
        () ->
            plans.confirmClass(
                school.getId(),
                new DurationDto.ClassRequest(
                    school.getDurationRevision(),
                    ClassDurationPlan.Source.COURSE,
                    2L,
                    null,
                    "Wrong course version")));
  }

  @Test
  void staleCourseRevisionCannotOverwrite() {
    plans.confirmCourse(course.getId(), new DurationDto.CourseRequest(0L, 90, "Verified syllabus"));
    assertThrows(
        ApiException.class,
        () ->
            plans.confirmCourse(
                course.getId(), new DurationDto.CourseRequest(0L, 120, "Old editor")));
  }

  @Test
  void minutesAreBoundedAndCustomHasAudit() {
    for (int value : List.of(0, -1, 600001)) assertThrows(ApiException.class, () -> custom(value));
    custom(90);
    var history = plans.classHistory(school.getId());
    assertEquals(2, history.size());
    assertEquals("Verified lesson plan", history.getFirst().getReason());
    assertEquals("actor", history.getFirst().getActorName());
  }

  @Test
  void sharedCourseRequiresSuperAdmin() {
    course.setBranchId(null);
    courses.flush();
    assertThrows(
        ApiException.class,
        () ->
            plans.confirmCourse(
                course.getId(), new DurationDto.CourseRequest(0L, 90, "Shared plan")));
    actor(Role.SUPER_ADMIN, 7L);
    assertTrue(
        plans
            .confirmCourse(course.getId(), new DurationDto.CourseRequest(0L, 90, "Shared plan"))
            .verified());
  }

  @Test
  void staffAndTeacherCannotConfirmPlans() {
    for (var role : List.of(Role.STAFF, Role.TEACHER)) {
      actor(role, 7L);
      assertThrows(
          ApiException.class,
          () ->
              plans.confirmCourse(
                  course.getId(), new DurationDto.CourseRequest(0L, 90, "Denied role")));
      assertThrows(ApiException.class, () -> custom(90));
    }
  }

  @Test
  void teacherOnlyReadsAssignedClassAndBatchIsAllOrNothing() {
    actor(Role.TEACHER, 8L);
    assertThrows(ApiException.class, () -> progress.get(school.getId()));
    actor(Role.TEACHER, 7L);
    assertEquals(1, progress.batch(List.of(school.getId())).size());
    var old = branch;
    branch += 100;
    actor(Role.STAFF, 7L);
    assertThrows(ApiException.class, () -> plans.getClass(school.getId()));
    branch = old;
  }

  @Test
  void twoNinetyMinuteCompletedSessionsEqualThreeHours() {
    custom(180);
    session(480, 570, ClassSession.Status.COMPLETED);
    session(600, 690, ClassSession.Status.COMPLETED);
    var p = progress.get(school.getId());
    assertEquals(180L, p.scheduledMinutes());
    assertEquals(180L, p.taughtMinutes());
    assertTrue(p.timeReadyForClosure());
  }

  @Test
  void pastScheduledIsNotTaught() {
    custom(90);
    session(480, 570, ClassSession.Status.SCHEDULED);
    var p = progress.get(school.getId());
    assertEquals(90L, p.scheduledMinutes());
    assertEquals(0L, p.taughtMinutes());
    assertEquals(1, p.overdueSessions());
    assertFalse(p.timeReadyForClosure());
  }

  @Test
  void cancelledDoesNotCountOrCreateUnknown() {
    custom(90);
    var s = session(480, 570, ClassSession.Status.CANCELLED);
    s.setStartTime(null);
    sessions.flush();
    var p = progress.get(school.getId());
    assertEquals(0L, p.scheduledMinutes());
    assertEquals(0, p.unknownActiveSessions());
    assertEquals(1, p.cancelledSessions());
  }

  @Test
  void unknownCompletedTimeCannotBecomeZeroOrSufficient() {
    custom(90);
    session(480, 570, ClassSession.Status.COMPLETED);
    var s = session(600, 690, ClassSession.Status.COMPLETED);
    s.setStartTime(null);
    sessions.flush();
    var p = progress.get(school.getId());
    assertNull(p.scheduledMinutes());
    assertNull(p.taughtMinutes());
    assertEquals(90, p.knownTaughtMinutes());
    assertEquals("UNKNOWN", p.dataState());
    assertFalse(p.timeReadyForClosure());
  }

  @Test
  void fractionalMinuteOrOvernightIsUnknown() {
    assertNull(DurationProgressService.minutes(LocalTime.of(9, 0, 1), LocalTime.of(10, 0)));
    assertNull(DurationProgressService.minutes(LocalTime.of(23, 30), LocalTime.of(1, 0)));
    assertEquals(90L, DurationProgressService.minutes(LocalTime.of(9, 0), LocalTime.of(10, 30)));
  }

  @Test
  void makeupCountsOnceAfterSourceCancellation() {
    custom(90);
    var original = session(480, 570, ClassSession.Status.CANCELLED);
    var makeup = session(600, 690, ClassSession.Status.COMPLETED);
    makeup.setKind(ClassSession.Kind.MAKEUP);
    makeup.setMakeupForId(original.getId());
    original.setMakeupSessionId(makeup.getId());
    sessions.flush();
    var p = progress.get(school.getId());
    assertEquals(90L, p.taughtMinutes());
    assertEquals(1, p.cancelledSessions());
    assertTrue(p.timeReadyForClosure());
  }

  @Test
  void invalidMakeupDoesNotDoubleCount() {
    custom(90);
    var original = session(480, 570, ClassSession.Status.COMPLETED);
    var makeup = session(600, 690, ClassSession.Status.COMPLETED);
    makeup.setKind(ClassSession.Kind.MAKEUP);
    makeup.setMakeupForId(original.getId());
    sessions.flush();
    var p = progress.get(school.getId());
    assertNull(p.taughtMinutes());
    assertEquals(1, p.invalidMakeupSessions());
    assertFalse(p.timeReadyForClosure());
  }

  @Test
  void futureCompletedIsUnknown() {
    custom(90);
    var s = session(480, 570, ClassSession.Status.COMPLETED);
    s.setSessionDate(LocalDate.now(BusinessTime.ZONE).plusDays(1));
    sessions.flush();
    assertNull(progress.get(school.getId()).taughtMinutes());
  }

  @Test
  void activeMissingTimeKeepsKnownCompletedButBlocksClosure() {
    custom(90);
    session(480, 570, ClassSession.Status.COMPLETED);
    var s = session(600, 690, ClassSession.Status.SCHEDULED);
    s.setEndTime(null);
    sessions.flush();
    var p = progress.get(school.getId());
    assertEquals(90L, p.taughtMinutes());
    assertNull(p.scheduledMinutes());
    assertFalse(p.timeReadyForClosure());
  }

  @Test
  void closeCannotSilentlyCancelUnfinishedSessions() {
    custom(90);
    session(480, 570, ClassSession.Status.SCHEDULED);
    assertThrows(
        ApiException.class, () -> classService.status(school.getId(), SchoolClass.Status.FINISHED));
    assertEquals(
        ClassSession.Status.SCHEDULED,
        sessions.findBySchoolClassIdOrderBySessionDateAsc(school.getId()).getFirst().getStatus());
  }

  @Test
  void closesOnlyWithVerifiedMinutesAndProcessedSessions() {
    session(480, 570, ClassSession.Status.COMPLETED);
    assertThrows(
        ApiException.class, () -> classService.status(school.getId(), SchoolClass.Status.FINISHED));
    custom(90);
    assertThrows(ApiException.class, () -> classService.status(school.getId(), SchoolClass.Status.FINISHED));
    assertTrue(progress.get(school.getId()).timeReadyForClosure());
  }

  @Test
  void cancelledAndRestoredTotalsFollowStatus() {
    custom(90);
    var s = session(480, 570, ClassSession.Status.SCHEDULED);
    var cancelled =
        planner.cancel(s.getId(), new SessionDto.Action("Reviewed cancellation", s.getVersion()));
    assertEquals(0L, progress.get(school.getId()).scheduledMinutes());
    planner.restore(s.getId(), new SessionDto.Action("Reviewed restore", cancelled.version()));
    assertEquals(90L, progress.get(school.getId()).scheduledMinutes());
  }

  @Test
  void batchLimitsAndOrderAreExplicit() {
    var second = openClass();
    var list = progress.batch(List.of(second.getId(), school.getId()));
    assertEquals(second.getId(), list.getFirst().classId());
    assertThrows(ApiException.class, () -> progress.batch(List.of()));
    assertThrows(ApiException.class, () -> progress.batch(List.of(school.getId(), school.getId())));
    assertThrows(ApiException.class, () -> progress.batch(List.of(school.getId(), 999999L)));
  }

  @Test
  void normalCourseEditCannotOverwriteVerifiedPlan() {
    plans.confirmCourse(course.getId(), new DurationDto.CourseRequest(0L, 90, "Verified syllabus"));
    assertThrows(
        ApiException.class,
        () ->
            courseService.save(
                course.getId(),
                new CourseDto.Request(
                    course.getCode(),
                    course.getName(),
                    null,
                    null,
                    24,
                    BigDecimal.TEN,
                    null,
                    null,
                    10,
                    branch,
                    120)));
    assertEquals(24, course.getDuration());
  }

  @Autowired jakarta.persistence.EntityManager entityManager;

  @Test
  void batchUsesTwoSetQueriesInsteadOfPerClassQueries() {
    var second = openClass();
    var third = openClass();
    var ids = List.of(school.getId(), second.getId(), third.getId());
    entityManager.flush();
    entityManager.clear();
    var statistics =
        entityManager
            .getEntityManagerFactory()
            .unwrap(org.hibernate.SessionFactory.class)
            .getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();
    assertEquals(3, progress.batch(ids).size());
    assertEquals(2, statistics.getPrepareStatementCount());
  }

 @Autowired com.example.academic.evaluation.EvaluationContextService evaluationContext;
 @Autowired com.example.academic.enrollment.EnrollmentRepository enrollmentRows;
 @Autowired com.example.academic.enrollment.EnrollmentPeriodRepository enrollmentPeriods;
 @Autowired com.example.academic.student.StudentRepository studentRows;
 @Test void evaluationContextRetainsReservedEnrollmentAndHalfOpenPeriod(){var student=new com.example.academic.student.Student();student.setFullName("Learner");student.setPhone("0900000000");student.setBranchId(branch);studentRows.saveAndFlush(student);var e=new com.example.academic.enrollment.Enrollment();e.setSchoolClass(school);e.setStudent(student);e.setTuitionFee(BigDecimal.TEN);e.setStudyStatus(com.example.academic.enrollment.Enrollment.StudyStatus.RESERVED);enrollmentRows.saveAndFlush(e);var p=new com.example.academic.enrollment.EnrollmentPeriod();p.setEnrollment(e);p.setActiveFrom(LocalDate.now(BusinessTime.ZONE).minusDays(2));p.setActiveUntil(LocalDate.now(BusinessTime.ZONE));enrollmentPeriods.saveAndFlush(p);var c=evaluationContext.get(school.getId(),false);assertEquals(e.getId(),c.enrollments().getFirst().enrollmentId());assertEquals("RESERVED",c.enrollments().getFirst().studyStatus());assertEquals(p.getActiveUntil(),c.enrollments().getFirst().periods().getFirst().activeUntil());assertTrue(c.attendance().isEmpty());}
 @Test void evaluationContextDoesNotGrantCrmToStaffOrAnotherTeacher(){actor(Role.STAFF,7L);assertThrows(ApiException.class,()->evaluationContext.get(school.getId(),true));actor(Role.TEACHER,8L);assertThrows(ApiException.class,()->evaluationContext.get(school.getId(),true));}
}
