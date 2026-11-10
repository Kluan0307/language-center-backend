package com.example.academic.placement;

import com.example.academic.admission.AdmissionDto;
import com.example.academic.assessment.AssessmentDto;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

public final class PlacementDto {
  public enum Intent {
    ENROLL,
    TRANSFER,
    RESUME
  }

  public record Context(
      @NotNull @Positive Long studentId,
      @NotNull @Positive Long classId,
      @NotNull Intent intent,
      @Positive Long sourceEnrollmentId) {}

  public record Issue(String code, String message) {}

  public record Preview(
      Context context,
      String classCode,
      String className,
      Long branchId,
      String studentName,
      String legacyLevel,
      long classRevision,
      AdmissionDto.Policy policy,
      AssessmentDto assessment,
      String academicStatus,
      List<Issue> blockers,
      boolean eligible,
      boolean canRequestException,
      String fingerprint,
      Long sourceVersion,
      Long reusedDecisionId,
      long currentSize,
      int maxSize) {}

  public record ExceptionRequest(
      @NotNull @jakarta.validation.Valid Context context,
      @NotBlank @Pattern(regexp = "[a-f0-9]{64}") String fingerprint,
      @NotBlank @Size(min = 3, max = 500) String reason) {}

  public record Decision(
      @NotNull @PositiveOrZero Long version,
      @NotNull Boolean approve,
      @NotBlank @Size(min = 3, max = 500) String reason) {}

  public record ExceptionView(
      Long id,
      long version,
      Context context,
      Long branchId,
      String studentName,
      String classCode,
      String status,
      String storedStatus,
      String fingerprint,
      String academicStatus,
      String requestedReason,
      Long requestedBy,
      String requestedByName,
      LocalDateTime requestedAt,
      String decisionReason,
      String decidedByName,
      LocalDateTime decidedAt,
      Long consumedEnrollmentId,
      LocalDateTime consumedAt,
      Long policyId,
      long classRevision,
      Long assessmentId,
      Long assessmentVersion,
      Long sourceVersion,
      AdmissionDto.Policy requestedPolicy,
      AssessmentDto requestedAssessment) {}
}
