package com.example.academic;

import static org.junit.jupiter.api.Assertions.*;

import com.example.academic.admission.*;
import com.example.academic.assessment.*;
import com.example.academic.branch.*;
import com.example.academic.common.*;
import com.example.academic.course.*;
import com.example.academic.enrollment.*;
import com.example.academic.placement.*;
import com.example.academic.room.*;
import com.example.academic.schoolclass.*;
import com.example.academic.security.*;
import com.example.academic.student.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:placement-gate-tests;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class PlacementWorkflowTest {
  @org.springframework.test.context.bean.override.mockito.MockitoBean RemoteApi remote;
  @Autowired SchoolClassService classService;
  @Autowired PlacementService gate;
  @Autowired EnrollmentService enroll;
  @Autowired EnrollmentRepository enrollments;
  @Autowired PlacementExceptionRepository exceptions;
  @Autowired EnrollmentAdmissionRepository admissions;
  @Autowired BranchRepository branches;
  @Autowired CourseRepository courses;
  @Autowired RoomRepository rooms;
  @Autowired SchoolClassRepository classes;
  @Autowired StudentRepository students;
  @Autowired ProgramCatalogService catalog;
  @Autowired AdmissionPolicyService policies;
  @Autowired PlacementAssessmentService assessment;
  @Autowired StudentService studentService;
  Long branch;
  Student student;
  SchoolClass school;
  CatalogDto.ProgramDto program;
  CatalogDto.LevelDto entry, out;

  void actor(Role role) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(
                    99L, 7L, branch, role, "actor", branch == null ? List.of() : List.of(branch)),
                null,
                List.of()));
  }

  @BeforeEach
  void setup() {
    org.mockito.Mockito.when(remote.token()).thenReturn("Bearer test");
    actor(Role.SUPER_ADMIN);
    var b = new Branch();
    b.setCode(UUID.randomUUID().toString());
    b.setName("Branch");
    b.setPhone("0900000000");
    branch = branches.saveAndFlush(b).getId();
    actor(Role.SUPER_ADMIN);
    program =
        catalog.save(
            null, new CatalogDto.ProgramRequest("P" + UUID.randomUUID(), "IELTS", true, null));
    entry =
        catalog.saveLevel(
            program.id(), null, new CatalogDto.LevelRequest("ENTRY", "Entry", 0, true, null));
    out =
        catalog.saveLevel(
            program.id(), null, new CatalogDto.LevelRequest("OUT", "Target", 1, true, null));
    var co = new Course();
    co.setBranchId(branch);
    co.setCode(UUID.randomUUID().toString());
    co.setName("Course");
    co.setDuration(10);
    co.setTuitionFee(BigDecimal.TEN);
    co.setMaxStudents(10);
    courses.saveAndFlush(co);
    var room = new Room();
    room.setCode(UUID.randomUUID().toString());
    room.setName("Room");
    room.setBranchId(branch);
    room.setCapacity(10);
    rooms.saveAndFlush(room);
    school = new SchoolClass();
    school.setCode(UUID.randomUUID().toString());
    school.setName("Class");
    school.setBranchId(branch);
    school.setCourse(co);
    school.setRoom(room);
    school.setTeacherId(7L);
    school.setTeacherName("Teacher");
    school.setMaxSize(10);
    school.setStartDate(LocalDate.now(BusinessTime.ZONE));
    school.setEndDate(LocalDate.now(BusinessTime.ZONE).plusDays(20));
    classes.saveAndFlush(school);
    student = new Student();
    student.setFullName("Student");
    student.setPhone("0900000000");
    student.setBranchId(branch);
    student.setCurrentLevel("Legacy target text");
    students.saveAndFlush(student);
    actor(Role.STAFF);
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  PlacementDto.Context context() {
    return new PlacementDto.Context(
        student.getId(), school.getId(), PlacementDto.Intent.ENROLL, null);
  }

  void policy(boolean open) {
    actor(Role.ADMIN);
    var old = policies.getCourse(school.getCourse().getId()).policy();
    var p =
        policies.confirm(
            school.getCourse().getId(),
            new AdmissionDto.Confirm(
                old.revision(),
                open
                    ? CourseAdmissionPolicy.Mode.OPEN_ENTRY
                    : CourseAdmissionPolicy.Mode.REQUIRES_ASSESSMENT,
                program.id(),
                out.id(),
                open ? List.of() : List.of(entry.id()),
                "Verified course conditions"));
    policies.apply(
        school.getId(),
        new AdmissionDto.Apply(
            school.getAdmissionRevision(), p.policy().revision(), "Apply condition deliberately"));
    actor(Role.STAFF);
  }

  AssessmentDto confirmed(Long level) {
    var d =
        assessment.save(
            student.getId(),
            null,
            new AssessmentDto.Draft(
                null,
                program.id(),
                level,
                PlacementAssessment.Method.INTERVIEW,
                LocalDate.now(BusinessTime.ZONE),
                "Assessor",
                "Source record",
                null,
                null,
                null));
    var current = assessment.current(student.getId());
    return assessment.confirm(
        student.getId(),
        d.id(),
        new AssessmentDto.Action(
            d.version(), "Source confirmed", current.isEmpty() ? null : current.getFirst().id()));
  }

  PlacementDto.ExceptionView request(PlacementDto.Context r) {
    var p = gate.preview(r);
    return gate.request(
        new PlacementDto.ExceptionRequest(r, p.fingerprint(), "Manager should review this case"));
  }

  PlacementDto.ExceptionView approve(PlacementDto.ExceptionView r) {
    actor(Role.ADMIN);
    var a =
        gate.decide(
            r.id(), new PlacementDto.Decision(r.version(), true, "Reviewed supporting evidence"));
    actor(Role.STAFF);
    return a;
  }

  EnrollmentDto create(Long approval) {
    return enroll.enroll(new EnrollmentDto.Request(student.getId(), school.getId(), approval));
  }

  EnrollmentDto reserve(EnrollmentDto e) {
    return enroll.status(
        e.id(),
        new EnrollmentDto.StatusRequest(
            Enrollment.StudyStatus.RESERVED,
            "Leave for personal reason",
            e.version(),
            LocalDate.now(BusinessTime.ZONE).plusDays(5)));
  }

  @Test
  void unverifiedNeverUsesLegacyText() {
    var p = gate.preview(context());
    assertEquals("UNVERIFIED_POLICY", p.academicStatus());
    assertTrue(p.canRequestException());
    assertThrows(ApiException.class, () -> create(null));
    assertTrue(enrollments.findByStudentId(student.getId()).isEmpty());
  }

  @Test
  void openEntryAdmitsWithoutInventedAssessment() {
    policy(true);
    assertTrue(gate.preview(context()).eligible());
    var e = create(null);
    assertEquals("ELIGIBLE", gate.history(e.id()).getFirst().getAcademicStatus());
    assertNull(gate.history(e.id()).getFirst().getAssessmentId());
  }

  @Test
  void matchingAssessmentAdmitsAndSnapshots() {
    policy(false);
    var a = confirmed(entry.id());
    assertTrue(gate.preview(context()).eligible());
    var e = create(null);
    assertEquals(a.id(), gate.history(e.id()).getFirst().getAssessmentId());
  }

  @Test
  void missingAssessmentIsStructured() {
    policy(false);
    assertEquals("MISSING_ASSESSMENT", gate.preview(context()).academicStatus());
    assertThrows(ApiException.class, () -> create(null));
  }

  @Test
  void targetLevelDoesNotSatisfyEntry() {
    policy(false);
    confirmed(out.id());
    assertEquals("LEVEL_MISMATCH", gate.preview(context()).academicStatus());
    assertThrows(ApiException.class, () -> create(null));
  }

  @Test
  void stalePreviewIsRecheckedAtWrite() {
    policy(false);
    var a = confirmed(entry.id());
    assertTrue(gate.preview(context()).eligible());
    assessment.revoke(
        student.getId(), a.id(), new AssessmentDto.Action(a.version(), "Source withdrawn", null));
    assertThrows(ApiException.class, () -> create(null));
  }

  @Test
  void directCallByTeacherIsForbidden() {
    actor(Role.TEACHER);
    assertThrows(ApiException.class, () -> gate.preview(context()));
    assertThrows(ApiException.class, () -> create(null));
  }

  @Test
  void foreignScopeCannotPreview() {
    var other = branch;
    branch = branch + 100;
    actor(Role.STAFF);
    assertThrows(ApiException.class, () -> gate.preview(context()));
    branch = other;
  }

  @Test
  void staffCannotApproveOwnRequest() {
    var r = request(context());
    assertThrows(
        ApiException.class,
        () -> gate.decide(r.id(), new PlacementDto.Decision(r.version(), true, "Not authorized")));
  }

  @Test
  void approvedExceptionConsumesAtomicallyAndRetryReturnsSame() {
    var a = approve(request(context()));
    var e = create(a.id());
    assertEquals(e.id(), create(a.id()).id());
    assertEquals("CONSUMED", gate.get(a.id()).status());
    assertEquals(1, enrollments.findByStudentId(student.getId()).size());
    assertEquals(a.id(), gate.history(e.id()).getFirst().getApprovalId());
  }

  @Test
  void requestedOrRejectedCannotBeUsed() {
    var r = request(context());
    assertThrows(ApiException.class, () -> create(r.id()));
    actor(Role.ADMIN);
    gate.decide(r.id(), new PlacementDto.Decision(r.version(), false, "Insufficient evidence"));
    actor(Role.STAFF);
    assertThrows(ApiException.class, () -> create(r.id()));
  }

  @Test
  void staleDecisionVersionIsRejected() {
    var r = request(context());
    approve(r);
    actor(Role.ADMIN);
    assertThrows(
        ApiException.class,
        () -> gate.decide(r.id(), new PlacementDto.Decision(r.version(), false, "Stale decision")));
  }

  @Test
  void assessmentChangeInvalidatesApproval() {
    policy(false);
    var r = approve(request(context()));
    confirmed(out.id());
    assertEquals("INVALIDATED", gate.get(r.id()).status());
    assertThrows(ApiException.class, () -> create(r.id()));
  }

  @Test
  void policyChangeInvalidatesPendingAndApproved() {
    var r = approve(request(context()));
    policy(true);
    assertEquals("INVALIDATED", gate.get(r.id()).status());
    assertThrows(ApiException.class, () -> create(r.id()));
  }

  @Test
  void changedContextCannotBeRequestedFromOldPreview() {
    var p = gate.preview(context());
    policy(true);
    assertThrows(
        ApiException.class,
        () ->
            gate.request(
                new PlacementDto.ExceptionRequest(context(), p.fingerprint(), "Old dialog")));
  }

  @Test
  void fullClassCannotBeWaived() {
    var r = approve(request(context()));
    school.setMaxSize(0);
    classes.flush();
    assertTrue(
        gate.preview(context()).blockers().stream().anyMatch(x -> x.code().equals("CLASS_FULL")));
    assertThrows(ApiException.class, () -> create(r.id()));
    assertEquals("APPROVED", gate.get(r.id()).status());
  }

  @Test
  void closedCourseCannotBeWaived() {
    var r = approve(request(context()));
    school.getCourse().setStatus(Course.Status.CLOSED);
    courses.flush();
    assertThrows(ApiException.class, () -> create(r.id()));
  }

  @Test
  void approvalCannotBeReboundToOtherStudent() {
    var r = approve(request(context()));
    var s = new Student();
    s.setFullName("Other");
    s.setPhone("0901111111");
    s.setBranchId(branch);
    students.saveAndFlush(s);
    assertThrows(
        ApiException.class,
        () -> enroll.enroll(new EnrollmentDto.Request(s.getId(), school.getId(), r.id())));
  }

  @Test
  void oneApprovalDoesNotCreateDuplicateWhenUsedAgain() {
    var a = approve(request(context()));
    create(a.id());
    assertThrows(ApiException.class, () -> create(null));
    assertEquals(1, gate.history(create(a.id()).id()).size());
  }

  @Test
  void requestHistoryProtectsStudentAndDraftClass() {
    request(context());
    assertThrows(ApiException.class, () -> studentService.delete(student.getId()));
    assertThrows(ApiException.class, () -> classService.delete(school.getId()));
  }

  void newRequestBad() {
    var p = gate.preview(context());
    gate.request(new PlacementDto.ExceptionRequest(context(), "wrongfingerprint", "Wrong dialog"));
  }

  @Test
  void resumeKeepsAcceptedBasisAfterAssessmentChanges() {
    policy(false);
    var a = confirmed(entry.id());
    var e = reserve(create(null));
    confirmed(out.id());
    var r =
        new PlacementDto.Context(
            student.getId(), school.getId(), PlacementDto.Intent.RESUME, e.id());
    assertEquals("REUSED_ACCEPTANCE", gate.preview(r).academicStatus());
    var resumed =
        enroll.status(
            e.id(),
            new EnrollmentDto.StatusRequest(
                Enrollment.StudyStatus.STUDYING, "Return to class", e.version(), null));
    assertEquals(a.id(), gate.history(resumed.id()).getFirst().getAssessmentId());
    assertEquals(2, gate.history(e.id()).size());
  }

  @Test
  void policyChangeForcesResumeRecheck() {
    policy(true);
    var e = reserve(create(null));
    policy(false);
    assertThrows(
        ApiException.class,
        () ->
            enroll.status(
                e.id(),
                new EnrollmentDto.StatusRequest(
                    Enrollment.StudyStatus.STUDYING, "Return to class", e.version(), null)));
    var ctx =
        new PlacementDto.Context(
            student.getId(), school.getId(), PlacementDto.Intent.RESUME, e.id());
    var a = approve(request(ctx));
    var r =
        new EnrollmentDto.StatusRequest(
            Enrollment.StudyStatus.STUDYING, "Return approved", e.version(), null, a.id());
    assertEquals(e.id(), enroll.status(e.id(), r).id());
    assertEquals(e.id(), enroll.status(e.id(), r).id());
  }

  @Test
  void legacyEnrollmentDoesNotGetInventedEvidence() {
    var e = new Enrollment();
    e.setStudent(student);
    e.setSchoolClass(school);
    e.setTuitionFee(BigDecimal.TEN);
    e.setStudyStatus(Enrollment.StudyStatus.RESERVED);
    enrollments.saveAndFlush(e);
    assertTrue(gate.history(e.getId()).isEmpty());
    assertEquals(
        "UNVERIFIED_POLICY",
        gate.preview(
                new PlacementDto.Context(
                    student.getId(), school.getId(), PlacementDto.Intent.RESUME, e.getId()))
            .academicStatus());
  }

  @Test
  void suggestionsKeepBlockedClassesVisible() {
    school.setMaxSize(0);
    classes.flush();
    var list = gate.suggestions(student.getId(), 0, 20);
    assertTrue(
        list.content().stream()
            .anyMatch(
                p -> p.context().classId().equals(school.getId()) && !p.blockers().isEmpty()));
  }

  @Autowired com.example.academic.session.ClassSessionRepository sessions;

  @Test
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
  void conflictPreviewReturnsNormallyOutsideTestTransaction() {
    var other = new SchoolClass();
    other.setCode(UUID.randomUUID().toString());
    other.setName("Other");
    other.setBranchId(branch);
    other.setCourse(school.getCourse());
    other.setRoom(school.getRoom());
    other.setTeacherId(7L);
    other.setTeacherName("Teacher");
    other.setMaxSize(10);
    other.setStartDate(school.getStartDate());
    other.setEndDate(school.getEndDate());
    classes.saveAndFlush(other);
    var e = new Enrollment();
    e.setStudent(student);
    e.setSchoolClass(other);
    e.setTuitionFee(BigDecimal.TEN);
    enrollments.saveAndFlush(e);
    for (var c : List.of(school, other)) {
      var session = new com.example.academic.session.ClassSession();
      session.setSchoolClass(c);
      session.setSessionDate(LocalDate.now(BusinessTime.ZONE).plusDays(1));
      session.setStartTime(LocalTime.of(9, 0));
      session.setEndTime(LocalTime.of(10, 0));
      sessions.saveAndFlush(session);
    }
    var p = gate.preview(context());
    assertTrue(p.blockers().stream().anyMatch(b -> b.code().equals("SCHEDULE_CONFLICT")));
    assertFalse(p.canRequestException());
    assertThrows(ApiException.class, () -> create(null));
  }
}
