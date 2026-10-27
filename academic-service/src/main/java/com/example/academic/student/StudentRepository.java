package com.example.academic.student;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface StudentRepository
    extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {
  boolean existsByPhoneAndBranchIdAndIdNot(String phone, Long branchId, Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from Student s where s.id=:id")
  Optional<Student> lock(@Param("id") Long id);
}
