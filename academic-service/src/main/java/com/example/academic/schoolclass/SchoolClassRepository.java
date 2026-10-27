package com.example.academic.schoolclass;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SchoolClassRepository
    extends JpaRepository<SchoolClass, Long>, JpaSpecificationExecutor<SchoolClass> {
  boolean existsByCodeIgnoreCaseAndBranchIdAndIdNot(String code, Long branchId, Long id);

  boolean existsByRoomId(Long id);

  boolean existsByRoomIdAndMaxSizeGreaterThan(Long id, Integer size);

  boolean existsByCourseIdAndMaxSizeGreaterThan(Long id, Integer size);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from SchoolClass c where c.id=:id")
  Optional<SchoolClass> lock(@Param("id") Long id);
}
