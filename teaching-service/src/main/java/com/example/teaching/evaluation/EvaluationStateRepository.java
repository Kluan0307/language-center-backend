package com.example.teaching.evaluation;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface EvaluationStateRepository extends JpaRepository<EvaluationState, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from EvaluationState s where s.classId=:id")
  Optional<EvaluationState> lock(@Param("id") Long id);
}
