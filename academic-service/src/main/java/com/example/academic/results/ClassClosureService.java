package com.example.academic.results;

import com.example.academic.branch.BranchService;
import com.example.academic.common.*;
import com.example.academic.duration.DurationProgressService;
import com.example.academic.enrollment.*;
import com.example.academic.schoolclass.*;
import com.example.academic.security.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ClassClosureService {
  private final SchoolClassService classes;
  private final BranchService branches;
  private final ResultBarrierService barriers;
  private final CompletionBridge completion;
  private final DurationProgressService duration;
  private final EnrollmentRepository enrollments;
  private final ClassClosureRepository closures;
  private final ClassClosureEventRepository events;

  public record Request(
      @NotBlank @Size(max = 36) String requestId,
      @NotNull @PositiveOrZero Long expectedRevision,
      @NotBlank @Size(min = 3, max = 500) String reason) {}

  public record View(
      Long classId,
      long revision,
      boolean closed,
      String verification,
      ClassClosure detail,
      List<ClassClosureEvent> history) {}

  @Transactional(readOnly = true)
  public View get(Long id) {
    var c = classes.require(id);
    var row = closures.findById(id).orElse(null);
    var barrier = barriers.read(id);
    boolean review =
        row != null
            && row.isClosed()
            && (barrier == null
                || row.getResultRevision() != barrier.getResultRevision()
                || row.getSourceRevision() != barrier.getSourceRevision());
    return new View(
        id,
        row == null ? 0 : row.getRevision(),
        c.getStatus() == SchoolClass.Status.FINISHED,
        row == null && c.getStatus() == SchoolClass.Status.FINISHED
            ? "LEGACY_UNVERIFIED"
            : review ? "NEEDS_REVIEW" : row == null ? "OPEN" : "VERIFIED",
        row,
        events.findByClassIdOrderByRevisionDesc(id));
  }

  public View change(Long id, boolean close, Request r) {
    if (!Set.of(Role.SUPER_ADMIN, Role.ADMIN).contains(Users.current().role()))
      throw ApiException.forbidden();
    String intent;
    try {
      intent = UUID.fromString(r.requestId()).toString();
    } catch (Exception ex) {
      throw ApiException.bad("requestId phải là UUID");
    }
    String why = r.reason().trim();
    if (why.length() < 3 || why.length() > 500) throw ApiException.bad("Lý do từ 3 đến 500 ký tự");
    var initial = classes.require(id);
    branches.lock(initial.getBranchId());
    var c = classes.lock(id);
    var b = barriers.lock(id, c.getBranchId());
    var replay = events.findById(intent).orElse(null);
    if (replay != null) {
      if (!replay.getClassId().equals(id)
          || replay.isClosed() != close
          || replay.getExpectedRevision() != r.expectedRevision()
          || !replay.getReason().equals(why))
        throw ApiException.conflict("requestId đã dùng cho thao tác khác");
      return get(id);
    }
    var state =
        closures
            .findById(id)
            .orElseGet(
                () -> {
                  var x = new ClassClosure();
                  x.setClassId(id);
                  return x;
                });
    if (state.getRevision() != r.expectedRevision())
      throw ApiException.conflict("Trạng thái đóng lớp đã thay đổi");
    if (close) {
      duration.assertReadyToClose(c);
      var all = enrollments.findBySchoolClassId(id);
      if (all.stream()
          .anyMatch(
              e ->
                  Set.of(Enrollment.StudyStatus.STUDYING, Enrollment.StudyStatus.RESERVED)
                      .contains(e.getStudyStatus())))
        throw ApiException.conflict("Còn đăng ký đang học hoặc bảo lưu; xử lý trước khi đóng lớp");
      var finals = completion.results(id).results();
      for (var e : all)
        if (e.getStudyStatus() == Enrollment.StudyStatus.COMPLETED) {
          var result =
              finals.stream()
                  .filter(x -> x.enrollmentId().equals(e.getId()))
                  .findFirst()
                  .orElseThrow(
                      () -> ApiException.conflict("Đăng ký hoàn thành chưa có kết quả xác minh"));
          completion.validateResult(e, result, b);
          if (!Objects.equals(e.getCompletionResultId(), result.id())
              || !Objects.equals(e.getCompletionResultRevision(), result.revision()))
            throw ApiException.conflict("Chưa đồng bộ phiên bản kết quả hoàn thành");
        }
      c.setStatus(SchoolClass.Status.FINISHED);
    } else {
      if (c.getStatus() != SchoolClass.Status.FINISHED)
        throw ApiException.conflict("Lớp chưa đóng");
      if (b.getConfirmedCount() > 0)
        throw ApiException.conflict("Mở lại kết quả cuối khóa trước khi mở lớp");
      c.setStatus(SchoolClass.Status.ONGOING);
      b.setSourceRevision(b.getSourceRevision() + 1);
    }
    c.setStatusManuallySet(true);
    state.setRevision(state.getRevision() + 1);
    state.setResultRevision(b.getResultRevision());
    state.setSourceRevision(b.getSourceRevision());
    state.setClosed(close);
    state.setReason(why);
    state.setActorId(Users.current().accountId());
    state.setActorName(Users.current().username());
    state.setChangedAt(LocalDateTime.now(BusinessTime.ZONE));
    closures.save(state);
    var event = new ClassClosureEvent();
    event.setRequestId(intent);
    event.setClassId(id);
    event.setExpectedRevision(r.expectedRevision());
    event.setRevision(state.getRevision());
    event.setClosed(close);
    event.setReason(why);
    event.setActorId(state.getActorId());
    event.setActorName(state.getActorName());
    event.setChangedAt(state.getChangedAt());
    events.save(event);
    return get(id);
  }
}
