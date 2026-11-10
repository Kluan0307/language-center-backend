package com.example.teaching;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.teaching.common.*;
import com.example.teaching.evaluation.*;
import com.example.teaching.exam.*;
import com.example.teaching.grade.*;
import com.example.teaching.integration.*;
import com.example.teaching.security.*;
import java.math.*;
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
        "spring.datasource.url=jdbc:h2:mem:evaluation-policy-tests;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class EvaluationPolicyWorkflowTest {
  @Autowired EvaluationPolicyService policies;
  @Autowired EvaluationService evaluation;
  @Autowired ExamRepository exams;
  @Autowired ExamService examService;
  @Autowired GradeService grades;
  @Autowired ExamCandidateRepository candidates;
  @MockitoBean AcademicDirectory academic;
  Exam exam;
  LocalDate today;

  BigDecimal n(String s) {
    return new BigDecimal(s);
  }

  void actor(Role role) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, 2L, 3L, role, "actor"), null, List.of()));
  }

  @BeforeEach
  void setup() {
    actor(Role.ADMIN);
    today = LocalDate.now(BusinessTime.ZONE);
    when(academic.schoolClass(9L))
        .thenReturn(new AcademicDirectory.SchoolClass(9L, 3L, 2L, 7L, "Class"));
    when(academic.studentsAt(eq(9L), any()))
        .thenReturn(
            List.of(new AcademicDirectory.Student(11L, "Learner", 10L, "ENROLLMENT_PERIOD")));
    var context =
        new AcademicEvaluationContext(
            9L,
            3L,
            today.atStartOfDay(),
            List.of(
                new AcademicEvaluationContext.Learner(
                    10L,
                    11L,
                    "Learner",
                    "RESERVED",
                    List.of(new AcademicEvaluationContext.Period(today.minusDays(10), null)))),
            List.of(),
            List.of(),
            null);
    when(academic.evaluationContext(eq(9L), anyBoolean())).thenReturn(context);
    exam = exam("Exam");
  }

  Exam exam(String name) {
    var e = new Exam();
    e.setName(name);
    e.setExamType("TEST");
    e.setClassId(9L);
    e.setBranchId(3L);
    e.setExamDate(today);
    e.setWeight(BigDecimal.ONE);
    e.setMaxScore(BigDecimal.TEN);
    return exams.saveAndFlush(e);
  }

  EvaluationDto.Confirm request(long version, boolean required, String threshold, Long examId) {
    return new EvaluationDto.Confirm(
        version,
        n(threshold),
        null,
        List.of(
            new EvaluationDto.RuleRequest(
                examId,
                exams.findById(examId).orElseThrow().getVersion(),
                required,
                BigDecimal.ONE)),
        "Reviewed class-specific rules");
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void noPolicyIsUnconfiguredAndDoesNotApplyFifty() {
    assertEquals("UNCONFIGURED", policies.get(9L).status());
    assertNull(evaluation.get(9L).passRate());
    assertNull(evaluation.get(9L).students().getFirst().passed());
  }

  @Test
  void adminConfirmsImmutablePolicyWithExplicitThreshold() {
    var p = policies.confirm(9L, request(0, true, "70", exam.getId()));
    assertEquals(n("70"), p.policy().getPassPercent());
    assertEquals("actor", p.policy().getActorName());
    policies.confirm(9L, request(1, false, "80", exam.getId()));
    assertEquals(2, policies.history(9L).size());
    assertEquals(0,n("70").compareTo(policies.history(9L).getLast().getPassPercent()));
  }

  @Test
  void staffAndTeacherCannotSetRules() {
    for (var role : List.of(Role.STAFF, Role.TEACHER)) {
      actor(role);
      assertThrows(
          ApiException.class, () -> policies.confirm(9L, request(0, true, "70", exam.getId())));
    }
  }

  @Test
  void stalePolicyAndExamRevisionAreRejected() {
    policies.confirm(9L, request(0, true, "70", exam.getId()));
    assertThrows(
        ApiException.class, () -> policies.confirm(9L, request(0, true, "50", exam.getId())));
    assertThrows(
        ApiException.class,
        () ->
            policies.confirm(
                9L,
                new EvaluationDto.Confirm(
                    1L,
                    n("70"),
                    null,
                    List.of(new EvaluationDto.RuleRequest(exam.getId(), 99L, true, BigDecimal.ONE)),
                    "Stale exam editor")));
  }

  @Test
  void wrongClassAndDuplicateExamsRejected() {
    var other = exam("Other");
    other.setClassId(8L);
    exams.flush();
    assertThrows(
        ApiException.class, () -> policies.confirm(9L, request(0, true, "70", other.getId())));
    var item = new EvaluationDto.RuleRequest(exam.getId(), 0L, true, BigDecimal.ONE);
    assertThrows(
        ApiException.class,
        () ->
            policies.confirm(
                9L,
                new EvaluationDto.Confirm(
                    0L, n("70"), null, List.of(item, item), "Duplicate items")));
  }

  @Test
  void teacherCannotSilentlyChangeConfirmedScaleOrDeleteHistory() {
    policies.confirm(9L, request(0, true, "70", exam.getId()));
    actor(Role.TEACHER);
    assertThrows(
        ApiException.class,
        () ->
            examService.save(
                exam.getId(),
                new ExamDto.Request(9L, "Exam", "TEST", today, BigDecimal.ONE, n("20"))));
    assertThrows(ApiException.class, () -> examService.delete(exam.getId()));
  }

  @Test
  void firstGradeFreezesEnrollmentIdentity() {
    actor(Role.TEACHER);
    grades.save(new GradeDto.Request(exam.getId(), 11L, n("7"), null, null));
    assertEquals(
        10L,
        candidates.findByExamIdOrderByStudentNameAsc(exam.getId()).getFirst().getEnrollmentId());
    actor(Role.ADMIN);
    policies.confirm(9L, request(0, true, "70", exam.getId()));
    assertEquals("PASS", evaluation.get(9L).students().getFirst().status());
  }

  @Test
  void changedPolicyChangesOnlyTemporaryEvaluationAndKeepsGradeHistory() {
    actor(Role.TEACHER);
    var grade = grades.save(new GradeDto.Request(exam.getId(), 11L, n("7"), null, null));
    actor(Role.ADMIN);
    policies.confirm(9L, request(0, true, "70", exam.getId()));
    assertEquals("PASS", evaluation.get(9L).students().getFirst().status());
    policies.confirm(9L, request(1, true, "80", exam.getId()));
    assertEquals("FAIL", evaluation.get(9L).students().getFirst().status());
    grades.save(new GradeDto.Request(exam.getId(), 11L, n("9"), null, "Verified correction"));
    assertEquals(1, grades.history(grade.id()).size());
    assertEquals("PASS", evaluation.get(9L).students().getFirst().status());
  }

  @Test
  void courseChangeMakesExistingPolicyUnconfiguredForNewClassContext() {
    policies.confirm(9L, request(0, true, "70", exam.getId()));
    when(academic.schoolClass(9L))
        .thenReturn(new AcademicDirectory.SchoolClass(9L, 3L, 2L, 99L, "Changed"));
    assertEquals("STALE_CLASS_CONTEXT", policies.get(9L).status());
    assertEquals("UNCONFIGURED", evaluation.get(9L).policyStatus());
  }

  @Test
  void dependencyFailureDoesNotProduceZeroOrPass() {
    policies.confirm(9L, request(0, true, "70", exam.getId()));
    when(academic.evaluationContext(eq(9L), anyBoolean()))
        .thenThrow(
            new ApiException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Academic unavailable"));
    assertThrows(ApiException.class, () -> evaluation.get(9L));
  }
}
