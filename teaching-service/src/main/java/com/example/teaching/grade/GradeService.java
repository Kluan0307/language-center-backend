package com.example.teaching.grade;

import com.example.teaching.common.*;
import com.example.teaching.exam.*;
import com.example.teaching.integration.AcademicDirectory;
import com.example.teaching.security.*;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class GradeService {
  private final GradeRepository repository;
  private final GradeChangeRepository changes;
  private final ExamService exams;
  private final AcademicDirectory academic;
  private final BranchScope scope;
  private final ExamRosterService rosters;

  @Transactional(readOnly = true)
  public PageResponse<GradeDto> list(Long examId, int page, int size) {
    exams.require(examId);
    return PageResponse.of(
        repository
            .findAll(
                Query.<Grade>eq("exam.id", examId).and(Query.branch(scope.filter(null))),
                Query.page(page, size))
            .map(GradeDto::of));
  }

  public GradeDto save(GradeDto.Request r) {
    Exam exam = exams.locked(r.examId());
    if (r.score().signum() < 0 || r.score().compareTo(exam.getMaxScore()) > 0)
      throw ApiException.bad("Score must be between 0 and maxScore");
    Grade g = repository.findByExamIdAndStudentId(r.examId(), r.studentId()).orElse(null);
    var u = Users.current();
    boolean admin = u.role() == Role.ADMIN || u.role() == Role.SUPER_ADMIN;
    if (admin && g == null) throw ApiException.forbidden();
    if (admin && (r.reason() == null || r.reason().isBlank()))
      throw ApiException.bad("Administrative correction requires a reason");
    if (u.role() == Role.TEACHER) {
      var student =
          rosters.freeze(exam).stream()
              .filter(s -> s.id().equals(r.studentId()))
              .findFirst()
              .orElseThrow(
                  () -> ApiException.bad("Học viên không thuộc danh sách dự thi tại ngày thi"));
      if (g == null) {
        g = new Grade();
        g.setStudentName(student.fullName());
      }
    }
    if (g == null) throw ApiException.forbidden();
    if (g.getId() != null) {
      GradeChange change = new GradeChange();
      change.setGradeId(g.getId());
      change.setOldScore(g.getScore());
      change.setNewScore(r.score());
      change.setModifiedBy(u.accountId());
      change.setModifiedByName(u.username());
      change.setReason(r.reason());
      changes.save(change);
      g.setLastModifiedBy(u.accountId());
      g.setLastModifiedByName(u.username());
      g.setLastModifiedAt(LocalDateTime.now(com.example.teaching.common.BusinessTime.ZONE));
      g.setModificationReason(r.reason());
    } else {
      g.setEnteredBy(u.accountId());
      g.setEnteredByName(u.username());
    }
    g.setExam(exam);
    g.setBranchId(exam.getBranchId());
    g.setStudentId(r.studentId());
    g.setScore(r.score());
    g.setComment(r.comment());
    return GradeDto.of(repository.save(g));
  }

  @Transactional(readOnly = true)
  public List<GradeDto.Change> history(Long id) {
    Grade g = repository.findById(id).orElseThrow(ApiException::missing);
    scope.check(g.getBranchId());
    exams.require(g.getExam().getId());
    return changes.findByGradeIdOrderByModifiedAtAsc(id).stream()
        .map(
            c ->
                new GradeDto.Change(
                    c.getOldScore(),
                    c.getNewScore(),
                    c.getModifiedBy(),
                    c.getModifiedByName(),
                    c.getModifiedAt(),
                    c.getReason()))
        .toList();
  }

}
