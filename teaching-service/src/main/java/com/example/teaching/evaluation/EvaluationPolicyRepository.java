package com.example.teaching.evaluation;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface EvaluationPolicyRepository extends JpaRepository<EvaluationPolicy, Long> {
  Optional<EvaluationPolicy> findFirstByClassIdOrderByRevisionDesc(Long classId);

  List<EvaluationPolicy> findByClassIdOrderByRevisionDesc(Long id);

  @Query("select count(p)>0 from EvaluationPolicy p join p.rules r where r.examId=:id")
  boolean referencesExam(@Param("id") Long id);

  @Query(
      "select count(p)>0 from EvaluationPolicy p join p.rules r where p.classId=:classId and"
          + " p.revision=(select s.revision from EvaluationState s where s.classId=:classId) and"
          + " r.examId=:id")
  boolean currentReferences(@Param("classId") Long classId, @Param("id") Long id);
}
