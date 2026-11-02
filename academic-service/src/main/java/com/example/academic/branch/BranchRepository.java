package com.example.academic.branch;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface BranchRepository
    extends JpaRepository<Branch, Long>, JpaSpecificationExecutor<Branch> {
  boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select b from Branch b where b.id=:id")
  Optional<Branch> lock(@Param("id") Long id);
}
