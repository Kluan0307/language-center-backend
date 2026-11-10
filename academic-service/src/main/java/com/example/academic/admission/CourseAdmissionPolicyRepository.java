package com.example.academic.admission;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseAdmissionPolicyRepository
    extends JpaRepository<CourseAdmissionPolicy, Long> {
  Optional<CourseAdmissionPolicy> findFirstByCourseIdOrderByRevisionDesc(Long courseId);

  List<CourseAdmissionPolicy> findByCourseIdOrderByRevisionDesc(Long courseId);
}
