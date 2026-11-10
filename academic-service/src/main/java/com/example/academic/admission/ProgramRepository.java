package com.example.academic.admission;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProgramRepository extends JpaRepository<Program, Long> {
  List<Program> findAllByOrderByCodeAsc();

  boolean existsByCode(String code);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Program p where p.id=:id")
  Optional<Program> lock(@Param("id") Long id);
}
