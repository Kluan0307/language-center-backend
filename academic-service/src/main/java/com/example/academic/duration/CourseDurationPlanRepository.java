package com.example.academic.duration;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseDurationPlanRepository extends JpaRepository<CourseDurationPlan, Long> {
  List<CourseDurationPlan> findByCourseIdOrderByRevisionDesc(Long id);
}
