package com.example.teaching;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.teaching.assistant.*;
import com.example.teaching.common.*;
import com.example.teaching.exam.*;
import com.example.teaching.grade.*;
import com.example.teaching.integration.AcademicDirectory;
import com.example.teaching.publication.*;
import com.example.teaching.security.*;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:assistant-teaching;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class AssistantQueryTest {
  @Autowired TeachingReadService service;
  @Autowired EntityManager em;
  @MockitoBean AcademicDirectory academic;
  @MockitoBean RemoteApi remote;
  Exam exam;

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

  @BeforeEach
  void seed() {
    actor(Role.ADMIN, 11L, List.of(1L, 2L));
    exam = new Exam();
    exam.setBranchId(1L);
    exam.setClassId(9L);
    exam.setName("Exam");
    exam.setExamType("FINAL");
    exam.setExamDate(LocalDate.now());
    exam.setMaxScore(BigDecimal.TEN);
    exam.setWeight(BigDecimal.ONE);
    exam.setRosterFrozenAt(LocalDateTime.now());
    em.persist(exam);
    for (long id : List.of(11L, 12L)) {
      var c = new ExamCandidate();
      c.setExamId(exam.getId());
      c.setStudentId(id);
      c.setEnrollmentId(id + 100);
      c.setStudentName("Student" + id);
      c.setSource("VERIFIED");
      em.persist(c);
    }
    var g = new Grade();
    g.setBranchId(1L);
    g.setExam(exam);
    g.setStudentId(11L);
    g.setStudentName("Student11");
    g.setScore(BigDecimal.ZERO);
    g.setEnteredBy(100L);
    g.setComment("PRIVATE_INTERNAL_COMMENT");
    em.persist(g);
    em.flush();
    when(academic.schoolClass(9L))
        .thenAnswer(
            i -> {
              if (Users.current().role() == Role.TEACHER
                  && !Objects.equals(Users.current().profileId(), 11L))
                throw ApiException.missing();
              return new AcademicDirectory.SchoolClass(9L, 1L, 11L, 1L, "Class");
            });
    when(remote.get(contains("/learning-profile"), eq(ManagementProgressService.Profile.class)))
        .thenReturn(
            new ManagementProgressService.Profile(
                new ManagementProgressService.Identity(11L),
                List.of(
                    new ManagementProgressService.Study(
                        new ManagementProgressService.Enrollment(111L, 11L, 9L, 1L)))));
  }

  @Test
  void zeroAndMissingCandidateScoreRemainDifferentAndPrivateCommentsNeverLeave() {
    var data =
        (Map<?, ?>) service.current(ReadQuery.Mode.ACTIVE_BRANCH, null, exam.getId(), 0, 20).data();
    var rows = (PageResponse<?>) data.get("records");
    assertEquals(2, rows.totalElements());
    var zero = (TeachingReadService.GradeRow) rows.content().get(0);
    var missing = (TeachingReadService.GradeRow) rows.content().get(1);
    assertEquals(0, zero.score().signum());
    assertEquals("GRADED", zero.status());
    assertNull(missing.score());
    assertEquals("NOT_GRADED", missing.status());
    assertFalse(data.toString().contains("PRIVATE"));
  }

  @Test
  void staffCannotReadDraftAndSubstituteCannotReadWholeClass() {
    actor(Role.STAFF, 11L, List.of(1L));
    assertThrows(
        org.springframework.security.access.AccessDeniedException.class,
        () -> service.current(ReadQuery.Mode.ACTIVE_BRANCH, null, exam.getId(), 0, 5));
    actor(Role.TEACHER, 12L, List.of(1L));
    assertThrows(
        ApiException.class,
        () -> service.current(ReadQuery.Mode.ACTIVE_BRANCH, null, exam.getId(), 0, 5));
    actor(Role.TEACHER, 11L, List.of(1L));
    assertEquals(
        "ANSWER", service.current(ReadQuery.Mode.ACTIVE_BRANCH, null, exam.getId(), 0, 5).status());
  }

  @Test
  void selectedScopeIsAppliedEvenWhenActorCanReadBothBranches() {
    assertThrows(
        ApiException.class,
        () -> service.current(ReadQuery.Mode.ACTIVE_BRANCH, 2L, exam.getId(), 0, 5));
    assertThrows(
        ApiException.class,
        () -> service.published(ReadQuery.Mode.ACTIVE_BRANCH, 2L, 11L, 111L, 0, 5));
    assertEquals(
        "ANSWER", service.current(ReadQuery.Mode.ALL_ASSIGNED, null, exam.getId(), 0, 5).status());
  }

  @Test
  void unpublishedIsEmptyAndCannotBeReplacedByCurrentGrade() {
    actor(Role.STAFF, 11L, List.of(1L));
    var r = service.published(ReadQuery.Mode.ACTIVE_BRANCH, null, 11L, 111L, 0, 5);
    assertEquals("NO_RESULTS", r.status());
    assertFalse(r.data().toString().contains("PRIVATE"));
  }

  GradePublication publication(long revision, String action) {
    var p = new GradePublication();
    p.setExam(exam);
    p.setClassId(9L);
    p.setBranchId(1L);
    p.setRevision(revision);
    p.setRequestId(UUID.randomUUID().toString());
    p.setFingerprint("a".repeat(64));
    p.setDraftFingerprint("b".repeat(64));
    p.setAction(action);
    p.setReason("Test");
    p.setExamName("Exam");
    p.setExamDate(LocalDate.now());
    p.setMaxScore(BigDecimal.TEN);
    p.setPublishedAt(LocalDateTime.now());
    em.persist(p);
    return p;
  }

  @Test
  void publishedSnapshotRemainsZeroAndRetractionRemovesIt() {
    var publication = publication(1, "PUBLISH");
    var score = new PublishedGrade();
    score.setPublication(publication);
    score.setStudentId(11L);
    score.setEnrollmentId(111L);
    score.setScore(BigDecimal.ZERO);
    score.setStudentComment("PRIVATE_FEEDBACK");
    em.persist(score);
    em.flush();
    actor(Role.STAFF, 11L, List.of(1L));
    var r = service.published(ReadQuery.Mode.ACTIVE_BRANCH, null, 11L, 111L, 0, 5);
    assertEquals("ANSWER", r.status());
    var data = (Map<?, ?>) r.data();
    var row =
        (TeachingReadService.PublishedRow)
            ((PageResponse<?>) data.get("records")).content().getFirst();
    assertEquals(0, row.score().signum());
    assertFalse(row.toString().contains("PRIVATE"));
    publication(2, "RETRACT");
    em.flush();
    assertEquals(
        "NO_RESULTS",
        service.published(ReadQuery.Mode.ACTIVE_BRANCH, null, 11L, 111L, 0, 5).status());
  }

  @Test
  void identifiersPageSizeAndStudentRolesAreRejected() {
    assertThrows(
        ApiException.class, () -> service.current(ReadQuery.Mode.ACTIVE_BRANCH, null, -1L, 0, 5));
    assertThrows(
        ApiException.class,
        () -> service.current(ReadQuery.Mode.ACTIVE_BRANCH, null, exam.getId(), 0, 21));
    assertThrows(
        ApiException.class,
        () -> service.published(ReadQuery.Mode.ACTIVE_BRANCH, null, 11L, 999L, 0, 5));
    actor(Role.STUDENT, 11L, List.of(1L));
    assertThrows(
        org.springframework.security.access.AccessDeniedException.class,
        () -> service.published(ReadQuery.Mode.ACTIVE_BRANCH, null, 11L, 111L, 0, 5));
  }
}
