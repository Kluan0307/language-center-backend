package com.example.academic.lead;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface LeadRepository extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {
  @Override
  @EntityGraph(attributePaths = {"interestedCourse", "convertedStudent"})
  org.springframework.data.domain.Page<Lead> findAll(
      org.springframework.data.jpa.domain.Specification<Lead> spec,
      org.springframework.data.domain.Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select l from Lead l where l.id=:id")
  Optional<Lead> lock(@Param("id") Long id);

  boolean existsByConvertedStudentId(Long studentId);
}
