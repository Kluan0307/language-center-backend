package com.example.teaching.exam;

import com.example.teaching.common.*;
import com.example.teaching.grade.*;
import com.example.teaching.integration.AcademicDirectory;
import com.example.teaching.security.Users;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ExamRosterService {
  private final ExamCandidateRepository candidates;
  private final GradeRepository grades;
  private final AcademicDirectory academic;

  public record Candidate(Long id,String fullName,String source,Long enrollmentId){public Candidate(Long id,String name,String source){this(id,name,source,null);}}

  public record Roster(
      Long examId,
      LocalDate examDate,
      boolean frozen,
      LocalDateTime frozenAt,
      String frozenByName,
      List<Candidate> students) {}

  public List<Candidate> members(Exam exam) {
    if (exam.getRosterFrozenAt() != null)
      return candidates.findByExamIdOrderByStudentNameAsc(exam.getId()).stream()
          .map(c -> new Candidate(c.getStudentId(), c.getStudentName(), c.getSource(),c.getEnrollmentId()))
          .toList();
    var rows = new TreeMap<Long, Candidate>();
    for (var s : academic.studentsAt(exam.getClassId(), exam.getExamDate()))
      rows.put(s.id(), new Candidate(s.id(),s.fullName(),s.source()==null?"ENROLLMENT_PERIOD":s.source(),s.enrollmentId()));
    for (var g : grades.findByExamId(exam.getId()))
      rows.putIfAbsent(
          g.getStudentId(), new Candidate(g.getStudentId(), g.getStudentName(), "SAVED_GRADE"));
    return List.copyOf(rows.values());
  }

  @Transactional(readOnly = true)
  public Roster get(Exam exam) {
    return new Roster(
        exam.getId(),
        exam.getExamDate(),
        exam.getRosterFrozenAt() != null,
        exam.getRosterFrozenAt(),
        exam.getRosterFrozenByName(),
        members(exam));
  }

  public List<Candidate> freeze(Exam exam) {
    if (exam.getExamDate().isAfter(LocalDate.now(BusinessTime.ZONE)))
      throw ApiException.bad("Chưa đến ngày thi; không được nhập điểm trước");
    var rows = members(exam);
    if (rows.isEmpty()) throw ApiException.conflict("Không có học viên đủ điều kiện tại ngày thi");
    if (exam.getRosterFrozenAt() == null) {
      for (var s : rows) {
        var c = new ExamCandidate();
        c.setExamId(exam.getId());
        c.setStudentId(s.id());
        c.setEnrollmentId(s.enrollmentId());
        c.setStudentName(s.fullName());
        c.setSource(s.source());
        candidates.save(c);
      }
      exam.setRosterFrozenAt(LocalDateTime.now(BusinessTime.ZONE));
      exam.setRosterFrozenBy(Users.current().accountId());
      exam.setRosterFrozenByName(Users.current().username());
      candidates.flush();
    }
    return rows;
  }
}
