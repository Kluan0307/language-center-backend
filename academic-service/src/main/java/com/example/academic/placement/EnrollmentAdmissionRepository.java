package com.example.academic.placement;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentAdmissionRepository extends JpaRepository<EnrollmentAdmission, Long> {
  Optional<EnrollmentAdmission> findFirstByEnrollmentIdOrderByIdDesc(Long id);

  List<EnrollmentAdmission> findByEnrollmentIdOrderByIdDesc(Long id);
}
