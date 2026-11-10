package com.example.academic.enrollment;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface EnrollmentPeriodRepository extends JpaRepository<EnrollmentPeriod, Long> {
  List<EnrollmentPeriod> findByEnrollmentId(Long id);
 @EntityGraph(attributePaths="enrollment") List<EnrollmentPeriod> findByEnrollmentStudentId(Long id);

  @EntityGraph(attributePaths = {"enrollment", "enrollment.student"})
  List<EnrollmentPeriod> findByEnrollmentSchoolClassId(Long classId);
}
