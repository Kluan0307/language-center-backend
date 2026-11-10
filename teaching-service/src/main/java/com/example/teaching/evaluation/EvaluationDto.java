package com.example.teaching.evaluation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.*;
import java.time.*;
import java.util.*;

public final class EvaluationDto {
  public record RuleRequest(
      @NotNull @Positive Long examId,
      @NotNull @PositiveOrZero Long expectedExamVersion,
      @NotNull Boolean required,
      @NotNull
          @DecimalMin(value = "0", inclusive = false)
          @DecimalMax("100")
          @Digits(integer = 3, fraction = 2)
          BigDecimal weight) {}

  public record Confirm(
      @NotNull @PositiveOrZero Long expectedRevision,
      @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2)
          BigDecimal passPercent,
      @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2)
          BigDecimal minAttendancePercent,
      @NotEmpty @Size(max = 200) List<@NotNull @Valid RuleRequest> exams,
      @NotBlank @Size(min = 3, max = 500) String reason) {}

  public record PolicyView(
      Long classId, long revision, String status, boolean canManage, EvaluationPolicy policy) {}

  public record Issue(String code, String message) {}

  public record ExamResult(
      Long examId,
      String examName,
      LocalDate examDate,
      boolean required,
      BigDecimal weight,
      BigDecimal maxScore,
      String applicability,
      String gradeStatus,
      BigDecimal score,
      BigDecimal percent) {}

  public record Attendance(
      String status,
      BigDecimal percent,
      long attended,
      long expected,
      long excusedAbsences,
      long missingRecords) {}

  public record Row(
      Long enrollmentId,
      Long studentId,
      String studentName,
      String studyStatus,
      BigDecimal averagePercent,
      BigDecimal unroundedAveragePercent,
      Boolean passed,
      String status,
      int gradedExams,
      int applicableExams,
      int missingRequiredExams,
      Attendance attendance,
      List<ExamResult> exams,
      List<Issue> reasons) {}

  public record Report(
      Long classId,
      long policyRevision,
      String policyStatus,
      List<Row> students,
      BigDecimal passRate,
      long assessableCount,
      long insufficientCount,
      long passedCount,
      long failedCount,
      LocalDateTime generatedAt,
      AcademicEvaluationContext.Duration duration,
      List<Issue> warnings) {}
}
