package com.example.teaching;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.teaching.common.*;
import com.example.teaching.evaluation.*;
import com.example.teaching.integration.*;
import com.example.teaching.results.*;
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
        "spring.datasource.url=jdbc:h2:mem:final-result-tests;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class FinalResultWorkflowTest {
  @Autowired FinalResultService service;
  @Autowired ResultBarrierService barriers;
  @Autowired EvaluationLock locks;
  @Autowired FinalResultRepository results;
  @MockitoBean AcademicDirectory academic;
  @MockitoBean RemoteApi remote;
  @MockitoBean EvaluationService evaluation;

  void actor(Role role) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, 2L, 3L, role, "operator"), null, List.of()));
  }

  void report(String outcome, String study, boolean ready) {
    var row =
        new EvaluationDto.Row(
            10L,
            11L,
            "Learner",
            study,
            BigDecimal.TEN,
            BigDecimal.TEN,
            outcome.equals("INSUFFICIENT") ? null : outcome.equals("PASS"),
            outcome,
            2,
            2,
            0,
            new EvaluationDto.Attendance("KNOWN", BigDecimal.TEN, 1, 1, 0, 0),
            List.of(),
            List.of());
    when(evaluation.get(9L))
        .thenReturn(
            new EvaluationDto.Report(
                9L,
                1,
                "CONFIRMED",
                List.of(row),
                null,
                1,
                0,
                0,
                1,
                LocalDateTime.now(),
                new AcademicEvaluationContext.Duration(90, 1, 90L, 90L, 0, 0, ready),
                List.of()));
  }

  @BeforeEach
  void setup() {
    actor(Role.ADMIN);
    when(academic.schoolClass(9L))
        .thenReturn(new AcademicDirectory.SchoolClass(9L, 3L, 2L, 7L, "Class"));
    when(remote.get(contains("/result-source"), eq(FinalResultService.Source.class)))
        .thenAnswer(
            inv -> {
              var b = barriers.read(9L);
              return new FinalResultService.Source(b.getToken(), b.getSourceRevision());
            });
    report("FAIL", "STUDYING", true);
  }

  FinalResultDto.Confirm request(FinalResultDto.Preview p) {
    return new FinalResultDto.Confirm(
        UUID.randomUUID().toString(), p.fingerprint(), "Reviewed assessment evidence");
  }

  @Test
  void failCanBeFinalAndRetryDoesNotDuplicate() {
    var r = request(service.preview(9L, 10L));
    var saved = service.confirm(9L, 10L, r);
    assertEquals("FAIL", saved.outcome());
    assertEquals(saved.id(), service.confirm(9L, 10L, r).id());
    assertEquals(1, results.findByEnrollmentIdOrderByRevisionDesc(10L).size());
    assertEquals(1, barriers.read(9L).getConfirmedCount());
    assertThrows(
        ApiException.class,
        () ->
            service.confirm(
                9L,
                10L,
                new FinalResultDto.Confirm(r.requestId(), r.fingerprint(), "Different reason")));
  }

  @Test
  void reopeningRetainsSnapshotAndNextRevision() {
    var request = request(service.preview(9L, 10L));
    var saved = service.confirm(9L, 10L, request);
    String snapshot = results.findById(saved.id()).orElseThrow().getSnapshot();
    assertThrows(ApiException.class, () -> locks.lock(9L, 3L));
    var r =
        new FinalResultDto.Reopen(
            UUID.randomUUID().toString(),
            service.classResults(9L).resultRevision(),
            "Correct source after review");
    service.reopen(9L, r);
    service.reopen(9L, r);
    assertEquals(0, barriers.read(9L).getConfirmedCount());
    locks.lock(9L, 3L);
    assertEquals(snapshot, results.findById(saved.id()).orElseThrow().getSnapshot());
    assertEquals("REOPENED", service.confirm(9L, 10L, request).state());
    report("PASS", "COMPLETED", true);
    var next = service.confirm(9L, 10L, request(service.preview(9L, 10L)));
    assertEquals(2, next.revision());
    assertEquals("PASS", next.outcome());
    assertEquals(2, service.history(9L, 10L).size());
  }

  @Test
  void staleSourceCannotCommit() {
    var r = request(service.preview(9L, 10L));
    locks.lock(9L, 3L);
    assertThrows(ApiException.class, () -> service.confirm(9L, 10L, r));
    assertTrue(results.findByClassIdOrderByIdDesc(9L).isEmpty());
  }

  @Test
  void missingConditionsCannotBeFinal() {
    for (var x :
        List.of(
            new String[] {"INSUFFICIENT", "STUDYING", "true"},
            new String[] {"PASS", "STUDYING", "false"},
            new String[] {"PASS", "RESERVED", "true"})) {
      report(x[0], x[1], Boolean.parseBoolean(x[2]));
      var p = service.preview(9L, 10L);
      assertFalse(p.blockers().isEmpty());
      assertThrows(ApiException.class, () -> service.confirm(9L, 10L, request(p)));
    }
  }

  @Test
  void teacherAndStaffCannotConfirm() {
    for (var role : List.of(Role.TEACHER, Role.STAFF)) {
      actor(role);
      assertThrows(ApiException.class, () -> service.preview(9L, 10L));
      assertThrows(
          ApiException.class,
          () ->
              service.reopen(
                  9L,
                  new FinalResultDto.Reopen(UUID.randomUUID().toString(), 0L, "Review results")));
    }
  }

  @Test
  void mismatchedDatabaseBlocksConfirmation() {
    when(remote.get(contains("/result-source"), eq(FinalResultService.Source.class)))
        .thenReturn(new FinalResultService.Source("different", 0));
    assertThrows(ApiException.class, () -> service.preview(9L, 10L));
  }
}
