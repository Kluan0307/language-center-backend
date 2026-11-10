package com.example.academic.assessment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentEventRepository extends JpaRepository<AssessmentEvent, Long> {
  List<AssessmentEvent> findByAssessmentIdOrderByIdDesc(Long id);
}
