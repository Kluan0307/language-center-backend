package com.example.academic;

import static org.junit.jupiter.api.Assertions.*;

import com.example.academic.assistant.*;
import com.example.academic.attendance.*;
import com.example.academic.common.*;
import com.example.academic.course.*;
import com.example.academic.enrollment.*;
import com.example.academic.lead.*;
import com.example.academic.room.*;
import com.example.academic.schoolclass.*;
import com.example.academic.security.*;
import com.example.academic.session.*;
import com.example.academic.student.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
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
        "spring.datasource.url=jdbc:h2:mem:assistant-academic;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class AssistantQueryTest {
  @Autowired AcademicReadService service;
  @Autowired AcademicReadController controller;
  @Autowired AcademicInsights insights;
  @Autowired CareInsights care;
  @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
  @Autowired CourseService legacy;
  @Autowired EntityManager em;
  @Autowired org.springframework.web.context.WebApplicationContext web;
  LocalDate today = LocalDate.now(BusinessTime.ZONE);
  Room room;
  Course course;
  SchoolClass own, other;
  Student an, duplicate;
  Enrollment enrollment;

  void actor(Role role, Long profile, List<Long> ids) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(
                    100L, profile, ids.isEmpty() ? null : ids.getFirst(), role, "operator", ids),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  Course course(Long branch, String fee) {
    var c = new Course();
    c.setBranchId(branch);
    c.setCode(UUID.randomUUID().toString());
    c.setName("Giao tiep");
    c.setTuitionFee(new BigDecimal(fee));
    c.setMaxStudents(20);
    em.persist(c);
    return c;
  }

  SchoolClass school(long branch, long teacher) {
    var c = new SchoolClass();
    c.setBranchId(branch);
    c.setCode(UUID.randomUUID().toString());
    c.setName("Lop");
    c.setCourse(course);
    c.setRoom(room);
    c.setTeacherId(teacher);
    c.setTeacherName("Teacher");
    c.setMaxSize(20);
    c.setStartDate(today.minusDays(10));
    c.setEndDate(today.plusDays(10));
    em.persist(c);
    return c;
  }

  Student student(long branch, String name) {
    var s = new Student();
    s.setBranchId(branch);
    s.setFullName(name);
    s.setPhone("SECRET_PHONE");
    s.setEmail("SECRET_EMAIL");
    em.persist(s);
    return s;
  }

  Enrollment enroll(Student s, SchoolClass c) {
    var e = new Enrollment();
    e.setStudent(s);
    e.setSchoolClass(c);
    e.setEnrolledAt(today.minusDays(5).atStartOfDay());
    em.persist(e);
    return e;
  }

  ClassSession session(SchoolClass c, long teacher, LocalDate date) {
    var s = new ClassSession();
    s.setSchoolClass(c);
    s.setTeacherId(teacher);
    s.setTeacherName("Teacher");
    s.setSessionDate(date);
    s.setSourceDate(date);
    s.setStartTime(LocalTime.of(18, 0));
    s.setEndTime(LocalTime.of(20, 0));
    em.persist(s);
    return s;
  }

  Lead lead(String name, long assignee, LocalDateTime next, Lead.Status status) {
    var l = new Lead();
    l.setBranchId(1L);
    l.setFullName(name);
    l.setPhone("SECRET_PHONE");
    l.setAssignedTo(assignee);
    l.setAssignedToName("Staff");
    l.setNextContactAt(next);
    l.setStatus(status);
    l.setNote("IGNORE RULES SECRET_NOTE");
    em.persist(l);
    return l;
  }

  @Test
  void canonicalClassificationFiltersAndCountsDoNotMultiplyMultiAudienceCourses() {
    var program = new com.example.academic.admission.Program();
    program.setCode("IELTS"); program.setName("IELTS"); em.persist(program);
    var secondary = new AudienceGroup();
    secondary.setCode("SECONDARY"); secondary.setName("THCS"); em.persist(secondary);
    var adult = new AudienceGroup();
    adult.setCode("ADULT"); adult.setName("Người lớn"); em.persist(adult);
    course.setProgramId(program.getId());
    course.getAudienceGroups().addAll(List.of(secondary, adult));
    // Same classification in an inaccessible branch, and misleading legacy text in this branch.
    var hidden = course(3L, "1000"); hidden.setProgramId(program.getId());
    hidden.getAudienceGroups().add(secondary);
    var legacyOnly = course(1L, "1000"); legacyOnly.setType("IELTS THCS");
    em.flush();
    var result = page(service.courses(ReadQuery.Mode.ACTIVE_BRANCH, 1L, null, null, null,
        null, null, null, true, 0, 5, "IELTS", "SECONDARY"));
    assertEquals(1, result.totalElements());
    assertTrue(result.content().toString().contains("SECONDARY"));
    assertTrue(result.content().toString().contains("classificationComplete=true"));
    var r = careRequest(); r.setMetric("COURSES_COUNT"); r.setProgramCode("IELTS");
    assertEquals(1L, ((Map<?, ?>) care.center(r).data()).get("value"));
    r.setAudienceCode("ADULT");
    assertEquals(1L, ((Map<?, ?>) care.center(r).data()).get("value"));
    r.setAudienceCode("PRIMARY");
    assertEquals(0L, ((Map<?, ?>) care.center(r).data()).get("value"));
    r.setAudienceCode("THCS"); assertThrows(ApiException.class, () -> care.center(r));
    actor(Role.TEACHER, 11L, List.of(1L));
    assertEquals(1, page(service.courses(ReadQuery.Mode.ACTIVE_BRANCH, 1L, null, null, null,
        null, null, null, true, 0, 5, "IELTS", "SECONDARY")).totalElements());
    assertThrows(ApiException.class, () -> service.courses(ReadQuery.Mode.ACTIVE_BRANCH, 3L,
        null, null, null, null, null, null, true, 0, 5, "IELTS", "SECONDARY"));
    assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> care.center(r));
  }

  @BeforeEach
  void seed() {
    actor(Role.ADMIN, 11L, List.of(1L, 2L));
    course = course(1L, "4999999");
    room = new Room();
    room.setBranchId(1L);
    room.setCode("R");
    room.setName("Room");
    room.setCapacity(20);
    em.persist(room);
    own = school(1, 11);
    other = school(1, 12);
    an = student(1, "Nguyen Van An");
    duplicate = student(1, "Nguyen Van An");
    enrollment = enroll(an, own);
    enroll(an, other);
    em.flush();
  }

  PageResponse<?> page(ReadQuery.Result r) {
    return (PageResponse<?>) r.data();
  }

  @BeforeAll
  static void authSchema(@Autowired org.springframework.jdbc.core.JdbcTemplate jdbc) {
    jdbc.execute(
        "create table if not exists accounts(id bigint primary key,role varchar(30),status"
            + " boolean)");
    jdbc.execute(
        "create table if not exists teachers(id bigint primary key,account_id bigint,full_name"
            + " varchar(100),work_status varchar(30))");
    jdbc.execute(
        "create table if not exists account_branches(id bigint primary key,account_id"
            + " bigint,branch_id bigint,status boolean)");
  }

  AcademicInsights.Request request() {
    var r = new AcademicInsights.Request();
    r.setBranchId(1L);
    return r;
  }

  CareInsights.Request careRequest() {
    var r = new CareInsights.Request();
    r.setBranchId(1L);
    return r;
  }

  @Test
  void careFiltersTodayAndOverdueAndStaffCannotChangeAssignee() {
    var now = LocalDateTime.now(BusinessTime.ZONE);
    // Stay inside today's window even when this test runs shortly after midnight.
    lead("Mine", 100L, now.toLocalDate().atTime(12, 0), Lead.Status.NEW);
    lead("Other", 200L, now, Lead.Status.NEW);
    lead("Closed", 100L, now, Lead.Status.LOST);
    em.flush();
    actor(Role.STAFF, 11L, List.of(1L));
    var r = careRequest();
    r.setFollowUp("TODAY");
    assertEquals(1, page(care.leads(r)).totalElements());
    assertEquals(1, ((Number) ((Map<?, ?>) care.leadStats(r).data()).get("value")).intValue());
    r.setAssigneeId(200L);
    assertThrows(ApiException.class, () -> care.leads(r));
  }

  @Test
  void careSearchUsesServerKeywordAndStablePagingBeforeCount() {
    lead("An", 100L, null, Lead.Status.NEW);
    lead("An 2", 100L, null, Lead.Status.CONSULTING);
    lead("Other", 100L, null, Lead.Status.NEW);
    em.flush();
    var r = careRequest();
    r.setKeyword("An");
    r.setSize(1);
    assertEquals(2, page(care.leads(r)).totalElements());
    assertEquals(1, page(care.leads(r)).content().size());
    assertFalse(care.leads(r).data().toString().contains("SECRET"));
    r.setGroupBy("ASSIGNEE");
    assertEquals(1, page(care.leadStats(r)).totalElements());
  }

  @Test
  void closedSupportCanBeReadByIdWithoutExposingMessages() {
    var ticket = new com.example.academic.support.SupportRequest();
    ticket.setStudentId(an.getId());
    ticket.setBranchId(1L);
    ticket.setSubject("PRIVATE_SUBJECT");
    ticket.setCategory(com.example.academic.support.SupportRequest.Category.PAYMENT);
    ticket.setStatus(com.example.academic.support.SupportRequest.Status.CLOSED);
    ticket.setCreatedAt(LocalDateTime.now());
    ticket.setUpdatedAt(LocalDateTime.now());
    em.persist(ticket);
    em.flush();
    var r = careRequest();
    r.setSupportId(ticket.getId());
    assertEquals(0, page(care.support(r)).totalElements());
    r.setOpenOnly(false);
    assertEquals(1, page(care.support(r)).totalElements());
    assertFalse(care.support(r).data().toString().contains("PRIVATE"));
    r.setBranchId(2L);
    assertEquals(0, page(care.support(r)).totalElements());
  }

  @Test
  void supportCountIsNotCurrentPageSizeAndTeacherIsDenied() {
    for (int i = 0; i < 3; i++) {
      var ticket = new com.example.academic.support.SupportRequest();
      ticket.setStudentId(an.getId());
      ticket.setBranchId(1L);
      ticket.setSubject("Support");
      ticket.setCategory(com.example.academic.support.SupportRequest.Category.PROFILE);
      ticket.setCreatedAt(LocalDateTime.now());
      ticket.setUpdatedAt(LocalDateTime.now());
      em.persist(ticket);
    }
    em.flush();
    var r = careRequest();
    r.setSize(1);
    assertEquals(3, ((Number) ((Map<?, ?>) care.supportStats(r).data()).get("value")).intValue());
    actor(Role.TEACHER, 11L, List.of(1L));
    assertThrows(
        org.springframework.security.access.AccessDeniedException.class, () -> care.support(r));
  }

  @Test
  void centerCountsAndInactiveAdminAssignmentsAreScoped() {
    var branch = new com.example.academic.branch.Branch();
    branch.setCode("R6B");
    branch.setName("Branch");
    branch.setPhone("PRIVATE_PHONE");
    em.persist(branch);
    em.flush();
    var r = careRequest();
    r.setScopeMode(ReadQuery.Mode.WHOLE_SYSTEM);
    r.setBranchId(null);
    r.setMetric("BRANCHES_WITHOUT_ADMIN");
    actor(Role.SUPER_ADMIN, 11L, List.of());
    var before = page(care.center(r)).totalElements();
    jdbc.update("insert into accounts values(995,'ADMIN',true)");
    jdbc.update("insert into account_branches values(995,995,?,true)", branch.getId());
    assertEquals(before - 1, page(care.center(r)).totalElements());
    jdbc.update("update accounts set status=false where id=995");
    assertEquals(before, page(care.center(r)).totalElements());
    assertFalse(care.center(r).data().toString().contains("PRIVATE"));
  }

  @Test
  void classSearchSeparatesSeatsFromEffectiveRosterAndFiltersDatesBeforePaging() {
    own.setCurrentSize(18);
    own.setStartDate(today.plusDays(3));
    em.flush();
    var r = request();
    r.setRecruitingOnly(true);
    r.setStatus("UPCOMING");
    r.setFrom(today);
    r.setTo(today.plusDays(7));
    r.setSize(1);
    var row = first(insights.classes(r));
    assertEquals(2, ((Number) row.get("remainingSeats")).intValue());
    assertEquals(0, ((Number) row.get("effectiveStudentCount")).intValue());
    r.setTo(today.plusDays(2));
    assertEquals(0, page(insights.classes(r)).totalElements());
  }

  @Test
  void rosterCountsDistinctStudentsAcrossTwoClassesAndRespectsTeacher() {
    var r = request();
    assertEquals(
        1, ((Number) ((Map<?, ?>) insights.rosterCount(r).data()).get("value")).intValue());
    enroll(duplicate, other);
    em.flush();
    actor(Role.TEACHER, 11L, List.of(1L));
    assertEquals(
        1, ((Number) ((Map<?, ?>) insights.rosterCount(r).data()).get("value")).intValue());
    r.setClassId(other.getId());
    assertThrows(ApiException.class, () -> insights.rosterCount(r));
  }

  @Test
  void studentProfileAndReservedFilterDoNotBroadenTeacherAccess() {
    enrollment.setStudyStatus(Enrollment.StudyStatus.RESERVED);
    em.flush();
    var r = request();
    r.setRelationship("RESERVED");
    assertEquals(1, page(insights.students(r)).totalElements());
    var hidden = student(1, "Hidden");
    em.flush();
    actor(Role.TEACHER, 11L, List.of(1L));
    r = request();
    r.setStudentId(hidden.getId());
    var target = r;
    assertThrows(ApiException.class, () -> insights.profile(target));
    r = request();
    r.setStudentId(an.getId());
    var reservedTarget = r;
    assertThrows(ApiException.class, () -> insights.profile(reservedTarget));
    actor(Role.TEACHER, 12L, List.of(1L));
    assertTrue(insights.profile(reservedTarget).data().toString().contains(an.getFullName()));
  }

  @Test
  void sessionFiltersUseActualTeacherMakeupAndUpcomingTime() {
    var s = session(own, 12, today.plusDays(1));
    s.setKind(ClassSession.Kind.MAKEUP);
    s.setRoomName("R1");
    em.flush();
    actor(Role.TEACHER, 12L, List.of(1L));
    var r = request();
    r.setFrom(today);
    r.setTo(today.plusDays(7));
    r.setSubstituteOnly(true);
    r.setKind("MAKEUP");
    r.setUpcomingOnly(true);
    assertEquals(1, page(insights.sessions(r)).totalElements());
    actor(Role.TEACHER, 11L, List.of(1L));
    assertEquals(0, page(insights.sessions(r)).totalElements());
  }

  @Test
  void roomAvailabilityUsesHalfOpenTimeAndExcludesMaintenanceAndUnknownTimes() {
    var s = session(own, 11, today.plusDays(1));
    s.setRoomId(room.getId());
    em.flush();
    var r = request();
    r.setFrom(today.plusDays(1));
    r.setStartTime(LocalTime.of(19, 0));
    r.setEndTime(LocalTime.of(21, 0));
    assertEquals(0, page(insights.rooms(r)).totalElements());
    r.setStartTime(LocalTime.of(20, 0));
    r.setEndTime(LocalTime.of(21, 0));
    assertEquals(1, page(insights.rooms(r)).totalElements());
    s.setStartTime(null);
    em.flush();
    assertEquals(0, page(insights.rooms(r)).totalElements());
    s.setStatus(ClassSession.Status.CANCELLED);
    em.flush();
    assertEquals(1, page(insights.rooms(r)).totalElements());
    room.setStatus(Room.Status.MAINTENANCE);
    em.flush();
    assertEquals(0, page(insights.rooms(r)).totalElements());
    actor(Role.TEACHER, 11L, List.of(1L));
    assertThrows(
        org.springframework.security.access.AccessDeniedException.class, () -> insights.rooms(r));
  }

  @Test
  void resourceWindowMustBeComplete() {
    var r = request();
    r.setFrom(today);
    assertThrows(ApiException.class, () -> insights.rooms(r));
  }

  @Test
  void branchStatisticsCountPeopleOnceAndDoNotLeakOtherBranches() {
    var r = request();
    r.setMetric("ACTIVE_STUDENTS");
    assertEquals(1, ((Number) first(insights.grouped(r)).get("value")).intValue());
    r.setMetric("CLASSES_ONGOING");
    assertEquals(2, ((Number) first(insights.grouped(r)).get("value")).intValue());
    actor(Role.STAFF, 11L, List.of(1L));
    assertThrows(
        org.springframework.security.access.AccessDeniedException.class, () -> insights.grouped(r));
  }

  void absent(ClassSession se, Enrollment en) {
    var a = new Attendance();
    a.setEnrollment(en);
    a.setClassSession(se);
    a.setSessionDate(se.getSessionDate());
    a.setStatus(Attendance.Status.ABSENT);
    a.setRecordedBy(100L);
    a.setRecordedByName("Operator");
    em.persist(a);
  }

  @Test
  void absenceAggregateExcludesCancelledFutureAndUnrecordedSessions() {
    absent(session(own, 11, today.minusDays(1)), enrollment);
    absent(session(own, 11, today.minusDays(2)), enrollment);
    absent(session(own, 11, today.minusDays(3)), enrollment);
    var cancelled = session(own, 11, today.minusDays(4));
    cancelled.setStatus(ClassSession.Status.CANCELLED);
    absent(cancelled, enrollment);
    absent(session(own, 11, today.plusDays(1)), enrollment);
    session(own, 11, today);
    em.flush();
    var r = request();
    r.setFrom(today.minusDays(29));
    r.setTo(today.plusDays(1));
    r.setMinAbsences(3);
    assertEquals(3, ((Number) first(insights.absences(r)).get("absentCount")).intValue());
    r.setMinAbsences(4);
    assertEquals(0, page(insights.absences(r)).totalElements());
    actor(Role.TEACHER, 12L, List.of(1L));
    r.setMinAbsences(1);
    assertEquals(0, page(insights.absences(r)).totalElements());
  }

  @Test
  void missingAttendanceUsesEndedSessionsAndEffectiveRoster() {
    session(own, 11, today.minusDays(1));
    session(own, 11, today.plusDays(1));
    em.flush();
    var r = request();
    r.setFrom(today.minusDays(29));
    r.setTo(today.plusDays(1));
    assertEquals(1, page(insights.missingAttendance(r)).totalElements());
    assertEquals(1, ((Number) first(insights.missingAttendance(r)).get("notRecorded")).intValue());
    enrollment.setStudyStatus(Enrollment.StudyStatus.RESERVED);
    em.flush();
    assertEquals(0, page(insights.missingAttendance(r)).totalElements());
  }

  @Test
  void absenceOutsideEnrollmentPeriodIsExcludedAndThresholdMetadataIsReturned() {
    var period = new EnrollmentPeriod();
    period.setEnrollment(enrollment);
    period.setActiveFrom(today.minusDays(2));
    period.setActiveUntil(today.minusDays(1));
    em.persist(period);
    absent(session(own, 11, today.minusDays(2)), enrollment);
    absent(session(own, 11, today.minusDays(1)), enrollment);
    em.flush();
    var r = request();
    r.setFrom(today.minusDays(5));
    r.setTo(today);
    r.setMinAbsences(1);
    assertEquals(1, ((Number) first(insights.absences(r)).get("absentCount")).intValue());
    assertTrue(insights.absences(r).warnings().getFirst().contains("ABSENT"));
  }

  @Test
  void attendanceFiltersStudentAndMissingWithoutDefaultingToPresent() {
    var se = session(own, 11, today.minusDays(1));
    em.flush();
    var result =
        service.attendance(ReadQuery.Mode.ACTIVE_BRANCH, 1L, se.getId(), an.getId(), true, 0, 5);
    var rows = (PageResponse<?>) ((Map<?, ?>) result.data()).get("records");
    assertEquals(1, rows.totalElements());
    assertTrue(rows.content().toString().contains("NOT_RECORDED"));
    result =
        service.attendance(
            ReadQuery.Mode.ACTIVE_BRANCH, 1L, se.getId(), duplicate.getId(), true, 0, 5);
    assertEquals(0, ((PageResponse<?>) ((Map<?, ?>) result.data()).get("records")).totalElements());
  }

  @Test
  void admissionSearchUsesLatestVerifiedInputLevelNotOutputLevel() {
    for (int version = 1; version <= 2; version++) {
      var p = new com.example.academic.admission.CourseAdmissionPolicy();
      p.setCourseId(course.getId());
      p.setRevision(version);
      p.setMode(com.example.academic.admission.CourseAdmissionPolicy.Mode.REQUIRES_ASSESSMENT);
      p.setProgramId(1L);
      p.setProgramCode("CEFR");
      p.setProgramName("CEFR");
      p.setOutputLevelId(3L);
      p.setOutputLevelCode("B1");
      p.setOutputLevelName("B1");
      p.setConfirmedBy(100L);
      p.setConfirmedByName("Admin");
      p.setConfirmedAt(LocalDateTime.now());
      p.setReason("Verified");
      p.getAcceptedLevels()
          .add(
              new com.example.academic.admission.CourseAdmissionPolicy.LevelSnapshot(
                  (long) version, version == 1 ? "A1" : "A2", "Input"));
      em.persist(p);
    }
    em.flush();
    var r = request();
    r.setEntryLevel("A1");
    assertEquals(0, page(insights.admissions(r)).totalElements());
    r.setEntryLevel("A2");
    assertEquals(1, page(insights.admissions(r)).totalElements());
    r.setEntryLevel("B1");
    assertEquals(0, page(insights.admissions(r)).totalElements());
  }

  @Test
  void teacherAvailabilityChecksOtherBranchConflictsWithoutReturningOtherBranchDetails() {

    jdbc.update("insert into accounts values(991,'TEACHER',true),(992,'TEACHER',true)");
    jdbc.update("insert into teachers values(11,991,'Busy','ACTIVE'),(12,992,'Free','ACTIVE')");
    jdbc.update("insert into account_branches values(991,991,1,true),(992,992,1,true)");
    var c = school(2, 11);
    session(c, 11, today.plusDays(1));
    em.flush();
    var r = request();
    r.setFrom(today.plusDays(1));
    r.setStartTime(LocalTime.of(18, 30));
    r.setEndTime(LocalTime.of(19, 30));
    assertEquals(1, page(insights.availableTeachers(r)).totalElements());
    assertEquals("Free", first(insights.availableTeachers(r)).get("fullName"));
    assertFalse(insights.availableTeachers(r).warnings().isEmpty());
  }

  @Test
  void teacherAssignmentsRespectBranchAndTeacherOwnership() {
    assertEquals(
        1,
        page(service.teacherClasses(ReadQuery.Mode.ACTIVE_BRANCH, 1L, 11L, true, 0, 5))
            .totalElements());
    school(2, 11);
    em.flush();
    assertEquals(
        2,
        page(service.teacherClasses(ReadQuery.Mode.ALL_ASSIGNED, null, 11L, true, 0, 1))
            .totalElements());
    actor(Role.TEACHER, 11L, List.of(1L));
    assertEquals(
        0,
        page(service.teacherClasses(ReadQuery.Mode.ACTIVE_BRANCH, 1L, 12L, true, 0, 5))
            .totalElements());
  }

  @Test
  void currentTeachingExcludesFutureAndFinishedClasses() {
    own.setStartDate(today.plusDays(1));
    em.flush();
    assertEquals(
        0,
        page(service.teacherClasses(ReadQuery.Mode.ACTIVE_BRANCH, 1L, 11L, true, 0, 5))
            .totalElements());
    assertEquals(
        1,
        page(service.teacherClasses(ReadQuery.Mode.ACTIVE_BRANCH, 1L, 11L, false, 0, 5))
            .totalElements());
    own.setStartDate(today.minusDays(1));
    own.setStatus(SchoolClass.Status.FINISHED);
    em.flush();
    assertEquals(
        0,
        page(service.teacherClasses(ReadQuery.Mode.ACTIVE_BRANCH, 1L, 11L, true, 0, 5))
            .totalElements());
  }

  @SuppressWarnings("unchecked")
  Map<String, Object> first(ReadQuery.Result r) {
    return (Map<String, Object>) page(r).content().getFirst();
  }

  @Test
  void pricesFilterBeforePaginationAndKeepLegacyDefaults() {
    course(null, "4000000");
    course(1L, "5000000");
    course(2L, "4500000");
    course(3L, "1000000");
    var closed = course(1L, "100");
    closed.setStatus(Course.Status.CLOSED);
    em.flush();
    var p =
        service.courses(
            ReadQuery.Mode.ACTIVE_BRANCH,
            null,
            null,
            null,
            null,
            null,
            null,
            new BigDecimal("5000000"),
            false,
            0,
            1);
    assertEquals(2, page(p).totalElements());
    assertEquals(1, page(p).content().size());
    assertEquals(
        3,
        page(service.courses(
                ReadQuery.Mode.ACTIVE_BRANCH,
                null,
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("5000000"),
                true,
                0,
                20))
            .totalElements());
    assertEquals(
        3,
        page(service.courses(
                ReadQuery.Mode.ALL_ASSIGNED,
                null,
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("5000000"),
                false,
                0,
                20))
            .totalElements());
    assertEquals(4, legacy.list(0, 20, null, null, null, null, 1L).totalElements());
    assertFalse(first(p).containsKey("content"));
    assertTrue(first(p).get("path").toString().startsWith("/courses?"));
  }

  @Test
  void invalidMoneyAndScopeFailClosed() {
    assertThrows(
        ApiException.class,
        () ->
            service.courses(
                ReadQuery.Mode.ACTIVE_BRANCH,
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("6"),
                new BigDecimal("3"),
                true,
                0,
                5));
    assertThrows(
        ApiException.class,
        () ->
            service.courses(
                ReadQuery.Mode.ACTIVE_BRANCH,
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("-1"),
                null,
                true,
                0,
                5));
    assertThrows(
        ApiException.class,
        () -> service.students(ReadQuery.Mode.ALL_ASSIGNED, 1L, null, null, 0, 5));
    assertThrows(
        ApiException.class,
        () -> service.students(ReadQuery.Mode.ACTIVE_BRANCH, 3L, null, null, 0, 5));
    actor(Role.STAFF, 11L, List.of(1L));
    assertThrows(
        ApiException.class,
        () -> service.students(ReadQuery.Mode.ALL_ASSIGNED, null, null, null, 0, 5));
  }

  @Test
  void sameNameIsNotResolvedToFirstAndPiiIsExcluded() {
    student(3, "Nguyen Van An");
    em.flush();
    var r = service.students(ReadQuery.Mode.ACTIVE_BRANCH, null, "An", null, 0, 1);
    assertEquals(2, page(r).totalElements());
    assertFalse(first(r).containsKey("phone"));
    assertFalse(first(r).containsKey("email"));
    assertFalse(first(r).toString().contains("SECRET"));
  }

  @Test
  void escapedKeywordCannotEnumerateWithWildcard() {
    assertEquals(
        "NO_RESULTS",
        service.students(ReadQuery.Mode.ACTIVE_BRANCH, null, "%", null, 0, 5).status());
  }

  @Test
  void activeStudentsAreDistinctAndDoNotUseProfileStatus() {
    assertEquals(
        2L,
        ((Map<?, ?>)
                service
                    .statistics(ReadQuery.Mode.ACTIVE_BRANCH, null, "TOTAL_PROFILES", null)
                    .data())
            .get("value"));
    assertEquals(
        1L,
        ((Map<?, ?>)
                service
                    .statistics(ReadQuery.Mode.ACTIVE_BRANCH, null, "ACTIVE_IN_CLASS", null)
                    .data())
            .get("value"));
    assertEquals(
        2L,
        ((Map<?, ?>)
                service
                    .statistics(ReadQuery.Mode.ACTIVE_BRANCH, null, "PROFILE_STUDYING", null)
                    .data())
            .get("value"));
  }

  @Test
  void exclusivePeriodsAndReservedOrTransferredAreNotActive() {
    var period = new EnrollmentPeriod();
    period.setEnrollment(enrollment);
    period.setActiveFrom(today.minusDays(5));
    period.setActiveUntil(today);
    em.persist(period);
    var second =
        em.createQuery("select e from Enrollment e where e.schoolClass.id=:id", Enrollment.class)
            .setParameter("id", other.getId())
            .getSingleResult();
    second.setStudyStatus(Enrollment.StudyStatus.RESERVED);
    em.flush();
    assertEquals(
        0L,
        ((Map<?, ?>)
                service
                    .statistics(ReadQuery.Mode.ACTIVE_BRANCH, null, "ACTIVE_IN_CLASS", null)
                    .data())
            .get("value"));
    var rows =
        page(service.studentClasses(ReadQuery.Mode.ACTIVE_BRANCH, null, an.getId(), null, 0, 5))
            .content()
            .toString();
    assertTrue(rows.contains("RESERVED"));
    assertTrue(rows.contains("INACTIVE"));
    assertFalse(rows.contains("relationship=ACTIVE"));
  }

  @Test
  void futureClassIsNotAlreadyStudyingAndHistoricalClaimsAreRejected() {
    own.setStartDate(today.plusDays(1));
    em.flush();
    assertTrue(
        page(service.studentClasses(ReadQuery.Mode.ACTIVE_BRANCH, null, an.getId(), null, 0, 5))
            .content()
            .toString()
            .contains("NOT_STARTED"));
    assertThrows(
        ApiException.class,
        () ->
            service.statistics(
                ReadQuery.Mode.ACTIVE_BRANCH, null, "TOTAL_PROFILES", today.minusDays(1)));
    assertThrows(
        ApiException.class,
        () ->
            service.studentClasses(
                ReadQuery.Mode.ACTIVE_BRANCH, null, an.getId(), today.minusDays(1), 0, 5));
  }

  @Test
  void teacherSeesOnlyOwnRosterAndOwnClassRelationship() {
    actor(Role.TEACHER, 11L, List.of(1L));
    assertEquals(
        1,
        page(service.students(ReadQuery.Mode.ACTIVE_BRANCH, null, "An", null, 0, 5))
            .totalElements());
    assertEquals(
        0,
        page(service.students(ReadQuery.Mode.ACTIVE_BRANCH, null, "An", other.getId(), 0, 5))
            .totalElements());
    assertEquals(
        1,
        page(service.studentClasses(ReadQuery.Mode.ACTIVE_BRANCH, null, an.getId(), null, 0, 5))
            .totalElements());
    assertEquals(
        1,
        page(service.classes(ReadQuery.Mode.ACTIVE_BRANCH, null, null, null, null, false, 0, 5))
            .totalElements());
    assertThrows(
        org.springframework.security.access.AccessDeniedException.class,
        () -> service.statistics(ReadQuery.Mode.ACTIVE_BRANCH, null, "TOTAL_PROFILES", null));
  }

  @Test
  void recruitingClassAndStatusAreFilteredOnServer() {
    other.setCurrentSize(20);
    em.flush();
    assertEquals(
        1,
        page(service.classes(ReadQuery.Mode.ACTIVE_BRANCH, null, null, null, null, true, 0, 1))
            .totalElements());
    assertEquals(
        1,
        page(service.classes(
                ReadQuery.Mode.ACTIVE_BRANCH,
                null,
                null,
                null,
                SchoolClass.Status.FULL,
                false,
                0,
                1))
            .totalElements());
    assertEquals(
        2,
        page(service.classes(
                ReadQuery.Mode.ACTIVE_BRANCH,
                null,
                null,
                null,
                SchoolClass.Status.ONGOING,
                false,
                0,
                1))
            .totalElements());
  }

  @Test
  void sessionListUsesActualTeacherAndBoundedDateRange() {
    session(own, 12, today);
    var sub = session(other, 11, today);
    em.flush();
    actor(Role.TEACHER, 11L, List.of(1L));
    var r =
        service.sessions(ReadQuery.Mode.ACTIVE_BRANCH, null, null, null, today, today, null, 0, 5);
    assertEquals(1, page(r).totalElements());
    assertEquals(sub.getId(), ((Number) first(r).get("id")).longValue());
    assertEquals(true, first(r).get("substitute"));
    assertThrows(
        ApiException.class,
        () ->
            service.sessions(
                ReadQuery.Mode.ACTIVE_BRANCH, null, null, 12L, today, today, null, 0, 5));
    assertThrows(
        ApiException.class,
        () ->
            service.sessions(
                ReadQuery.Mode.ACTIVE_BRANCH,
                null,
                null,
                null,
                today,
                today.plusDays(31),
                null,
                0,
                5));
  }

  @Test
  void attendanceDoesNotTurnDefaultPresentIntoRecordedPresence() {
    var s = session(own, 11, today);
    s.setStartTime(
        LocalTime.MIDNIGHT); // The fixture has started regardless of test execution hour.
    em.flush();
    actor(Role.TEACHER, 11L, List.of(1L));
    var data =
        (Map<?, ?>) service.attendance(ReadQuery.Mode.ACTIVE_BRANCH, null, s.getId(), 0, 5).data();
    assertTrue(data.get("records").toString().contains("NOT_RECORDED"));
    assertEquals(1L, data.get("notRecorded"));
    var a = new Attendance();
    a.setEnrollment(enrollment);
    a.setSessionDate(today);
    a.setClassSession(s);
    a.setStatus(Attendance.Status.ABSENT);
    a.setRecordedBy(100L);
    a.setRecordedByName("Teacher");
    em.persist(a);
    em.flush();
    assertTrue(
        service
            .attendance(ReadQuery.Mode.ACTIVE_BRANCH, null, s.getId(), 0, 5)
            .data()
            .toString()
            .contains("ABSENT"));
  }

  @Test
  void futureCancelledAndOtherTeachersSessionsAreNotFabricated() {
    var future = session(own, 11, today.plusDays(1));
    var cancelled = session(own, 11, today);
    cancelled.setStatus(ClassSession.Status.CANCELLED);
    var foreign = session(other, 12, today);
    em.flush();
    actor(Role.TEACHER, 11L, List.of(1L));
    assertEquals(
        "NOT_STARTED",
        ((Map<?, ?>)
                service.attendance(ReadQuery.Mode.ACTIVE_BRANCH, null, future.getId(), 0, 5).data())
            .get("state"));
    assertEquals(
        "CANCELLED",
        ((Map<?, ?>)
                service
                    .attendance(ReadQuery.Mode.ACTIVE_BRANCH, null, cancelled.getId(), 0, 5)
                    .data())
            .get("state"));
    assertThrows(
        ApiException.class,
        () -> service.attendance(ReadQuery.Mode.ACTIVE_BRANCH, null, foreign.getId(), 0, 5));
  }

  @Test
  void priorityLeadsAreRankedBeforeLimitAndStaffOwnershipIsForced() {
    lead("Future", 100, today.plusDays(2).atStartOfDay(), Lead.Status.NEW);
    lead("No schedule", 100, null, Lead.Status.NEW);
    var overdue = lead("Overdue", 100, today.minusDays(1).atStartOfDay(), Lead.Status.CONSULTING);
    lead("Lost", 100, today.minusDays(3).atStartOfDay(), Lead.Status.LOST);
    lead("Other staff", 101, today.minusDays(2).atStartOfDay(), Lead.Status.NEW);
    em.flush();
    actor(Role.STAFF, 11L, List.of(1L));
    var r = service.leads(ReadQuery.Mode.ACTIVE_BRANCH, null, 1);
    assertEquals(3, page(r).totalElements());
    assertEquals(overdue.getId(), ((Number) first(r).get("id")).longValue());
    assertEquals("OVERDUE", first(r).get("priorityReason"));
    assertFalse(first(r).toString().contains("SECRET"));
  }

  @Test
  void superMustChooseExplicitWholeSystemAndEmptyAssignedDoesNotBecomeGlobal() {
    actor(Role.SUPER_ADMIN, 11L, List.of());
    assertThrows(
        ApiException.class,
        () -> service.students(ReadQuery.Mode.ACTIVE_BRANCH, null, null, null, 0, 5));
    assertEquals(
        2,
        page(service.students(ReadQuery.Mode.WHOLE_SYSTEM, null, null, null, 0, 5))
            .totalElements());
    actor(Role.ADMIN, 11L, List.of());
    assertEquals(
        0,
        page(service.students(ReadQuery.Mode.ALL_ASSIGNED, null, null, null, 0, 5))
            .totalElements());
    assertEquals(
        0,
        page(service.courses(
                ReadQuery.Mode.ALL_ASSIGNED, null, null, null, null, null, null, null, true, 0, 5))
            .totalElements());
  }

  @Test
  void invalidIdsPagesUnknownHelpAndRolesAreRejected() {
    assertThrows(
        ApiException.class,
        () -> service.students(ReadQuery.Mode.ACTIVE_BRANCH, null, null, -1L, 0, 5));
    assertThrows(
        ApiException.class,
        () ->
            service.students(
                ReadQuery.Mode.ACTIVE_BRANCH, null, null, null, Integer.MAX_VALUE, 20));
    assertThrows(
        ApiException.class, () -> controller.help(ReadQuery.Mode.ACTIVE_BRANCH, null, "READ_ENV"));
    assertTrue(
        controller
            .help(ReadQuery.Mode.ACTIVE_BRANCH, null, "WEEKLY_CALENDAR")
            .data()
            .toString()
            .contains("/schedule"));
    actor(Role.STUDENT, 11L, List.of(1L));
    assertThrows(
        org.springframework.security.access.AccessDeniedException.class,
        () -> service.students(ReadQuery.Mode.ACTIVE_BRANCH, null, null, null, 0, 5));
  }

  @Test
  void httpRoutesRequireAuthenticationBindFiltersAndExposeNoWrites() throws Exception {
    var mvc =
        org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(web)
            .apply(
                org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
                    .springSecurity())
            .build();
    var auth = SecurityContextHolder.getContext().getAuthentication();
    SecurityContextHolder.clearContext();
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/api/v1/academic/assistant-query/students"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                .isUnauthorized());
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/v1/academic/assistant-query/courses")
                .param("maxTuition", "5000000")
                .param("maxInclusive", "false")
                .with(
                    org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.authentication(auth)))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$.data.totalElements")
                .value(1))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$.scope.mode")
                .value("ACTIVE_BRANCH"));
    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
            "/api/v1/academic/assistant-query/courses")
        .param("programCode", "IELTS").param("audienceCode", "SECONDARY")
        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth)))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.totalElements").value(0));
    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
            "/api/v1/academic/assistant-query/courses")
        .param("audienceCode", "THCS")
        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth)))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/api/v1/academic/assistant-query/students")
                .with(
                    org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.authentication(auth)))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                .isMethodNotAllowed());
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/v1/academic/assistant-query/classes")
                .param("classId", own.getId().toString())
                .with(
                    org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.authentication(auth)))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$.data.content[0].remainingSeats")
                .value(20))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$.data.content[0].effectiveStudentCount")
                .value(1));
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/v1/academic/assistant-query/rooms")
                .param("status", "MAINTENANCE")
                .with(
                    org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.authentication(auth)))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$.data.totalElements")
                .value(0));
    actor(Role.STUDENT, 11L, List.of(1L));
    var student = SecurityContextHolder.getContext().getAuthentication();
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/v1/academic/assistant-query/students")
                .with(
                    org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.authentication(student)))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                .isForbidden());
  }
}
