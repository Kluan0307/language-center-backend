package com.example.academic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.academic.admission.*;
import com.example.academic.branch.*;
import com.example.academic.common.ApiException;
import com.example.academic.course.*;
import com.example.academic.room.*;
import com.example.academic.schoolclass.*;
import com.example.academic.security.*;
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
        "spring.datasource.url=jdbc:h2:mem:admission-policy-tests;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class AdmissionPolicyWorkflowTest {
  @Autowired ProgramCatalogService catalog;
  @Autowired AdmissionPolicyService service;
  @Autowired CourseRepository courses;
  @Autowired SchoolClassService classes;
  @Autowired SchoolClassRepository classRows;
  @Autowired BranchRepository branches;
  @Autowired RoomRepository rooms;
  @MockitoBean TeacherDirectory teachers;
  Long branch;
  CatalogDto.ProgramDto program;
  CatalogDto.LevelDto input, output;

  void actor(Role role, Long scope) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(
                    99L, 7L, scope, role, "actor", scope == null ? List.of() : List.of(scope)),
                null,
                List.of()));
  }

  @BeforeEach
  void setup() {
    actor(Role.SUPER_ADMIN, null);
    var b = new Branch();
    b.setCode("B" + UUID.randomUUID());
    b.setName("Branch");
    b.setPhone("0900000000");
    branch = branches.saveAndFlush(b).getId();
    program =
        catalog.save(
            null, new CatalogDto.ProgramRequest("I" + UUID.randomUUID(), "IELTS", true, null));
    input =
        catalog.saveLevel(
            program.id(), null, new CatalogDto.LevelRequest("ENTRY", "Entry 4.0", 1, true, null));
    output =
        catalog.saveLevel(
            program.id(), null, new CatalogDto.LevelRequest("TARGET", "Target 6.5", 2, true, null));
    when(teachers.require(anyLong(), anyLong()))
        .thenAnswer(
            i ->
                new TeacherDirectory.Teacher(
                    i.getArgument(0), 100L, "Teacher", branch, List.of(branch)));
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  Course course(Long branchId) {
    var c = new Course();
    c.setCode("C" + UUID.randomUUID());
    c.setName("Course");
    c.setType("IELTS text");
    c.setLevel("6.5 legacy");
    c.setEntryRequirement("Legacy input text");
    c.setDuration(24);
    c.setMaxStudents(20);
    c.setTuitionFee(java.math.BigDecimal.valueOf(1000));
    c.setBranchId(branchId);
    return courses.saveAndFlush(c);
  }

  AdmissionDto.Confirm config(long revision, CourseAdmissionPolicy.Mode mode, List<Long> accepted) {
    return new AdmissionDto.Confirm(
        revision, mode, program.id(), output.id(), accepted, "Confirmed against course plan");
  }

  Long school(Course c) {
    var r = new Room();
    r.setCode("R" + UUID.randomUUID());
    r.setName("Room");
    r.setBranchId(branch);
    r.setCapacity(20);
    rooms.saveAndFlush(r);
    return classes
        .save(
            null,
            new SchoolClassDto.Request(
                "CL" + UUID.randomUUID(),
                "Class",
                c.getId(),
                7L,
                "Teacher",
                r.getId(),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(20),
                10,
                branch,
                List.of()))
        .id();
  }

  @Test
  void legacyTextNeverBecomesVerifiedPolicy() {
    var c = course(branch);
    var view = service.getCourse(c.getId());
    assertEquals(CourseAdmissionPolicy.Mode.UNVERIFIED, view.policy().mode());
    assertNull(view.policy().programId());
    assertEquals("6.5 legacy", view.legacyLevel());
    assertEquals(24, c.getDuration());
  }

  @Test
  void catalogWritesAreSuperOnly() {
    actor(Role.ADMIN, branch);
    assertThrows(
        ApiException.class,
        () -> catalog.save(null, new CatalogDto.ProgramRequest("FORBIDDEN", "Denied", true, null)));
  }

  @Test
  void catalogVersionAndImmutableCode() {
    var saved =
        catalog.save(
            program.id(), new CatalogDto.ProgramRequest(program.code(), "New label", true, 0L));
    assertEquals(1, saved.version());
    assertThrows(
        ApiException.class,
        () ->
            catalog.save(
                program.id(), new CatalogDto.ProgramRequest(program.code(), "Stale", true, 0L)));
    assertThrows(
        ApiException.class,
        () ->
            catalog.save(
                program.id(), new CatalogDto.ProgramRequest("OTHER", "Changed code", true, 1L)));
  }

  @Test
  void policySeparatesInputFromTargetAndPersistsHistory() {
    var c = course(branch);
    var p =
        service
            .confirm(
                c.getId(),
                config(0, CourseAdmissionPolicy.Mode.REQUIRES_ASSESSMENT, List.of(input.id())))
            .policy();
    assertEquals(input.id(), p.acceptedLevels().getFirst().levelId());
    assertEquals(output.id(), p.outputLevel().levelId());
    assertNotEquals(p.acceptedLevels().getFirst().levelId(), p.outputLevel().levelId());
    assertEquals(1, p.revision());
    assertEquals(1, service.history(c.getId()).size());
  }

  @Test
  void mixedProgramAndDuplicateLevelsAreRejected() {
    var c = course(branch);
    var other =
        catalog.save(
            null, new CatalogDto.ProgramRequest("P" + UUID.randomUUID(), "Other", true, null));
    var l =
        catalog.saveLevel(
            other.id(), null, new CatalogDto.LevelRequest("E", "Other level", 0, true, null));
    assertThrows(
        ApiException.class,
        () ->
            service.confirm(
                c.getId(),
                config(0, CourseAdmissionPolicy.Mode.REQUIRES_ASSESSMENT, List.of(l.id()))));
    assertThrows(
        ApiException.class,
        () ->
            service.confirm(
                c.getId(),
                config(
                    0,
                    CourseAdmissionPolicy.Mode.REQUIRES_ASSESSMENT,
                    List.of(input.id(), input.id()))));
  }

  @Test
  void openEntryIsExplicitAndDoesNotAcceptInputLevels() {
    var c = course(branch);
    assertThrows(
        ApiException.class,
        () ->
            service.confirm(
                c.getId(), config(0, CourseAdmissionPolicy.Mode.OPEN_ENTRY, List.of(input.id()))));
    assertEquals(
        CourseAdmissionPolicy.Mode.OPEN_ENTRY,
        service
            .confirm(c.getId(), config(0, CourseAdmissionPolicy.Mode.OPEN_ENTRY, List.of()))
            .policy()
            .mode());
  }

  @Test
  void staleCoursePolicyCannotOverwrite() {
    var c = course(branch);
    service.confirm(c.getId(), config(0, CourseAdmissionPolicy.Mode.OPEN_ENTRY, List.of()));
    assertThrows(
        ApiException.class,
        () ->
            service.confirm(
                c.getId(),
                config(0, CourseAdmissionPolicy.Mode.REQUIRES_ASSESSMENT, List.of(input.id()))));
    assertEquals(1, service.history(c.getId()).size());
  }

  @Test
  void inactiveCatalogBlocksNewPolicyWithoutChangingOldSnapshot() {
    var c = course(branch);
    service.confirm(c.getId(), config(0, CourseAdmissionPolicy.Mode.OPEN_ENTRY, List.of()));
    catalog.save(program.id(), new CatalogDto.ProgramRequest(program.code(), "Renamed", false, 0L));
    assertEquals("IELTS", service.getCourse(c.getId()).policy().programName());
    assertThrows(
        ApiException.class,
        () ->
            service.confirm(
                c.getId(), config(1, CourseAdmissionPolicy.Mode.OPEN_ENTRY, List.of())));
  }

  @Test
  void staffCannotConfigureAndAdminCannotChangeSharedCourse() {
    var shared = course(null);
    actor(Role.ADMIN, branch);
    assertFalse(service.getCourse(shared.getId()).canManage());
    assertThrows(
        ApiException.class,
        () ->
            service.confirm(
                shared.getId(), config(0, CourseAdmissionPolicy.Mode.OPEN_ENTRY, List.of())));
    var local = course(branch);
    actor(Role.STAFF, branch);
    assertThrows(
        ApiException.class,
        () ->
            service.confirm(
                local.getId(), config(0, CourseAdmissionPolicy.Mode.OPEN_ENTRY, List.of())));
  }

  @Test
  void classSnapshotDoesNotFollowLaterCourseRevision() {
    var c = course(branch);
    var first =
        service
            .confirm(
                c.getId(),
                config(0, CourseAdmissionPolicy.Mode.REQUIRES_ASSESSMENT, List.of(input.id())))
            .policy();
    Long id = school(c);
    assertEquals(first.id(), service.getClass(id).policy().id());
    service.confirm(c.getId(), config(1, CourseAdmissionPolicy.Mode.OPEN_ENTRY, List.of()));
    assertEquals(first.id(), service.getClass(id).policy().id());
    assertThrows(
        ApiException.class,
        () -> service.apply(id, new AdmissionDto.Apply(0L, 2L, "Stale class revision")));
    assertThrows(
        ApiException.class,
        () -> service.apply(id, new AdmissionDto.Apply(1L, 1L, "Stale course revision")));
    var updated =
        service.apply(id, new AdmissionDto.Apply(1L, 2L, "Explicitly apply new conditions"));
    assertEquals(2, updated.revision());
    assertEquals(2, updated.policy().revision());
    assertEquals(2, service.classHistory(id).size());
  }

  @Test
  void classCreatedWithoutPolicyStaysUnverifiedUntilExplicitApply() {
    var c = course(branch);
    Long id = school(c);
    assertEquals(CourseAdmissionPolicy.Mode.UNVERIFIED, service.getClass(id).policy().mode());
    service.confirm(c.getId(), config(0, CourseAdmissionPolicy.Mode.OPEN_ENTRY, List.of()));
    assertEquals(CourseAdmissionPolicy.Mode.UNVERIFIED, service.getClass(id).policy().mode());
    assertEquals(
        CourseAdmissionPolicy.Mode.OPEN_ENTRY,
        service
            .apply(id, new AdmissionDto.Apply(1L, 1L, "Verified legacy class inputs"))
            .policy()
            .mode());
  }

  @Test
  void levelRenameDoesNotRewritePolicySnapshot() {
    var c = course(branch);
    service.confirm(c.getId(), config(0, CourseAdmissionPolicy.Mode.OPEN_ENTRY, List.of()));
    catalog.saveLevel(
        null,
        output.id(),
        new CatalogDto.LevelRequest(output.code(), "New label", 99, true, output.version()));
    assertEquals("Target 6.5", service.getCourse(c.getId()).policy().outputLevel().name());
  }
}
