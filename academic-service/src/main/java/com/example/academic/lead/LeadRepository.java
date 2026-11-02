package com.example.academic.lead;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface LeadRepository extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select l from Lead l where l.id=:id")
  Optional<Lead> lock(@Param("id") Long id);

  boolean existsByConvertedStudentId(Long studentId);
}
