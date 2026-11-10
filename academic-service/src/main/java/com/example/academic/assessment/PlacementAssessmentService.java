package com.example.academic.assessment;

import com.example.academic.admission.ProgramCatalogService;
import com.example.academic.branch.BranchService;
import com.example.academic.common.*;
import com.example.academic.common.Query;
import com.example.academic.security.*;
import com.example.academic.student.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class PlacementAssessmentService {
  private final PlacementAssessmentRepository repository;
  private final AssessmentEventRepository events;
  private final StudentService students;
  private final BranchService branches;
  private final ProgramCatalogService catalog;
  private final EntityManager em;

  private void operations() {
    if (!Set.of(Role.SUPER_ADMIN, Role.ADMIN, Role.STAFF).contains(Users.current().role()))
      throw ApiException.forbidden();
  }

  private Student student(Long id, boolean write) {
    operations();
    var s = students.require(id);
    if (write) {
      branches.lock(s.getBranchId());
      s = students.lock(id);
    }
    return s;
  }

  private PlacementAssessment require(Long studentId, Long id, boolean write) {
    var a = repository.findById(id).orElseThrow(ApiException::missing);
    if (!a.getStudent().getId().equals(studentId)) throw ApiException.missing();
    if (write) em.refresh(a, LockModeType.PESSIMISTIC_WRITE);
    return a;
  }

  private void version(PlacementAssessment a, Long v) {
    if (v == null) throw ApiException.bad("Thiếu phiên bản đánh giá đã đọc");
    if (v != a.getVersion()) throw ApiException.conflict("Đánh giá đã thay đổi; hãy tải lại");
  }

  @Transactional(readOnly = true)
  public PageResponse<AssessmentDto> list(
      Long studentId, int page, int size, Long programId, PlacementAssessment.Status status) {
    student(studentId, false);
    return PageResponse.of(
        repository
            .findAll(
                Query.<PlacementAssessment>eq("student.id", studentId)
                    .and(Query.eq("programId", programId))
                    .and(Query.eq("status", status)),
                Query.page(page, size))
            .map(AssessmentDto::of));
  }

  @Transactional(readOnly = true)
  public AssessmentDto get(Long studentId, Long id) {
    student(studentId, false);
    return AssessmentDto.of(require(studentId, id, false));
  }

  @Transactional(readOnly = true)
  public List<AssessmentDto> current(Long studentId) {
    student(studentId, false);
    return repository
        .findByStudentIdAndStatusOrderByProgramIdAsc(
            studentId, PlacementAssessment.Status.CONFIRMED)
        .stream()
        .map(AssessmentDto::of)
        .toList();
  }

  private String reason(String s) {
    if (s == null || s.trim().length() < 3 || s.trim().length() > 500)
      throw ApiException.bad("Nhập lý do từ 3 đến 500 ký tự");
    return s.trim();
  }

  private void data(PlacementAssessment a, AssessmentDto.Draft r) {
    if (r.assessedOn() == null || r.assessedOn().isAfter(LocalDate.now(BusinessTime.ZONE)))
      throw ApiException.bad("Ngày đánh giá không ở tương lai");
    if (r.method() == null) throw ApiException.bad("Chọn phương thức đánh giá");
    if (r.method() == PlacementAssessment.Method.TEST && (r.score() == null || r.maxScore() == null)
        || r.score() == null && r.maxScore() != null
        || r.score() != null && r.maxScore() == null)
      throw ApiException.bad("Điểm và thang điểm phải cùng có; bài test bắt buộc có điểm");
    if (r.score() != null
        && (r.score().signum() < 0
            || r.maxScore().signum() <= 0
            || r.score().compareTo(r.maxScore()) > 0))
      throw ApiException.bad("Điểm phải từ 0 đến thang điểm dương");
    var p = catalog.lock(r.programId());
    catalog.active(p);
    var l = catalog.level(r.levelId(), p.getId(), true);
    a.setProgramId(p.getId());
    a.setProgramCode(p.getCode());
    a.setProgramName(p.getName());
    a.setLevelId(l.getId());
    a.setLevelCode(l.getCode());
    a.setLevelName(l.getName());
    a.setMethod(r.method());
    a.setAssessedOn(r.assessedOn());
    a.setAssessorName(r.assessorName().trim());
    a.setSource(r.source().trim());
    a.setScore(r.score());
    a.setMaxScore(r.maxScore());
  }

  private void event(PlacementAssessment a, String before, String action, String reason) {
    a.setUpdatedAt(LocalDateTime.now(BusinessTime.ZONE));
    repository.saveAndFlush(a);
    var e = new AssessmentEvent();
    e.setAssessmentId(a.getId());
    e.setBeforeState(before);
    e.setAfterState(AssessmentDto.of(a).toString());
    e.setAction(action);
    e.setReason(reason);
    e.setActorId(Users.current().accountId());
    e.setActorName(Users.current().username());
    e.setOccurredAt(LocalDateTime.now(BusinessTime.ZONE));
    e.setAssessmentVersion(a.getVersion());
    events.save(e);
  }

  public AssessmentDto save(Long studentId, Long id, AssessmentDto.Draft r) {
    var s = student(studentId, true);
    var a = id == null ? new PlacementAssessment() : require(studentId, id, true);
    if (id != null) {
      version(a, r.version());
      if (a.getStatus() != PlacementAssessment.Status.DRAFT)
        throw ApiException.conflict("Kết quả đã xác nhận không được ghi đè; hãy tạo bản thay thế");
    }
    String before = id == null ? "" : AssessmentDto.of(a).toString();
    data(a, r);
    if (id == null) {
      a.setStudent(s);
      a.setBranchId(s.getBranchId());
      a.setRecordedBy(Users.current().accountId());
      a.setRecordedByName(Users.current().username());
      a.setCreatedAt(LocalDateTime.now(BusinessTime.ZONE));
    }
    event(
        a,
        before,
        id == null ? "DRAFT_CREATED" : "DRAFT_UPDATED",
        id == null ? null : reason(r.reason()));
    return AssessmentDto.of(a);
  }

  public AssessmentDto confirm(Long studentId, Long id, AssessmentDto.Action r) {
    student(studentId, true);
    var a = require(studentId, id, true);
    version(a, r.version());
    if (a.getStatus() != PlacementAssessment.Status.DRAFT)
      throw ApiException.conflict(
          "Chỉ xác nhận bản nháp; tải lại kết quả nếu phản hồi trước bị gián đoạn");
    String why = reason(r.reason());
    var p = catalog.lock(a.getProgramId());
    catalog.active(p);
    catalog.level(a.getLevelId(), p.getId(), true);
    var current =
        repository
            .findByStudentIdAndProgramIdAndStatus(
                studentId, p.getId(), PlacementAssessment.Status.CONFIRMED)
            .orElse(null);
    if (!Objects.equals(r.expectedCurrentId(), current == null ? null : current.getId()))
      throw ApiException.conflict(
          "Kết quả hiện hành đã thay đổi; kiểm tra lại trước khi xác nhận thay thế");
    String beforeConfirmation = AssessmentDto.of(a).toString();
    if (current != null) {
      var before = AssessmentDto.of(current).toString();
      current.setStatus(PlacementAssessment.Status.SUPERSEDED);
      current.setSupersededById(a.getId());
      event(current, before, "SUPERSEDED", why);
      a.setSupersedesId(current.getId());
    }
    var before = beforeConfirmation;
    a.setStatus(PlacementAssessment.Status.CONFIRMED);
    a.setConfirmedAt(LocalDateTime.now(BusinessTime.ZONE));
    a.setConfirmedBy(Users.current().accountId());
    a.setConfirmedByName(Users.current().username());
    event(a, before, "CONFIRMED", why);
    return AssessmentDto.of(a);
  }

  public AssessmentDto revoke(Long studentId, Long id, AssessmentDto.Action r) {
    student(studentId, true);
    var a = require(studentId, id, true);
    version(a, r.version());
    if (a.getStatus() != PlacementAssessment.Status.DRAFT
        && a.getStatus() != PlacementAssessment.Status.CONFIRMED)
      throw ApiException.conflict("Đánh giá không còn ở trạng thái có thể thu hồi");
    var before = AssessmentDto.of(a).toString();
    a.setStatus(PlacementAssessment.Status.REVOKED);
    event(a, before, "REVOKED", reason(r.reason()));
    return AssessmentDto.of(a);
  }

  @Transactional(readOnly = true)
  public List<AssessmentDto.Event> history(Long studentId, Long id) {
    student(studentId, false);
    require(studentId, id, false);
    return events.findByAssessmentIdOrderByIdDesc(id).stream()
        .map(
            e ->
                new AssessmentDto.Event(
                    e.getId(),
                    e.getAction(),
                    e.getBeforeState(),
                    e.getAfterState(),
                    e.getReason(),
                    e.getActorName(),
                    e.getOccurredAt(),
                    e.getAssessmentVersion()))
        .toList();
  }
}
