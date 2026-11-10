package com.example.finance.ledger;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TuitionAdjustmentRepository extends JpaRepository<TuitionAdjustment, Long> {
  Optional<TuitionAdjustment> findByBranchIdAndRequestKey(Long branch, String key);

  List<TuitionAdjustment> findByTuitionIdOrderByIdDesc(Long id);
}
