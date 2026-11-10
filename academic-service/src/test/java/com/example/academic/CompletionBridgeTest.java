package com.example.academic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.academic.common.*;
import com.example.academic.duration.*;
import com.example.academic.enrollment.*;
import com.example.academic.results.*;
import com.example.academic.schoolclass.*;
import com.example.academic.student.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

class CompletionBridgeTest {
  RemoteApi remote = mock(RemoteApi.class);
  ResultBarrierService barriers = mock(ResultBarrierService.class);
  DurationProgressService duration = mock(DurationProgressService.class);
  CompletionBridge bridge = new CompletionBridge(remote, barriers, duration);
  Enrollment enrollment;
  ResultBarrier barrier;

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(bridge, "teachingUrl", "http://teaching");
    var c = new SchoolClass();
    c.setId(1L);
    c.setBranchId(2L);
    var s = new Student();
    s.setId(3L);
    enrollment = new Enrollment();
    enrollment.setId(4L);
    enrollment.setSchoolClass(c);
    enrollment.setStudent(s);
    barrier = new ResultBarrier();
    barrier.setToken("shared");
    barrier.setSourceRevision(5);
    when(barriers.lock(1L, 2L)).thenReturn(barrier);
  }

  CompletionBridge.Result result(String state, String outcome, long source, String token) {
    return new CompletionBridge.Result(6L, 1L, 4L, 3L, 7, state, outcome, source, token);
  }

  void upstream(CompletionBridge.Result r) {
    when(remote.get(anyString(), eq(CompletionBridge.Results.class)))
        .thenReturn(new CompletionBridge.Results(1L, 1, List.of(r)));
  }

  @Test
  void passAndFailCompleteWithExactReference() {
    for (String outcome : List.of("PASS", "FAIL")) {
      upstream(result("CONFIRMED", outcome, 5, "shared"));
      assertDoesNotThrow(() -> bridge.validate(enrollment, 6L, 7L));
    }
    verify(duration, times(2)).assertReadyToClose(enrollment.getSchoolClass());
  }

  @Test
  void exactReferenceRequired() {
    upstream(result("CONFIRMED", "PASS", 5, "shared"));
    assertThrows(ApiException.class, () -> bridge.validate(enrollment, null, null));
    assertThrows(ApiException.class, () -> bridge.validate(enrollment, 5L, 7L));
    assertThrows(ApiException.class, () -> bridge.validate(enrollment, 6L, 6L));
  }

  @Test
  void reopenedStaleForeignAndInsufficientCannotComplete() {
    for (var r :
        List.of(
            result("REOPENED", "PASS", 5, "shared"),
            result("CONFIRMED", "PASS", 4, "shared"),
            result("CONFIRMED", "PASS", 5, "other"),
            result("CONFIRMED", "INSUFFICIENT", 5, "shared"))) {
      upstream(r);
      assertThrows(ApiException.class, () -> bridge.validate(enrollment, 6L, 7L));
    }
  }

  @Test
  void outageDoesNotMutateEnrollment() {
    when(remote.get(anyString(), eq(CompletionBridge.Results.class)))
        .thenThrow(
            new ApiException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Teaching unavailable"));
    assertThrows(ApiException.class, () -> bridge.validate(enrollment, 6L, 7L));
    assertNull(enrollment.getCompletionResultId());
    assertEquals(Enrollment.StudyStatus.STUDYING, enrollment.getStudyStatus());
  }
}
