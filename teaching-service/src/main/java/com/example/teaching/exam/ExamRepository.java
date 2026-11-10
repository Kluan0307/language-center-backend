package com.example.teaching.exam;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ExamRepository extends JpaRepository<Exam, Long>, JpaSpecificationExecutor<Exam> {
  List<Exam> findByClassId(Long id);
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Exam e where e.id=:id")
  Optional<Exam> lock(@Param("id") Long id);
}
