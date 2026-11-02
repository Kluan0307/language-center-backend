package com.example.academic.enrollment;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface EnrollmentRepository
    extends JpaRepository<Enrollment, Long>, JpaSpecificationExecutor<Enrollment> {
  List<Enrollment> findByStudentId(Long studentId);

  List<Enrollment> findByStudentIdAndStudyStatus(Long studentId, Enrollment.StudyStatus status);

  List<Enrollment> findBySchoolClassIdAndStudyStatus(Long classId, Enrollment.StudyStatus status);

  long countBySchoolClassIdAndStudyStatus(Long classId, Enrollment.StudyStatus status);

  boolean existsByStudentId(Long studentId);

  boolean existsBySchoolClassId(Long classId);

  boolean existsByStudentIdAndSchoolClassIdAndStudyStatusAndIdNot(
      Long studentId, Long classId, Enrollment.StudyStatus status, Long id);
}
