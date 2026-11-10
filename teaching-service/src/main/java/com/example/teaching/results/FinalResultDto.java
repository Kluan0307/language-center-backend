package com.example.teaching.results;

import com.example.teaching.evaluation.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

public final class FinalResultDto {
  public record Confirm(
      @NotBlank @Size(max = 36) String requestId,
      @NotBlank @Size(max = 64) String fingerprint,
      @NotBlank @Size(min = 3, max = 500) String reason) {}

  public record Reopen(
      @NotBlank @Size(max = 36) String requestId,
      @NotNull @PositiveOrZero Long expectedRevision,
      @NotBlank @Size(min = 3, max = 500) String reason) {}

  public record Preview(
      Long enrollmentId,
      EvaluationDto.Row evaluation,
      EvaluationPolicy policy,
      AcademicEvaluationContext.Duration duration,
      String fingerprint,
      List<String> blockers,
      Summary current,
      long resultRevision) {}

  public record Summary(
      Long id,
      Long classId,
      Long enrollmentId,
      Long studentId,
      long revision,
      String state,
      String outcome,
      long academicSourceRevision,
      String barrierToken,
      LocalDateTime confirmedAt,
      String confirmedByName,
      String reason) {
    public static Summary of(FinalResult r, String token) {
      return new Summary(
          r.getId(),
          r.getClassId(),
          r.getEnrollmentId(),
          r.getStudentId(),
          r.getRevision(),
          r.getState(),
          r.getOutcome(),
          r.getAcademicSourceRevision(),
          token,
          r.getConfirmedAt(),
          r.getConfirmedByName(),
          r.getReason());
    }
  }

  public record ClassResults(Long classId, long resultRevision, List<Summary> results) {}
}
