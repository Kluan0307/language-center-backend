package com.example.teaching.results;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinalResultRepository extends JpaRepository<FinalResult, Long> {
  List<FinalResult> findByClassIdOrderByIdDesc(Long id);

  List<FinalResult> findByEnrollmentIdOrderByRevisionDesc(Long id);

  Optional<FinalResult> findByRequestId(String id);

  List<FinalResult> findByStudentIdOrderByIdDesc(Long id);
}
