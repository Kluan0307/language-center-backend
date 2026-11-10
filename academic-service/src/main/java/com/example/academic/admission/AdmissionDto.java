package com.example.academic.admission;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public final class AdmissionDto {
  public record Level(Long levelId, String code, String name) {}

  public record Policy(
      Long id,
      Long courseId,
      long revision,
      CourseAdmissionPolicy.Mode mode,
      Long programId,
      String programCode,
      String programName,
      Level outputLevel,
      List<Level> acceptedLevels,
      Long confirmedBy,
      String confirmedByName,
      LocalDateTime confirmedAt,
      String reason) {}

  public record CourseView(
      Policy policy,
      boolean canManage,
      String legacyType,
      String legacyLevel,
      String legacyEntryRequirement) {}

  public record Confirm(
      @NotNull @PositiveOrZero Long expectedRevision,
      @NotNull CourseAdmissionPolicy.Mode mode,
      @NotNull @Positive Long programId,
      @NotNull @Positive Long outputLevelId,
      @NotNull @Size(max = 200) List<@NotNull @Positive Long> acceptedLevelIds,
      @NotBlank @Size(min = 3, max = 500) String reason) {}

  public record ClassView(Long classId, long revision, Policy policy, boolean canManage) {}

  public record Apply(
      @NotNull @PositiveOrZero Long expectedRevision,
      @NotNull @Positive Long expectedCourseRevision,
      @NotBlank @Size(min = 3, max = 500) String reason) {}

  public record Change(
      Long id,
      long revision,
      Long oldPolicyId,
      Long newPolicyId,
      String actorName,
      LocalDateTime occurredAt,
      String reason) {}
}
