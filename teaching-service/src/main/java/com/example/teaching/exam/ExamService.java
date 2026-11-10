package com.example.teaching.exam;

import com.example.teaching.common.*;
import com.example.teaching.grade.GradeRepository;
import com.example.teaching.integration.AcademicDirectory;
import com.example.teaching.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ExamService {
  private final ExamRepository repository;
  private final com.example.teaching.evaluation.EvaluationLock evaluationLock;
  private final com.example.teaching.evaluation.EvaluationPolicyRepository policies;
  private final BranchScope scope;
  private final AcademicDirectory academic;
  private final GradeRepository grades;
  private final ExamCandidateRepository candidates;

  public Exam require(Long id) {
    Exam e = repository.findById(id).orElseThrow(ApiException::missing);
    scope.check(e.getBranchId());
    academic.schoolClass(e.getClassId());
    return e;
  }

  public Exam locked(Long id) {
    var initial=repository.findById(id).orElseThrow(ApiException::missing);academic.schoolClass(initial.getClassId());evaluationLock.lock(initial.getClassId(),initial.getBranchId());
    Exam e = repository.lock(id).orElseThrow(ApiException::missing);evaluationLock.refresh(e);
    scope.check(e.getBranchId());
    academic.schoolClass(e.getClassId());
    return e;
  }

  @Transactional(readOnly = true)
  public PageResponse<ExamDto> list(int page, int size, Long branchId, Long classId) {
    if (classId != null) academic.schoolClass(classId);
    if (Users.current().role() == Role.TEACHER && classId == null)
      throw ApiException.bad("Teacher must select a class");
    return PageResponse.of(
        repository
            .findAll(
                Query.<Exam>branch(scope.filter(branchId)).and(Query.eq("classId", classId)),
                Query.page(page, size))
            .map(ExamDto::of));
  }

  public ExamDto save(Long id, ExamDto.Request r) {
    var c = academic.schoolClass(r.classId());
    evaluationLock.lock(c.id(),c.branchId());
    Exam e = id == null ? new Exam() : locked(id);
    if(id!=null&&policies.currentReferences(e.getClassId(),id)&&(!e.getExamDate().equals(r.examDate())||e.getMaxScore().compareTo(r.maxScore())!=0||e.getWeight().compareTo(r.weight())!=0))throw ApiException.conflict("Bài đang thuộc quy tắc đã xác nhận; quản lý điều chỉnh quy tắc trước, không sửa ngầm ngày/thang/trọng số");
    if (id != null && !e.getClassId().equals(r.classId()))
      throw ApiException.bad("Cannot move an exam to another class");
    if (id != null
        && (grades.existsByExamId(id) || e.getRosterFrozenAt() != null)
        && (!e.getExamDate().equals(r.examDate())
            || e.getMaxScore().compareTo(r.maxScore()) != 0
            || e.getWeight().compareTo(r.weight()) != 0))
      throw ApiException.conflict("Cannot change scoring rules after grades exist");
    e.setBranchId(c.branchId());
    e.setClassId(c.id());
    e.setName(r.name().trim());
    e.setExamType(r.examType().trim());
    e.setExamDate(r.examDate());
    e.setWeight(r.weight());
    e.setMaxScore(r.maxScore());
    if (id == null) {
      e.setCreatedBy(Users.current().accountId());
      e.setCreatedByName(Users.current().username());
    }
    return ExamDto.of(repository.save(e));
  }

  public void delete(Long id) {
    Exam e = locked(id);
    if(policies.referencesExam(id))throw ApiException.conflict("Bài có lịch sử quy tắc đánh giá; không được xóa");
    if (grades.existsByExamId(id)) throw ApiException.conflict("Exam has grades");
    candidates.deleteByExamId(id);
    repository.delete(e);
  }
}
