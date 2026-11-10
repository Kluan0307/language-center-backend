package com.example.teaching.exam;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamCandidateRepository extends JpaRepository<ExamCandidate, Long> {
  List<ExamCandidate> findByExamIdOrderByStudentNameAsc(Long id);

  void deleteByExamId(Long id);
 java.util.List<ExamCandidate> findByExamIdIn(java.util.Collection<Long> ids);
}
