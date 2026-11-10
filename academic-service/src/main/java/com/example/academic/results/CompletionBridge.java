package com.example.academic.results;

import com.example.academic.common.*;
import com.example.academic.duration.DurationProgressService;
import com.example.academic.enrollment.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompletionBridge {
  private final RemoteApi remote;
  private final ResultBarrierService barriers;
  private final DurationProgressService duration;

  @Value("${services.teaching.url:http://localhost:8083}")
  private String teachingUrl;

  public record Result(
      Long id,
      Long classId,
      Long enrollmentId,
      Long studentId,
      long revision,
      String state,
      String outcome,
      long academicSourceRevision,
      String barrierToken) {}

  public record Results(Long classId, long resultRevision, List<Result> results) {}

  public Results results(Long classId) {
    var r =
        remote.get(
            teachingUrl + "/api/v1/teaching/classes/" + classId + "/final-results", Results.class);
    if (r == null || !Objects.equals(r.classId(), classId) || r.results() == null)
      throw ApiException.conflict("Kết quả Teaching không hợp lệ");
    return r;
  }

  public void validate(Enrollment e, Long resultId, Long resultRevision) {
    var c = e.getSchoolClass();
    var b = barriers.lock(c.getId(), c.getBranchId());
    duration.assertReadyToClose(c);
    var r =
        results(c.getId()).results().stream()
            .filter(x -> x.enrollmentId().equals(e.getId()))
            .findFirst()
            .orElseThrow(() -> ApiException.conflict("Chưa có kết quả chính thức"));
    validateResult(e, r, b);
    if (resultId == null
        || resultRevision == null
        || !r.id().equals(resultId)
        || r.revision() != resultRevision)
      throw ApiException.conflict("Cần tham chiếu đúng kết quả chính thức hiện hành");
  }

  public void validateResult(Enrollment e, Result r, ResultBarrier b) {
    if (!Objects.equals(r.classId(), e.getSchoolClass().getId())
        || !Objects.equals(r.studentId(), e.getStudent().getId())
        || !Objects.equals(r.enrollmentId(), e.getId())
        || !"CONFIRMED".equals(r.state())
        || !Set.of("PASS", "FAIL").contains(r.outcome())
        || !Objects.equals(r.barrierToken(), b.getToken())
        || r.academicSourceRevision() != b.getSourceRevision())
      throw ApiException.conflict("Kết quả chưa chốt, đã mở lại hoặc nguồn đã thay đổi");
  }
}
