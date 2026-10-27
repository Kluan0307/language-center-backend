package com.example.academic.course;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CourseRepository
    extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {
  boolean existsByCodeIgnoreCaseAndBranchIdAndIdNot(String code, Long branchId, Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Course c where c.id=:id")
  Optional<Course> lock(@Param("id") Long id);
}
