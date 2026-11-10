package com.example.teaching.evaluation;

import com.example.teaching.common.*;
import com.example.teaching.exam.*;
import com.example.teaching.grade.*;
import com.example.teaching.integration.AcademicDirectory;
import com.example.teaching.security.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
public class EvaluationService {
  private final AcademicDirectory academic;
  private final EvaluationPolicyRepository policies;
  private final ExamRepository exams;
  private final GradeRepository grades;
  private final ExamCandidateRepository candidates;

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public EvaluationDto.Report get(Long classId) {
    if (!Set.of(Role.SUPER_ADMIN, Role.ADMIN, Role.TEACHER).contains(Users.current().role()))
      throw ApiException.forbidden();
    var c = academic.schoolClass(classId);
    var p = policies.findFirstByClassIdOrderByRevisionDesc(classId).orElse(null);
    if(p!=null&&!Objects.equals(p.getCourseId(),c.courseId()))p=null;
    var context =
        academic.evaluationContext(classId, p != null && p.getMinAttendancePercent() != null);
    if (context == null
        || !Objects.equals(context.classId(), classId)
        || !Objects.equals(context.branchId(), c.branchId())
        || context.enrollments() == null) throw ApiException.bad("Nguồn học vụ không hợp lệ");
    var all = exams.findByClassId(classId);
    return EvaluationCalculator.calculate(
        p,
        context,
        all,
        grades.findByExamClassId(classId),
        all.isEmpty()
            ? List.of()
            : candidates.findByExamIdIn(all.stream().map(Exam::getId).toList()),
        LocalDate.now(BusinessTime.ZONE));
  }
}
