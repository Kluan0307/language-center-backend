package com.example.academic.assessment;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface PlacementAssessmentRepository
    extends JpaRepository<PlacementAssessment, Long>,
        JpaSpecificationExecutor<PlacementAssessment> {
  Optional<PlacementAssessment> findByStudentIdAndProgramIdAndStatus(
      Long studentId, Long programId, PlacementAssessment.Status status);

  List<PlacementAssessment> findByStudentIdAndStatusOrderByProgramIdAsc(
      Long studentId, PlacementAssessment.Status status);

  boolean existsByStudentId(Long studentId);
}
