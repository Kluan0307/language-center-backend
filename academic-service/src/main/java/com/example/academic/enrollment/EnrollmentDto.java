package com.example.academic.enrollment;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record EnrollmentDto(
    Long id,
    Long studentId,
    String studentName,
    String currentLevel,
    Long classId,
    String classCode,
    String className,
    LocalDateTime enrolledAt,
    Enrollment.StudyStatus studyStatus,
    Long branchId) {
  public static EnrollmentDto of(Enrollment e) {
    return new EnrollmentDto(
        e.getId(),
        e.getStudent().getId(),
        e.getStudent().getFullName(),
        e.getStudent().getCurrentLevel(),
        e.getSchoolClass().getId(),
        e.getSchoolClass().getCode(),
        e.getSchoolClass().getName(),
        e.getEnrolledAt(),
        e.getStudyStatus(),
        e.getSchoolClass().getBranchId());
  }

  public record Request(@NotNull @Positive Long studentId, @NotNull @Positive Long classId) {}

  public record StatusRequest(@NotNull Enrollment.StudyStatus studyStatus) {}

  public record TransferRequest(@NotNull @Positive Long newClassId) {}
}
