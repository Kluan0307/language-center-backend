package com.example.academic.assessment;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

public record AssessmentDto(
    Long id,
    long version,
    Long studentId,
    Long branchId,
    Long programId,
    String programCode,
    String programName,
    Long levelId,
    String levelCode,
    String levelName,
    PlacementAssessment.Method method,
    PlacementAssessment.Status status,
    LocalDate assessedOn,
    String assessorName,
    String source,
    BigDecimal score,
    BigDecimal maxScore,
    Long recordedBy,
    String recordedByName,
    Long confirmedBy,
    String confirmedByName,
    LocalDateTime confirmedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Long supersedesId,
    Long supersededById) {
  public static AssessmentDto of(PlacementAssessment a) {
    return new AssessmentDto(
        a.getId(),
        a.getVersion(),
        a.getStudent().getId(),
        a.getBranchId(),
        a.getProgramId(),
        a.getProgramCode(),
        a.getProgramName(),
        a.getLevelId(),
        a.getLevelCode(),
        a.getLevelName(),
        a.getMethod(),
        a.getStatus(),
        a.getAssessedOn(),
        a.getAssessorName(),
        a.getSource(),
        a.getScore(),
        a.getMaxScore(),
        a.getRecordedBy(),
        a.getRecordedByName(),
        a.getConfirmedBy(),
        a.getConfirmedByName(),
        a.getConfirmedAt(),
        a.getCreatedAt(),
        a.getUpdatedAt(),
        a.getSupersedesId(),
        a.getSupersededById());
  }

  public record Draft(
      @PositiveOrZero Long version,
      @NotNull @Positive Long programId,
      @NotNull @Positive Long levelId,
      @NotNull PlacementAssessment.Method method,
      @NotNull LocalDate assessedOn,
      @NotBlank @Size(max = 255) String assessorName,
      @NotBlank @Size(max = 2000) String source,
      @DecimalMin("0") @Digits(integer = 8, fraction = 4) BigDecimal score,
      @DecimalMin(value = "0", inclusive = false) @Digits(integer = 8, fraction = 4)
          BigDecimal maxScore,
      @Size(max = 500) String reason) {}

  public record Action(
      @NotNull @PositiveOrZero Long version,
      @NotBlank @Size(min = 3, max = 500) String reason,
      @Positive Long expectedCurrentId) {}

  public record Event(
      Long id,
      String action,
      String beforeState,
      String afterState,
      String reason,
      String actorName,
      LocalDateTime occurredAt,
      long assessmentVersion) {}
}
