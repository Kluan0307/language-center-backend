package com.example.teaching.exam;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExamDto(
    Long id,
    Long branchId,
    Long classId,
    String name,
    String examType,
    LocalDate examDate,
    BigDecimal weight,
    BigDecimal maxScore,long version) {
  public static ExamDto of(Exam e) {
    return new ExamDto(
        e.getId(),
        e.getBranchId(),
        e.getClassId(),
        e.getName(),
        e.getExamType(),
        e.getExamDate(),
        e.getWeight(),
        e.getMaxScore(),e.getVersion());
  }

  public record Request(
      @NotNull @Positive Long classId,
      @NotBlank @Size(max = 255) String name,
      @NotBlank @Size(max = 50) String examType,
      @NotNull LocalDate examDate,
      @NotNull
          @DecimalMin(value = "0", inclusive = false)
          @DecimalMax("100")
          @Digits(integer = 3, fraction = 2)
          BigDecimal weight,
      @NotNull
          @DecimalMin(value = "0", inclusive = false)
          @DecimalMax("10000")
          @Digits(integer = 5, fraction = 2)
          BigDecimal maxScore) {}
}
