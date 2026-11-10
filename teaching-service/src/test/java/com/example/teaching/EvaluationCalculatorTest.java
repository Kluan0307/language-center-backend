package com.example.teaching;

import static org.junit.jupiter.api.Assertions.*;

import com.example.teaching.evaluation.*;
import com.example.teaching.exam.*;
import com.example.teaching.grade.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;

class EvaluationCalculatorTest {
  LocalDate today = LocalDate.of(2026, 11, 8);
  EvaluationPolicy policy;
  AcademicEvaluationContext.Learner learner;
  List<Exam> exams;
  List<Grade> grades;
  List<ExamCandidate> candidates;

  BigDecimal n(String s) {
    return new BigDecimal(s);
  }

  @BeforeEach
  void setup() {
    policy = new EvaluationPolicy();
    policy.setRevision(1);
    policy.setClassId(1L);
    policy.setPassPercent(n("50"));
    learner =
        new AcademicEvaluationContext.Learner(
            10L,
            11L,
            "Learner",
            "RESERVED",
            List.of(new AcademicEvaluationContext.Period(today.minusDays(10), null)));
    exams = new ArrayList<>();
    grades = new ArrayList<>();
    candidates = new ArrayList<>();
  }

  Exam exam(long id, String max, String weight, boolean required, LocalDate date) {
    var e = new Exam();
    e.setId(id);
    e.setClassId(1L);
    e.setName("Exam" + id);
    e.setExamDate(date);
    e.setMaxScore(n(max));
    e.setWeight(n(weight));
    exams.add(e);
    policy
        .getRules()
        .add(new EvaluationPolicy.Rule(id, 0, e.getName(), date, required, n(weight), n(max)));
    return e;
  }

  void grade(Exam e, String score) {
    var g = new Grade();
    g.setId((long) grades.size() + 1);
    g.setExam(e);
    g.setStudentId(11L);
    g.setScore(n(score));
    grades.add(g);
  }

  AcademicEvaluationContext context(
      List<AcademicEvaluationContext.Learner> learners,
      List<AcademicEvaluationContext.Session> sessions,
      List<AcademicEvaluationContext.Mark> marks) {
    return new AcademicEvaluationContext(
        1L, 2L, today.atStartOfDay(), learners, sessions, marks, null);
  }

  EvaluationDto.Report report(
      List<AcademicEvaluationContext.Learner> learners,
      List<AcademicEvaluationContext.Session> sessions,
      List<AcademicEvaluationContext.Mark> marks) {
    return EvaluationCalculator.calculate(
        policy, context(learners, sessions, marks), exams, grades, candidates, today);
  }

  EvaluationDto.Row row() {
    return report(List.of(learner), List.of(), List.of()).students().getFirst();
  }

  @Test
  void unconfiguredNeverAssumesFifty() {
    var e = exam(1, "10", "1", true, today);
    grade(e, "10");
    policy = null;
    var r = row();
    assertEquals("INSUFFICIENT", r.status());
    assertNull(r.passed());
    assertNull(r.averagePercent());
  }

  @Test
  void zeroScoreIsARecordedGrade() {
    var e = exam(1, "10", "1", true, today);
    grade(e, "0");
    assertEquals(1, row().gradedExams());
    assertEquals(0, row().averagePercent().signum());
    assertEquals("FAIL", row().status());
  }

  @Test
  void missingRequiredIsInsufficientNotFail() {
    var e = exam(1, "10", "1", false, today);
    grade(e, "10");
    exam(2, "100", "1", true, today);
    assertEquals("INSUFFICIENT", row().status());
    assertEquals(1, row().missingRequiredExams());
    assertEquals(n("100.00"), row().averagePercent());
  }

  @Test
  void optionalMissingIsNotInventedZero() {
    var e = exam(1, "10", "1", true, today);
    grade(e, "8");
    exam(2, "100", "100", false, today);
    assertEquals(n("80.00"), row().averagePercent());
    assertEquals("PASS", row().status());
  }

  @Test
  void differentScalesAndWeightsNormalizeBeforeAveraging() {
    var a = exam(1, "20", "1", true, today);
    var b = exam(2, "100", "3", true, today);
    grade(a, "10");
    grade(b, "100");
    assertEquals(n("87.50"), row().averagePercent());
  }

  @Test
  void comparesUnroundedAverageBeforeDisplayRounding() {
    var e = exam(1, "200", "1", true, today);
    grade(e, "99.99");
    assertEquals(n("50.00"), row().averagePercent());
    assertTrue(row().unroundedAveragePercent().compareTo(n("50")) < 0);
    assertEquals("FAIL", row().status());
  }

  @Test
  void noApplicableGradeIsUnknownNotZero() {
    exam(1, "10", "1", false, today);
    assertNull(row().averagePercent());
    assertEquals("INSUFFICIENT", row().status());
  }

  @Test
  void requiredBeforeEnrollmentDoesNotApply() {
    exam(1, "10", "1", true, today.minusDays(20));
    var e = exam(2, "10", "1", true, today);
    grade(e, "10");
    assertEquals("PASS", row().status());
    assertEquals("NOT_APPLICABLE", row().exams().getFirst().applicability());
  }

  @Test
  void futureRequiredExamRemainsMissing() {
    var e = exam(1, "10", "1", false, today);
    grade(e, "10");
    exam(2, "10", "1", true, today.plusDays(2));
    assertEquals("INSUFFICIENT", row().status());
    assertTrue(
        row().reasons().stream().anyMatch(r -> r.code().equals("REQUIRED_EXAM_NOT_YET_DUE")));
  }

  @Test
  void missingPeriodsNeverUseCurrentStudentStatus() {
    learner = new AcademicEvaluationContext.Learner(10L, 11L, "Learner", "STUDYING", List.of());
    var e = exam(1, "10", "1", true, today);
    grade(e, "10");
    assertEquals("INSUFFICIENT", row().status());
    assertNull(row().passed());
  }

  @Test
  void frozenEnrollmentIdentitySurvivesLaterSameDayTransfer() {
    learner =
        new AcademicEvaluationContext.Learner(
            10L,
            11L,
            "Learner",
            "TRANSFERRED",
            List.of(new AcademicEvaluationContext.Period(today.minusDays(10), today)));
    var e = exam(1, "10", "1", true, today);
    e.setRosterFrozenAt(today.atStartOfDay());
    var c = new ExamCandidate();
    c.setExamId(1L);
    c.setStudentId(11L);
    c.setEnrollmentId(10L);
    candidates.add(c);
    grade(e, "10");
    assertEquals("PASS", row().status());
  }

  @Test
  void sameStudentMultipleEnrollmentsDoNotDuplicateScore() {
    var e = exam(1, "10", "1", true, today.minusDays(5));
    grade(e, "10");
    learner =
        new AcademicEvaluationContext.Learner(
            10L,
            11L,
            "Learner",
            "TRANSFERRED",
            List.of(new AcademicEvaluationContext.Period(today.minusDays(10), today.minusDays(2))));
    var next =
        new AcademicEvaluationContext.Learner(
            20L,
            11L,
            "Learner",
            "STUDYING",
            List.of(new AcademicEvaluationContext.Period(today.minusDays(2), null)));
    var rows = report(List.of(learner, next), List.of(), List.of()).students();
    assertEquals("PASS", rows.getFirst().status());
    assertEquals("INSUFFICIENT", rows.getLast().status());
    assertEquals(0, rows.getLast().gradedExams());
  }

  @Test
  void ambiguousPeriodMappingDoesNotAssignGradeTwice() {
    var e = exam(1, "10", "1", true, today);
    grade(e, "10");
    var other =
        new AcademicEvaluationContext.Learner(20L, 11L, "Learner", "STUDYING", learner.periods());
    assertTrue(
        report(List.of(learner, other), List.of(), List.of()).students().stream()
            .allMatch(r -> r.status().equals("INSUFFICIENT")));
  }

  @Test
  void invalidScoreIsNotSilentlyIncluded() {
    var e = exam(1, "10", "1", true, today);
    grade(e, "11");
    assertEquals("INSUFFICIENT", row().status());
    assertNull(row().averagePercent());
  }

  @Test
  void attendanceNotRequiredDoesNotAssumeOneHundred() {
    var e = exam(1, "10", "1", true, today);
    grade(e, "10");
    assertEquals("NOT_REQUIRED", row().attendance().status());
    assertNull(row().attendance().percent());
  }

  AcademicEvaluationContext.Session session() {
    return new AcademicEvaluationContext.Session(
        7L, today, LocalTime.of(9, 0), LocalTime.of(10, 0), "COMPLETED");
  }

  @Test
  void attendanceWithoutCompletedSessionsIsUnknown() {
    policy.setMinAttendancePercent(n("80"));
    var e = exam(1, "10", "1", true, today);
    grade(e, "10");
    assertEquals("UNKNOWN", row().attendance().status());
    assertEquals("INSUFFICIENT", row().status());
  }

  @Test
  void excusedAbsenceRemainsInDenominator() {
    policy.setMinAttendancePercent(n("80"));
    var e = exam(1, "10", "1", true, today);
    grade(e, "10");
    var r =
        report(
                List.of(learner),
                List.of(session()),
                List.of(new AcademicEvaluationContext.Mark(10L, 7L, today, "ABSENT", true)))
            .students()
            .getFirst();
    assertEquals("FAIL", r.status());
    assertEquals(1, r.attendance().expected());
    assertEquals(1, r.attendance().excusedAbsences());
    assertEquals(0, r.attendance().percent().signum());
  }

  @Test
  void lateCountsAsAttendance() {
    policy.setMinAttendancePercent(n("80"));
    var e = exam(1, "10", "1", true, today);
    grade(e, "10");
    assertEquals(
        "PASS",
        report(
                List.of(learner),
                List.of(session()),
                List.of(new AcademicEvaluationContext.Mark(10L, 7L, today, "LATE", false)))
            .students()
            .getFirst()
            .status());
  }

  @Test
  void missingAttendanceRecordIsUnknown() {
    policy.setMinAttendancePercent(n("80"));
    var e = exam(1, "10", "1", true, today);
    grade(e, "10");
    assertEquals(
        "INSUFFICIENT",
        report(List.of(learner), List.of(session()), List.of()).students().getFirst().status());
  }

  @Test
  void zeroAttendanceThresholdStillRequiresKnownEvidence() {
    policy.setMinAttendancePercent(BigDecimal.ZERO);
    var e = exam(1, "10", "1", true, today);
    grade(e, "10");
    assertEquals(
        "PASS",
        report(
                List.of(learner),
                List.of(session()),
                List.of(new AcademicEvaluationContext.Mark(10L, 7L, today, "ABSENT", false)))
            .students()
            .getFirst()
            .status());
  }

  @Test
  void passRateExcludesInsufficientAndIsNullWithoutAssessable() {
    exam(1, "10", "1", true, today);
    assertNull(report(List.of(learner), List.of(), List.of()).passRate());
    var e = exams.getFirst();
    grade(e, "10");
    var unknown =
        new AcademicEvaluationContext.Learner(20L, 22L, "Unverified", "STUDYING", List.of());
    var report = report(List.of(learner, unknown), List.of(), List.of());
    assertEquals(n("100.00"), report.passRate());
    assertEquals(1, report.insufficientCount());
    assertEquals(1, report.assessableCount());
  }

  @Test
  void savedAttendanceOutsideCurrentPeriodIsUnknownRatherThanSilentlyDropped() {
    policy.setMinAttendancePercent(n("80"));
    var exam = exam(1, "10", "1", true, today);
    grade(exam, "10");
    var outside =
        new AcademicEvaluationContext.Session(
            8L, today.minusDays(20), LocalTime.of(9, 0), LocalTime.of(10, 0), "COMPLETED");
    var r =
        report(
                List.of(learner),
                List.of(session(), outside),
                List.of(
                    new AcademicEvaluationContext.Mark(10L, 7L, today, "PRESENT", false),
                    new AcademicEvaluationContext.Mark(
                        10L, 8L, today.minusDays(20), "ABSENT", true)))
            .students()
            .getFirst();
    assertEquals("UNKNOWN", r.attendance().status());
    assertEquals("INSUFFICIENT", r.status());
  }
}
