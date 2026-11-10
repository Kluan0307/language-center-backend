package com.example.teaching.evaluation;

import com.example.teaching.common.*;
import com.example.teaching.exam.*;
import com.example.teaching.integration.AcademicDirectory;
import com.example.teaching.security.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class EvaluationPolicyService {
  private final EvaluationPolicyRepository policies;
  private final EvaluationLock locks;
  private final ExamRepository exams;
  private final AcademicDirectory academic;

  private boolean manager() {
    return Set.of(Role.SUPER_ADMIN, Role.ADMIN).contains(Users.current().role());
  }

  private void readRole() {
    if (!manager() && Users.current().role() != Role.TEACHER) throw ApiException.forbidden();
  }

  @Transactional(readOnly = true)
  public EvaluationDto.PolicyView get(Long id) {
    readRole();
    var school=academic.schoolClass(id);
    var p = policies.findFirstByClassIdOrderByRevisionDesc(id).orElse(null);
    return new EvaluationDto.PolicyView(
        id,
        p == null ? 0 : p.getRevision(),
        p == null ? "UNCONFIGURED" : Objects.equals(p.getCourseId(),school.courseId())?"CONFIRMED":"STALE_CLASS_CONTEXT",
        manager(),
        p);
  }

  @Transactional(readOnly = true)
  public List<EvaluationPolicy> history(Long id) {
    readRole();
    academic.schoolClass(id);
    return policies.findByClassIdOrderByRevisionDesc(id);
  }

  public EvaluationDto.PolicyView confirm(Long id, EvaluationDto.Confirm r) {
    if (!manager()) throw ApiException.forbidden();
    var c = academic.schoolClass(id);
    var state = locks.lock(id, c.branchId());
    if (r.expectedRevision() == null || r.expectedRevision() != state.getRevision())
      throw ApiException.conflict("Quy tắc đã thay đổi; tải lại");
    if (r.exams() == null
        || r.exams().isEmpty()
        || r.exams().size() > 200
        || r.exams().stream().map(EvaluationDto.RuleRequest::examId).distinct().count()
            != r.exams().size()) throw ApiException.bad("Chọn 1–200 bài khác nhau");
    if (r.passPercent() == null
        || r.passPercent().signum() < 0
        || r.passPercent().compareTo(java.math.BigDecimal.valueOf(100)) > 0
        || r.minAttendancePercent() != null
            && (r.minAttendancePercent().signum() < 0
                || r.minAttendancePercent().compareTo(java.math.BigDecimal.valueOf(100)) > 0))
      throw ApiException.bad("Ngưỡng từ 0 đến100 phần trăm");
    if (r.reason() == null || r.reason().trim().length() < 3 || r.reason().trim().length() > 500)
      throw ApiException.bad("Nhập lý do3–500 ký tự");
    var p = new EvaluationPolicy();
    p.setClassId(id);
    p.setBranchId(c.branchId());
    p.setCourseId(c.courseId());
    p.setRevision(Math.addExact(state.getRevision(), 1));
    p.setPassPercent(r.passPercent());
    p.setMinAttendancePercent(r.minAttendancePercent());
    for (var item :
        r.exams().stream()
            .sorted(Comparator.comparing(EvaluationDto.RuleRequest::examId))
            .toList()) {
      var beforeLock=exams.findById(item.examId()).orElseThrow(ApiException::missing);if(!beforeLock.getClassId().equals(id))throw ApiException.bad("Bài không thuộc lớp");
      var e = exams.lock(item.examId()).orElseThrow(ApiException::missing);
      locks.refresh(e);
      if (!e.getClassId().equals(id)) throw ApiException.bad("Bài không thuộc lớp");
      if (item.expectedExamVersion() == null || item.expectedExamVersion() != e.getVersion())
        throw ApiException.conflict("Bài kiểm tra đã thay đổi; tải lại");
      if (item.weight() == null
          || item.weight().signum() <= 0
          || item.weight().compareTo(java.math.BigDecimal.valueOf(100)) > 0
          || e.getMaxScore() == null
          || e.getMaxScore().signum() <= 0)
        throw ApiException.bad("Thang điểm và trọng số phải dương");
      p.getRules()
          .add(
              new EvaluationPolicy.Rule(
                  e.getId(),
                  e.getVersion(),
                  e.getName(),
                  e.getExamDate(),
                  Boolean.TRUE.equals(item.required()),
                  item.weight(),
                  e.getMaxScore()));
    }
    p.setActorId(Users.current().accountId());
    p.setActorName(Users.current().username());
    p.setConfirmedAt(LocalDateTime.now(BusinessTime.ZONE));
    p.setReason(r.reason().trim());
    policies.saveAndFlush(p);
    state.setRevision(p.getRevision());
    return new EvaluationDto.PolicyView(id, p.getRevision(), "CONFIRMED", true, p);
  }
}
