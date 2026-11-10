package com.example.teaching.results;

import com.example.teaching.common.*;
import com.example.teaching.evaluation.*;
import com.example.teaching.integration.AcademicDirectory;
import com.example.teaching.security.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class FinalResultService {
  private final ResultBarrierService barriers;
  private final EvaluationLock locks;
  private final FinalResultRepository results;
  private final ResultReopenRepository reopens;
  private final EvaluationService evaluations;
  private final EvaluationPolicyRepository policies;
  private final AcademicDirectory academic;
  private final RemoteApi remote;
  private final JsonMapper json;

  @Value("${services.academic.url}")
  private String academicUrl;

  public record Source(String token, long sourceRevision) {}

  private void manager() {
    if (!Set.of(Role.SUPER_ADMIN, Role.ADMIN).contains(Users.current().role()))
      throw ApiException.forbidden();
  }

  private void fullReader() {
    if (!Set.of(Role.SUPER_ADMIN, Role.ADMIN, Role.TEACHER).contains(Users.current().role()))
      throw ApiException.forbidden();
  }

  private String request(String id) {
    try {
      return UUID.fromString(id).toString();
    } catch (Exception ex) {
      throw ApiException.bad("requestId phải là UUID");
    }
  }

  private String reason(String why) {
    if (why == null || why.trim().length() < 3 || why.trim().length() > 500)
      throw ApiException.bad("Lý do từ 3 đến 500 ký tự");
    return why.trim();
  }

  private FinalResult latest(Long enrollmentId) {
    return results.findByEnrollmentIdOrderByRevisionDesc(enrollmentId).stream()
        .findFirst()
        .orElse(null);
  }

  private FinalResultDto.Summary summary(FinalResult r, ResultBarrier b) {
    return r == null ? null : FinalResultDto.Summary.of(r, b == null ? null : b.getToken());
  }

  private Source source(Long id, ResultBarrier b) {
    var s =
        remote.get(academicUrl + "/api/v1/academic/classes/" + id + "/result-source", Source.class);
    if (!Objects.equals(s.token(), b.getToken()) || s.sourceRevision() != b.getSourceRevision())
      throw ApiException.conflict(
          "Academic và Teaching chưa dùng chung cơ chế điều phối kết quả; không chốt.");
    return s;
  }

  private String fingerprint(Object value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(value.toString().getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private FinalResultDto.Preview calculate(
      Long classId, Long enrollmentId, ResultBarrier b, EvaluationState state) {
    source(classId, b);
    var report = evaluations.get(classId);
    var row =
        report.students().stream()
            .filter(r -> Objects.equals(r.enrollmentId(), enrollmentId))
            .findFirst()
            .orElseThrow(ApiException::missing);
    var reasons = new ArrayList<String>();
    if (Boolean.TRUE.equals(academic.schoolClass(classId).closed()))
      reasons.add("Lớp đã đóng thủ công; mở lại lớp có lý do trước khi chốt phiên bản mới");
    if (!Set.of("PASS", "FAIL").contains(row.status()))
      reasons.add("Chưa đủ dữ liệu đánh giá: " + row.reasons());
    if (!Set.of("STUDYING", "COMPLETED").contains(row.studyStatus()))
      reasons.add("Chỉ chốt đăng ký đang học hoặc đã hoàn thành cần xác nhận lại");
    if (report.duration() == null || !report.duration().timeReadyForClosure())
      reasons.add("Chưa xác minh đủ phút học hoặc còn buổi chưa xử lý");
    var old = latest(enrollmentId);
    if (old != null && "CONFIRMED".equals(old.getState()))
      reasons.add("Đã có kết quả hiện hành; mở lại trước khi chốt phiên bản mới");
    String fp =
        fingerprint(
            List.of(
                classId,
                enrollmentId,
                b.getSourceRevision(),
                state.getSourceRevision(),
                b.getResultRevision(),
                report.policyRevision(),
                row.toString(),
                String.valueOf(report.duration())));
    return new FinalResultDto.Preview(
        enrollmentId,
        row,
        policies.findFirstByClassIdOrderByRevisionDesc(classId).orElse(null),
        report.duration(),
        fp,
        List.copyOf(reasons),
        summary(old, b),
        b.getResultRevision());
  }

  public FinalResultDto.Preview preview(Long classId, Long enrollmentId) {
    manager();
    var c = academic.schoolClass(classId);
    var b = barriers.lock(classId, c.branchId());
    var state = locks.lockForResult(classId, c.branchId());
    return calculate(classId, enrollmentId, b, state);
  }

  public FinalResultDto.Summary confirm(Long classId, Long enrollmentId, FinalResultDto.Confirm r) {
    manager();
    var c = academic.schoolClass(classId);
    var b = barriers.lock(classId, c.branchId());
    var state = locks.lockForResult(classId, c.branchId());
    String id = request(r.requestId()), why = reason(r.reason());
    var replay = results.findByRequestId(id).orElse(null);
    if (replay != null) {
      if (!Objects.equals(replay.getClassId(), classId)
          || !Objects.equals(replay.getEnrollmentId(), enrollmentId)
          || !replay.getFingerprint().equals(r.fingerprint())
          || !replay.getReason().equals(why))
        throw ApiException.conflict("requestId đã dùng cho yêu cầu khác");
      return summary(replay, b);
    }
    var p = calculate(classId, enrollmentId, b, state);
    if (!p.blockers().isEmpty()) throw ApiException.conflict(String.join("; ", p.blockers()));
    if (!p.fingerprint().equals(r.fingerprint()))
      throw ApiException.conflict("Nguồn đã thay đổi; tải lại bản xem trước trước khi chốt");
    var previous = latest(enrollmentId);
    var result = new FinalResult();
    result.setClassId(classId);
    result.setBranchId(c.branchId());
    result.setEnrollmentId(enrollmentId);
    result.setStudentId(p.evaluation().studentId());
    result.setRevision(previous == null ? 1 : previous.getRevision() + 1);
    result.setPolicyRevision(evaluations.get(classId).policyRevision());
    result.setAcademicSourceRevision(b.getSourceRevision());
    result.setTeachingSourceRevision(state.getSourceRevision());
    result.setRequestId(id);
    result.setFingerprint(p.fingerprint());
    result.setOutcome(p.evaluation().status());
    result.setState("CONFIRMED");
    result.setReason(why);
    result.setConfirmedBy(Users.current().accountId());
    result.setConfirmedByName(Users.current().username());
    result.setConfirmedAt(LocalDateTime.now(BusinessTime.ZONE));
    result.setSnapshot(json.writeValueAsString(p));
    results.saveAndFlush(result);
    b.setConfirmedCount(b.getConfirmedCount() + 1);
    b.setResultRevision(b.getResultRevision() + 1);
    return summary(result, b);
  }

  public FinalResultDto.ClassResults reopen(Long classId, FinalResultDto.Reopen r) {
    manager();
    var c = academic.schoolClass(classId);
    var b = barriers.lock(classId, c.branchId());
    String id = request(r.requestId()), why = reason(r.reason());
    var replay = reopens.findById(id).orElse(null);
    if (replay != null) {
      if (!replay.getClassId().equals(classId)
          || replay.getPreviousRevision() != r.expectedRevision()
          || !replay.getReason().equals(why))
        throw ApiException.conflict("requestId đã dùng cho yêu cầu khác");
      return classResults(classId);
    }
    if (b.getResultRevision() != r.expectedRevision())
      throw ApiException.conflict("Danh sách kết quả đã thay đổi; tải lại");
    var all = results.findByClassIdOrderByIdDesc(classId);
    boolean any = false;
    for (var item : all)
      if ("CONFIRMED".equals(item.getState())) {
        item.setState("REOPENED");
        item.setReopenedBy(Users.current().accountId());
        item.setReopenedByName(Users.current().username());
        item.setReopenedAt(LocalDateTime.now(BusinessTime.ZONE));
        item.setReopenReason(why);
        any = true;
      }
    if (!any) throw ApiException.conflict("Không có kết quả đang chốt để mở lại");
    var event = new ResultReopen();
    event.setRequestId(id);
    event.setClassId(classId);
    event.setPreviousRevision(b.getResultRevision());
    event.setActorId(Users.current().accountId());
    event.setActorName(Users.current().username());
    event.setCreatedAt(LocalDateTime.now(BusinessTime.ZONE));
    event.setReason(why);
    reopens.save(event);
    b.setConfirmedCount(0);
    b.setResultRevision(b.getResultRevision() + 1);
    return classResults(classId);
  }

  @Transactional(readOnly = true)
  public FinalResultDto.ClassResults classResults(Long classId) {
    var c = academic.schoolClass(classId);
    var b = barriers.read(classId);
    var latest = new LinkedHashMap<Long, FinalResult>();
    for (var r : results.findByClassIdOrderByIdDesc(classId))
      latest.putIfAbsent(r.getEnrollmentId(), r);
    return new FinalResultDto.ClassResults(
        classId,
        b == null ? 0 : b.getResultRevision(),
        latest.values().stream().map(r -> summary(r, b)).toList());
  }

  @Transactional(readOnly = true)
  public List<FinalResult> history(Long classId, Long enrollmentId) {
    fullReader();
    academic.schoolClass(classId);
    return results.findByEnrollmentIdOrderByRevisionDesc(enrollmentId).stream()
        .filter(r -> r.getClassId().equals(classId))
        .toList();
  }

  @Transactional(readOnly = true)
  public FinalResultDto.Summary byRequest(Long classId, String requestId) {
    academic.schoolClass(classId);
    var r = results.findByRequestId(request(requestId)).orElseThrow(ApiException::missing);
    if (!r.getClassId().equals(classId)) throw ApiException.missing();
    return summary(r, barriers.read(classId));
  }
}
