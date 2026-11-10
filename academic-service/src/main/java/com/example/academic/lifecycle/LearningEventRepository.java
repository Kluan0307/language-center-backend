package com.example.academic.lifecycle;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningEventRepository extends JpaRepository<LearningEvent, Long> {
  List<LearningEvent> findByStudentIdOrderByIdDesc(Long id);

  List<LearningEvent> findByEnrollmentIdOrderByIdDesc(Long id);
}
