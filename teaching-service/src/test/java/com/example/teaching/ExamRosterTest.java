package com.example.teaching;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.teaching.common.*;
import com.example.teaching.exam.*;
import com.example.teaching.grade.*;
import com.example.teaching.integration.AcademicDirectory;
import com.example.teaching.security.*;
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
    properties = "spring.datasource.url=jdbc:h2:mem:exam-roster-tests;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class ExamRosterTest {
  @Autowired GradeService service;
  @Autowired ExamService exams;
  @Autowired ExamRepository examRows;
  @Autowired GradeRepository grades;
  @Autowired ExamRosterService rosters;
  @Autowired ExamCandidateRepository candidates;
  @MockitoBean AcademicDirectory academic;
  Exam exam;
  LocalDate today;

  @BeforeEach
  void setup() {
    today = LocalDate.now(BusinessTime.ZONE);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, 2L, 3L, Role.TEACHER, "teacher"), null, List.of()));
    when(academic.schoolClass(9L))
        .thenReturn(new AcademicDirectory.SchoolClass(9L, 3L, 2L, 7L, "Class"));
    when(academic.studentsAt(eq(9L), any()))
        .thenReturn(
            List.of(
                new AcademicDirectory.Student(11L, "Former learner"),
                new AcademicDirectory.Student(12L, "Other learner")));
    exam = new Exam();
    exam.setClassId(9L);
    exam.setBranchId(3L);
    exam.setName("Exam");
    exam.setExamType("FINAL");
    exam.setExamDate(today.minusDays(2));
    exam.setWeight(BigDecimal.ONE);
    exam.setMaxScore(BigDecimal.TEN);
    examRows.saveAndFlush(exam);
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  GradeDto save(Long student) {
    return service.save(new GradeDto.Request(exam.getId(), student, BigDecimal.ONE, "Good", null));
  }

  @Test
  void formerLearnerCanReceiveGradeUsingExamDayRoster() {
    var g = save(11L);
    assertEquals("Former learner", g.studentName());
    verify(academic).studentsAt(9L, today.minusDays(2));
    verify(academic, never()).students(any());
  }

  @Test
  void firstGradeFreezesWholeRosterAndLaterEnrollmentChangesDoNotAffectIt() {
    save(11L);
    when(academic.studentsAt(eq(9L), any())).thenReturn(List.of());
    save(12L);
    assertEquals(2, candidates.findByExamIdOrderByStudentNameAsc(exam.getId()).size());
    assertTrue(rosters.get(exam).frozen());
    assertEquals("teacher", rosters.get(exam).frozenByName());
  }

  @Test
  void unlistedStudentCannotReceiveGrade() {
    assertThrows(ApiException.class, () -> save(99L));
    assertTrue(grades.findByExamId(exam.getId()).isEmpty());
  }

  @Test
  void futureExamIsPreviewOnly() {
    exam.setExamDate(today.plusDays(1));
    examRows.flush();
    assertFalse(rosters.get(exam).frozen());
    assertThrows(ApiException.class, () -> save(11L));
  }

  @Test
  void dateAndRulesCannotChangeAfterFirstGrade() {
    save(11L);
    assertThrows(
        ApiException.class,
        () ->
            exams.save(
                exam.getId(),
                new ExamDto.Request(9L, "Exam", "FINAL", today, BigDecimal.ONE, BigDecimal.TEN)));
  }

  @Test
  void savedLegacyGradeOwnerRemainsEligibleWithoutKnownPeriod() {
    var g = new Grade();
    g.setExam(exam);
    g.setBranchId(3L);
    g.setStudentId(90L);
    g.setStudentName("Historic learner");
    g.setScore(BigDecimal.ONE);
    g.setEnteredBy(1L);
    g.setEnteredByName("Old teacher");
    grades.saveAndFlush(g);
    assertTrue(
        rosters.get(exam).students().stream()
            .anyMatch(s -> s.id().equals(90L) && s.source().equals("SAVED_GRADE")));
    save(90L);
    assertEquals(
        "Old teacher",
        grades.findByExamIdAndStudentId(exam.getId(), 90L).orElseThrow().getEnteredByName());
  }

  @Test
  void emptyRosterDoesNotFreezeOrCreateGrades() {
    when(academic.studentsAt(eq(9L), any())).thenReturn(List.of());
    assertThrows(ApiException.class, () -> save(11L));
    assertNull(exam.getRosterFrozenAt());
    assertTrue(candidates.findByExamIdOrderByStudentNameAsc(exam.getId()).isEmpty());
  }
}
