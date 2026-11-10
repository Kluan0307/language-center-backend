package com.example.academic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.academic.attendance.*;
import com.example.academic.branch.*;
import com.example.academic.common.*;
import com.example.academic.course.*;
import com.example.academic.enrollment.*;
import com.example.academic.room.*;
import com.example.academic.schedule.*;
import com.example.academic.schoolclass.*;
import com.example.academic.security.*;
import com.example.academic.session.*;
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
    properties = "spring.datasource.url=jdbc:h2:mem:session-workflows;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class SessionWorkflowTest {
  @Autowired com.example.academic.duration.DurationPlanService durationPlans;
  @Autowired PlacementTestFixtures placementFixtures;
  @Autowired SessionPlanner planner;
  @Autowired ClassSessionRepository sessions;
  @Autowired SessionEventRepository events;
  @Autowired SchoolClassService classService;
  @Autowired SchoolClassRepository classes;
  @Autowired BranchRepository branches;
  @Autowired CourseRepository courses;
  @Autowired RoomRepository rooms;
  @Autowired RoomService roomService;
  @Autowired ScheduleRepository schedules;
  @Autowired ScheduleService scheduleService;
  @Autowired AttendanceService attendance;
  @Autowired AttendanceRepository attendanceRows;
  @Autowired EnrollmentRepository enrollments;
  @Autowired EnrollmentPeriodRepository periods;
  @Autowired EnrollmentService enrollmentService;
  @Autowired StudentRepository students;
  @MockitoBean TeacherDirectory teachers;
  @MockitoBean RemoteApi remote;
  Long a, b;
  LocalDate today;
  Room roomA, roomB;

  @BeforeEach
  void setup() {
    today = SessionPlanner.today();
    a = branch();
    b = branch();
    roomA = room(a);
    roomB = room(b);
    actor(Role.SUPER_ADMIN, 999L, null, List.of());
    when(teachers.require(anyLong(), anyLong()))
        .thenAnswer(
            i -> {
              Long id = i.getArgument(0), branch = i.getArgument(1);
              if (id == 3L && !branch.equals(a)) throw ApiException.missing();
              return new TeacherDirectory.Teacher(id, id + 100, "Teacher " + id, a, List.of(a, b));
            });
    when(remote.token()).thenReturn("Bearer test");
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  void actor(Role role, Long profile, Long branch, List<Long> ids) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(profile + 100, profile, branch, role, "actor-" + profile, ids),
                null,
                List.of()));
  }

  Long branch() {
    var x = new Branch();
    x.setCode("B" + UUID.randomUUID());
    x.setName("Branch");
    x.setPhone("0900000000");
    return branches.saveAndFlush(x).getId();
  }

  Room room(Long branch) {
    var r = new Room();
    r.setBranchId(branch);
    r.setCode("R" + UUID.randomUUID());
    r.setName("Room");
    r.setCapacity(20);
    return rooms.saveAndFlush(r);
  }

  SchoolClass school(
      Long branch,
      Long teacher,
      Room room,
      LocalDate from,
      LocalDate to,
      String start,
      String end) {
    var course = new Course();
    course.setBranchId(branch);
    course.setCode("CO" + UUID.randomUUID());
    course.setName("Course");
    course.setDuration(10);
    course.setMaxStudents(20);
    course.setTuitionFee(new BigDecimal("1000"));
    courses.saveAndFlush(course);
    var result =
        classService.save(
            null,
            new SchoolClassDto.Request(
                "C" + UUID.randomUUID(),
                "Class",
                course.getId(),
                teacher,
                "Teacher",
                room.getId(),
                from,
                to,
                10,
                branch,
                List.of(
                    new ScheduleDto.Slot(
                        today.getDayOfWeek(), LocalTime.parse(start), LocalTime.parse(end)))));
    return classes.findById(result.id()).orElseThrow();
  }

  SchoolClass school() {
    return school(a, 1L, roomA, today.minusDays(7), today.plusDays(21), "08:00", "09:00");
  }

  ClassSession session(SchoolClass c, LocalDate date) {
    return sessions.findBySchoolClassIdAndSessionDate(c.getId(), date).stream()
        .filter(s -> s.getStatus() != ClassSession.Status.CANCELLED)
        .findFirst()
        .orElseThrow();
  }

  Enrollment enroll(SchoolClass c, Student student) {
    var e = new Enrollment();
    e.setStudent(student);
    e.setSchoolClass(c);
    e.setTuitionFee(new BigDecimal("1000"));
    enrollments.saveAndFlush(e);
    var p = new EnrollmentPeriod();
    p.setEnrollment(e);
    p.setActiveFrom(c.getStartDate());
    periods.saveAndFlush(p);
    return e;
  }

  Student student(Long branch) {
    var s = new Student();
    s.setBranchId(branch);
    s.setFullName("Learner");
    s.setPhone("0900000000");
    s.setStudentCode("S" + UUID.randomUUID());
    return students.saveAndFlush(s);
  }

  SessionDto.Edit edit(
      ClassSession s, LocalDate date, Long teacher, Long room, String from, String to) {
    return new SessionDto.Edit(
        date,
        LocalTime.parse(from),
        LocalTime.parse(to),
        room,
        teacher,
        "Operational change",
        s.getVersion());
  }

  AttendanceDto.Bulk bulk(
      ClassSession s, Enrollment e, Attendance.Status status, String reason, Long version) {
    return new AttendanceDto.Bulk(
        s.getSchoolClass().getId(),
        s.getSessionDate(),
        List.of(new AttendanceDto.Item(e.getId(), status, status == Attendance.Status.ABSENT)),
        s.getId(),
        version,
        reason);
  }

  @Test
  void generationIsIdempotentAndUsesRealDates() {
    var c = school();
    long count = sessions.count(), audit = events.count();
    var r = planner.generate(new SessionDto.Generate(c.getId(), null, null));
    assertEquals(0, r.created());
    assertEquals(count, sessions.count());
    assertEquals(audit, events.count());
    assertNotNull(session(c, today));
  }

  @Test
  void weeklyChangesPreservePastTodayAndCustomizedFuture() {
    var c = school();
    var custom = session(c, today.plusDays(7));
    planner.edit(
        custom.getId(), edit(custom, custom.getSessionDate(), 2L, roomA.getId(), "12:00", "13:00"));
    var slot = schedules.findBySchoolClassId(c.getId()).getFirst();
    scheduleService.save(
        slot.getId(),
        new ScheduleDto.Request(
            c.getId(), today.getDayOfWeek(), LocalTime.of(10, 0), LocalTime.of(11, 0)));
    assertEquals(LocalTime.of(8, 0), session(c, today.minusDays(7)).getStartTime());
    assertEquals(LocalTime.of(8, 0), session(c, today).getStartTime());
    assertEquals(LocalTime.of(12, 0), session(c, today.plusDays(7)).getStartTime());
    assertEquals(2L, session(c, today.plusDays(7)).getTeacherId());
    assertEquals(LocalTime.of(10, 0), session(c, today.plusDays(14)).getStartTime());
  }

  @Test
  void cancelledActualSessionFreesRoomAndTeacherDespiteWeeklyTemplate() {
    var c = school();
    var s = session(c, today);
    planner.cancel(s.getId(), new SessionDto.Action("Holiday", s.getVersion()));
    assertDoesNotThrow(() -> school(a, 1L, roomA, today, today, "08:00", "09:00"));
  }

  @Test
  void sameTeacherCannotOverlapAcrossBranches() {
    school();
    assertThrows(ApiException.class, () -> school(b, 1L, roomB, today, today, "08:30", "09:30"));
  }

  @Test
  void sameRoomCannotOverlapWithAnotherTeacher() {
    school();
    assertThrows(ApiException.class, () -> school(a, 2L, roomA, today, today, "08:30", "09:30"));
  }

  @Test
  void oneActiveSessionPerClassDateIsEnforced() {
    var c = school();
    var s = session(c, today.plusDays(7));
    assertThrows(
        ApiException.class,
        () -> planner.edit(s.getId(), edit(s, today, 1L, roomA.getId(), "15:00", "16:00")));
  }

  @Test
  void makeupMayExtendPastPlannedEndWithoutChangingPlannedDate() {
    var c = school();
    var s = session(c, today.plusDays(7));
    planner.cancel(s.getId(), new SessionDto.Action("Holiday", s.getVersion()));
    var actualDate = c.getEndDate().plusDays(3);
    var result =
        planner.makeup(s.getId(), edit(s, actualDate, 2L, roomA.getId(), "10:00", "11:00"));
    assertEquals(ClassSession.Kind.MAKEUP, result.kind());
    assertEquals(s.getId(), result.makeupForId());
    assertEquals(actualDate, c.effectiveEndDate());
    assertEquals(today.plusDays(21), c.getEndDate());
    assertEquals(result.id(), s.getMakeupSessionId());
  }

  @Test
  void restoringSourceCannotCreateDuplicateMakeupObligation() {
    var c = school();
    var s = session(c, today);
    planner.cancel(s.getId(), new SessionDto.Action("Holiday", s.getVersion()));
    planner.makeup(s.getId(), edit(s, today.plusDays(1), 2L, roomA.getId(), "10:00", "11:00"));
    assertThrows(
        ApiException.class,
        () ->
            planner.restore(s.getId(), new SessionDto.Action("Undo cancellation", s.getVersion())));
  }

  @Test
  void substituteCanAttendOnlyItsSessionAndCannotOpenClassRoster() {
    var c = school();
    var e = enroll(c, student(a));
    var s = session(c, today);
    planner.edit(s.getId(), edit(s, today, 2L, roomA.getId(), "08:00", "09:00"));
    actor(Role.TEACHER, 2L, a, List.of(a, b));
    var sheet = attendance.sheet(null, null, s.getId());
    assertTrue(sheet.canEdit());
    assertFalse(sheet.canViewClassHistory());
    assertEquals(1, sheet.records().size());
    assertThrows(ApiException.class, () -> classService.students(c.getId()));
    assertThrows(ApiException.class, () -> attendance.summary(c.getId()));
    var saved = attendance.bulk(bulk(s, e, Attendance.Status.PRESENT, null, s.getVersion()));
    assertEquals(ClassSession.Status.COMPLETED, saved.sessionStatus());
    assertFalse(saved.canEdit());
  }

  @Test
  void mainTeacherCannotWriteSessionAssignedToSubstitute() {
    var c = school();
    enroll(c, student(a));
    var s = session(c, today);
    planner.edit(s.getId(), edit(s, today, 2L, roomA.getId(), "08:00", "09:00"));
    actor(Role.TEACHER, 1L, a, List.of(a));
    assertFalse(attendance.sheet(null, null, s.getId()).canEdit());
  }

  @Test
  void completedSessionIsFrozenAndCorrectionRequiresReasonAndVersion() {
    var c = school();
    var e = enroll(c, student(a));
    var s = session(c, today);
    attendance.bulk(bulk(s, e, Attendance.Status.PRESENT, null, s.getVersion()));
    assertThrows(
        ApiException.class,
        () -> planner.edit(s.getId(), edit(s, today, 2L, roomA.getId(), "10:00", "11:00")));
    assertThrows(
        ApiException.class,
        () -> planner.cancel(s.getId(), new SessionDto.Action("Cancel learned", s.getVersion())));
    assertThrows(
        ApiException.class,
        () -> attendance.bulk(bulk(s, e, Attendance.Status.ABSENT, null, s.getVersion())));
    var previous = s.getVersion();
    attendance.bulk(bulk(s, e, Attendance.Status.ABSENT, "Correct attendance", previous));
    assertTrue(s.getVersion() > previous);
    assertEquals(1, attendance.changes(s.getId()).size());
    assertEquals(Attendance.Status.PRESENT, attendance.changes(s.getId()).getFirst().oldStatus());
    assertThrows(
        ApiException.class,
        () -> attendance.bulk(bulk(s, e, Attendance.Status.LATE, "Stale correction", previous)));
  }

  @Test
  void identicalRetryDoesNotRewriteAttendanceOrAudit() {
    var c = school();
    var e = enroll(c, student(a));
    var s = session(c, today);
    actor(Role.TEACHER, 1L, a, List.of(a));
    var request = bulk(s, e, Attendance.Status.PRESENT, null, s.getVersion());
    attendance.bulk(request);
    var version = s.getVersion();
    var count = events.count();
    attendance.bulk(request);
    assertEquals(version, s.getVersion());
    assertEquals(count, events.count());
  }

  @Test
  void cancelledSessionCannotBeAttended() {
    var c = school();
    var s = session(c, today);
    planner.cancel(s.getId(), new SessionDto.Action("Holiday", s.getVersion()));
    assertThrows(ApiException.class, () -> attendance.sheet(null, null, s.getId()));
  }

  @Test
  void sessionsCannotBeMovedIntoStudentConflict() {
    var c1 = school();
    var otherRoom = room(a);
    var c2 = school(a, 2L, otherRoom, today, today, "10:00", "11:00");
    var learner = student(a);
    enroll(c1, learner);
    enroll(c2, learner);
    var s = session(c2, today);
    assertThrows(
        ApiException.class,
        () -> planner.edit(s.getId(), edit(s, today, 2L, otherRoom.getId(), "08:30", "09:30")));
  }

  @Test
  void enrollmentChecksActualSessionsInsteadOfCancelledWeeklySlots() {
    var c1 = school();
    var s = session(c1, today);
    planner.cancel(s.getId(), new SessionDto.Action("Holiday", s.getVersion()));
    var c2 = school(a, 2L, room(a), today, today, "08:00", "09:00");
    var learner = student(a);
    enroll(c1, learner);
    placementFixtures.open(c2.getId());
    assertDoesNotThrow(
        () -> enrollmentService.enroll(new EnrollmentDto.Request(learner.getId(), c2.getId())));
  }

  @Test
  void roomCannotEnterMaintenanceWhileSessionsRemain() {
    school();
    assertThrows(
        ApiException.class, () -> roomService.status(roomA.getId(), Room.Status.MAINTENANCE));
  }

  @Test
  void closingClassRequiresExplicitCancellationAndRetainsCompleted() {
    var c = school();
    var e = enroll(c, student(a));
    var s = session(c, today);
    attendance.bulk(bulk(s, e, Attendance.Status.PRESENT, null, s.getVersion()));
    assertThrows(
        ApiException.class, () -> classService.status(c.getId(), SchoolClass.Status.FINISHED));
    for (var pending : sessions.findBySchoolClassIdOrderBySessionDateAsc(c.getId()))
      if (pending.getStatus() == ClassSession.Status.SCHEDULED)
        planner.cancel(
            pending.getId(),
            new SessionDto.Action("Explicitly resolved before closing", pending.getVersion()));
    var plan = durationPlans.getClass(c.getId());
    durationPlans.confirmClass(
        c.getId(),
        new com.example.academic.duration.DurationDto.ClassRequest(
            plan.revision(),
            com.example.academic.duration.ClassDurationPlan.Source.CUSTOM,
            null,
            Math.toIntExact(
                com.example.academic.duration.DurationProgressService.minutes(
                    s.getStartTime(), s.getEndTime())),
            "Verified taught plan after review"));
    assertThrows(ApiException.class, () -> classService.status(c.getId(), SchoolClass.Status.FINISHED));
    assertEquals(ClassSession.Status.COMPLETED, s.getStatus());
    assertTrue(
        sessions.findBySchoolClassIdOrderBySessionDateAsc(c.getId()).stream()
            .filter(x -> !x.getId().equals(s.getId()))
            .allMatch(x -> x.getStatus() == ClassSession.Status.CANCELLED));
  }

  @Test
  void staleSessionEditCannotOverwriteNewerChange() {
    var c = school();
    var s = session(c, today);
    var request = edit(s, today, 2L, roomA.getId(), "10:00", "11:00");
    planner.edit(s.getId(), request);
    assertThrows(ApiException.class, () -> planner.edit(s.getId(), request));
  }
}
