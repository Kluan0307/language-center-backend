package com.example.teaching.grade;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface GradeRepository
    extends JpaRepository<Grade, Long>, JpaSpecificationExecutor<Grade> {
  java.util.List<Grade> findByExamId(Long examId);

  boolean existsByExamId(Long examId);

  Optional<Grade> findByExamIdAndStudentId(Long examId, Long studentId);

  @EntityGraph(attributePaths = "exam")
  List<Grade> findByExamClassId(Long classId);
}
