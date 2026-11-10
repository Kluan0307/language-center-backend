package com.example.academic.admission;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramLevelRepository extends JpaRepository<ProgramLevel, Long> {
  List<ProgramLevel> findByProgramIdOrderByRankAscIdAsc(Long programId);

  boolean existsByProgramIdAndCode(Long programId, String code);
}
