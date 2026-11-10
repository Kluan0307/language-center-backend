package com.example.academic.placement;

import org.springframework.data.jpa.repository.*;

public interface PlacementExceptionRepository
    extends JpaRepository<PlacementException, Long>, JpaSpecificationExecutor<PlacementException> {
  boolean existsByStudentId(Long id);

  boolean existsByClassId(Long id);
}
