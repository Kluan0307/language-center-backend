package com.example.finance.tuition;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface TuitionRepository
    extends JpaRepository<Tuition, Long>, JpaSpecificationExecutor<Tuition> {
  Optional<Tuition> findByEnrollmentId(Long enrollmentId);

  boolean existsByStudentIdAndClassIdAndEnrollmentIdIsNull(Long studentId, Long classId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from Tuition t where t.id=:id")
  Optional<Tuition> lock(@Param("id") Long id);
}
