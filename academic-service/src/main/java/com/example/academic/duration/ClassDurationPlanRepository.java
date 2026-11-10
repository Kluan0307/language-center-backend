package com.example.academic.duration;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassDurationPlanRepository extends JpaRepository<ClassDurationPlan, Long> {
  List<ClassDurationPlan> findByClassIdOrderByRevisionDesc(Long id);
}
