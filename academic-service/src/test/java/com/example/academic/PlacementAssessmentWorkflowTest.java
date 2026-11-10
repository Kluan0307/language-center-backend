package com.example.academic;

import static org.junit.jupiter.api.Assertions.*;

import com.example.academic.admission.*;
import com.example.academic.assessment.*;
import com.example.academic.branch.*;
import com.example.academic.common.ApiException;
import com.example.academic.common.BusinessTime;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:placement-assessment-tests;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class PlacementAssessmentWorkflowTest {
  @Autowired ProgramCatalogService catalog;
  @Autowired PlacementAssessmentService service;
  @Autowired PlacementAssessmentRepository repository;
  @Autowired StudentRepository students;
  @Autowired StudentService studentService;
  @Autowired BranchRepository branches;
  Long branch;
  Student student;
  CatalogDto.ProgramDto program;
  CatalogDto.LevelDto level;

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
    student = new Student();
    student.setFullName("Student");
    student.setPhone("0901111111");
    student.setCurrentLevel("Legacy unverified");
    student.setBranchId(branch);
    students.saveAndFlush(student);
    program =
        catalog.save(
            null, new CatalogDto.ProgramRequest("I" + UUID.randomUUID(), "IELTS", true, null));
    level =
        catalog.saveLevel(
            program.id(), null, new CatalogDto.LevelRequest("ENTRY", "Entry", 0, true, null));
    actor(Role.STAFF, branch);
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  AssessmentDto.Draft draft(
      Long version, PlacementAssessment.Method method, BigDecimal score, BigDecimal max) {
    return new AssessmentDto.Draft(
        version,
        program.id(),
        level.id(),
        method,
        LocalDate.now(BusinessTime.ZONE),
        "External assessor",
        "Interview record A",
        score,
        max,
        "Correct draft metadata");
  }

  AssessmentDto create() {
    return service.save(
        student.getId(), null, draft(null, PlacementAssessment.Method.INTERVIEW, null, null));
  }

  AssessmentDto confirm(AssessmentDto a, Long current) {
    return service.confirm(
        student.getId(),
        a.id(),
        new AssessmentDto.Action(a.version(), "Source verified against record", current));
  }

  @Test
  void legacyTextIsNeverAnAssessmentAndDraftIsNotCurrent() {
    assertTrue(service.current(student.getId()).isEmpty());
    var a = create();
    assertEquals(PlacementAssessment.Status.DRAFT, a.status());
    assertTrue(service.current(student.getId()).isEmpty());
    assertEquals(
        "Legacy unverified", students.findById(student.getId()).orElseThrow().getCurrentLevel());
  }

  @Test
  void confirmsInterviewWithoutInventingScore() {
    var a = confirm(create(), null);
    assertNull(a.score());
    assertEquals(PlacementAssessment.Status.CONFIRMED, a.status());
    assertEquals(1, service.current(student.getId()).size());
    assertEquals("External assessor", a.assessorName());
    assertEquals(2, service.history(student.getId(), a.id()).size());
  }

  @Test
  void testNeedsBothScoreAndScale() {
    assertThrows(
        ApiException.class,
        () ->
            service.save(
                student.getId(), null, draft(null, PlacementAssessment.Method.TEST, null, null)));
    assertThrows(
        ApiException.class,
        () ->
            service.save(
                student.getId(),
                null,
                draft(null, PlacementAssessment.Method.INTERVIEW, BigDecimal.ONE, null)));
  }

  @Test
  void zeroIsValidButAboveScaleIsRejected() {
    var a =
        service.save(
            student.getId(),
            null,
            draft(null, PlacementAssessment.Method.TEST, BigDecimal.ZERO, BigDecimal.TEN));
    assertEquals(0, a.score().signum());
    assertThrows(
        ApiException.class,
        () ->
            service.save(
                student.getId(),
                null,
                draft(
                    null,
                    PlacementAssessment.Method.TEST,
                    BigDecimal.valueOf(11),
                    BigDecimal.TEN)));
  }

  @Test
  void futureDateIsRejectedInBusinessZone() {
    var r = draft(null, PlacementAssessment.Method.INTERVIEW, null, null);
    assertThrows(
        ApiException.class,
        () ->
            service.save(
                student.getId(),
                null,
                new AssessmentDto.Draft(
                    null,
                    r.programId(),
                    r.levelId(),
                    r.method(),
                    LocalDate.now(BusinessTime.ZONE).plusDays(1),
                    r.assessorName(),
                    r.source(),
                    null,
                    null,
                    null)));
  }

  @Test
  void staleDraftCannotOverwrite() {
    var a = create();
    var saved =
        service.save(
            student.getId(),
            a.id(),
            draft(a.version(), PlacementAssessment.Method.INTERVIEW, null, null));
    assertTrue(saved.version() > a.version());
    assertThrows(
        ApiException.class,
        () ->
            service.save(
                student.getId(),
                a.id(),
                draft(a.version(), PlacementAssessment.Method.INTERVIEW, null, null)));
  }

  @Test
  void confirmedContentCannotBeEditedInPlace() {
    var a = confirm(create(), null);
    assertThrows(
        ApiException.class,
        () ->
            service.save(
                student.getId(),
                a.id(),
                draft(a.version(), PlacementAssessment.Method.INTERVIEW, null, null)));
  }

  @Test
  void replacementPreservesOriginalAndCreatesExplicitLinks() {
    var first = confirm(create(), null);
    var next = confirm(create(), first.id());
    var old = service.get(student.getId(), first.id());
    assertEquals(PlacementAssessment.Status.SUPERSEDED, old.status());
    assertEquals(next.id(), old.supersededById());
    assertEquals(first.id(), next.supersedesId());
    assertEquals(first.source(), old.source());
    assertEquals(next.id(), service.current(student.getId()).getFirst().id());
  }

  @Test
  void staleCurrentReferenceCannotReplaceUnexpectedResult() {
    var first = confirm(create(), null);
    var next = create();
    assertThrows(ApiException.class, () -> confirm(next, null));
    assertEquals(first.id(), service.current(student.getId()).getFirst().id());
  }

  @Test
  void revocationDoesNotReviveSupersededResult() {
    var first = confirm(create(), null);
    var second = confirm(create(), first.id());
    var revoked =
        service.revoke(
            student.getId(),
            second.id(),
            new AssessmentDto.Action(second.version(), "Source withdrawn", null));
    assertEquals(PlacementAssessment.Status.REVOKED, revoked.status());
    assertTrue(service.current(student.getId()).isEmpty());
    assertEquals(
        PlacementAssessment.Status.SUPERSEDED, service.get(student.getId(), first.id()).status());
  }

  @Test
  void databaseAlsoEnforcesOneCurrentPerStudentProgram() {
    confirm(create(), null);
    var second = create();
    var entity = repository.findById(second.id()).orElseThrow();
    entity.setStatus(PlacementAssessment.Status.CONFIRMED);
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () -> repository.saveAndFlush(entity));
  }

  @Test
  void teacherAndForeignBranchCannotReadAssessment() {
    var a = create();
    actor(Role.TEACHER, branch);
    assertThrows(ApiException.class, () -> service.current(student.getId()));
    actor(Role.STAFF, branch + 100);
    assertThrows(ApiException.class, () -> service.get(student.getId(), a.id()));
  }

  @Test
  void historicalAssessmentBlocksStudentDelete() {
    var a = create();
    service.revoke(
        student.getId(), a.id(), new AssessmentDto.Action(a.version(), "Draft cancelled", null));
    assertThrows(ApiException.class, () -> studentService.delete(student.getId()));
  }

  @Test
  void levelMustBelongToProgram() {
    actor(Role.SUPER_ADMIN, null);
    var other =
        catalog.save(
            null, new CatalogDto.ProgramRequest("P" + UUID.randomUUID(), "Other", true, null));
    var otherLevel =
        catalog.saveLevel(
            other.id(), null, new CatalogDto.LevelRequest("OTHER", "Other", 0, true, null));
    actor(Role.STAFF, branch);
    var r = draft(null, PlacementAssessment.Method.INTERVIEW, null, null);
    assertThrows(
        ApiException.class,
        () ->
            service.save(
                student.getId(),
                null,
                new AssessmentDto.Draft(
                    null,
                    program.id(),
                    otherLevel.id(),
                    r.method(),
                    r.assessedOn(),
                    r.assessorName(),
                    r.source(),
                    null,
                    null,
                    null)));
  }

  @Test
  void disabledProgramPreventsConfirmationWithoutErasingDraft() {
    var a = create();
    actor(Role.SUPER_ADMIN, null);
    catalog.save(
        program.id(),
        new CatalogDto.ProgramRequest(program.code(), program.name(), false, program.version()));
    actor(Role.STAFF, branch);
    assertThrows(ApiException.class, () -> confirm(a, null));
    assertEquals(PlacementAssessment.Status.DRAFT, service.get(student.getId(), a.id()).status());
  }
}
