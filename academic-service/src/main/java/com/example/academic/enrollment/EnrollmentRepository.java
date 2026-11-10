package com.example.academic.enrollment;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface EnrollmentRepository
    extends JpaRepository<Enrollment, Long>, JpaSpecificationExecutor<Enrollment> {
  @Override
  @EntityGraph(attributePaths = {"student", "schoolClass"})
  org.springframework.data.domain.Page<Enrollment> findAll(
      org.springframework.data.jpa.domain.Specification<Enrollment> spec,
      org.springframework.data.domain.Pageable pageable);

  @EntityGraph(attributePaths = "schoolClass")
  List<Enrollment> findByStudentId(Long studentId);

  @EntityGraph(attributePaths = {"student", "schoolClass"})
  List<Enrollment> findBySchoolClassId(Long classId);

  List<Enrollment> findByStudentIdAndStudyStatus(Long studentId, Enrollment.StudyStatus status);

  List<Enrollment> findBySchoolClassIdAndStudyStatus(Long classId, Enrollment.StudyStatus status);

  long countBySchoolClassIdAndStudyStatus(Long classId, Enrollment.StudyStatus status);

  boolean existsByStudentId(Long studentId);

  boolean existsByStudentIdAndStudyStatus(Long studentId, Enrollment.StudyStatus status);

  boolean existsBySchoolClassId(Long classId);

  boolean existsByStudentIdAndSchoolClassIdAndStudyStatusAndIdNot(
      Long studentId, Long classId, Enrollment.StudyStatus status, Long id);
}
