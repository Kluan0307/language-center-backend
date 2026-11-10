package com.example.teaching.grade;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GradeDto(
    Long id,
    Long examId,
    Long studentId,
    String studentName,
    BigDecimal score,
    Long enteredBy,
    String enteredByName,
    LocalDateTime enteredAt,
    Long lastModifiedBy,
    String lastModifiedByName,
    LocalDateTime lastModifiedAt,
    String modificationReason,
    String comment) {
  public static GradeDto of(Grade g) {
    return new GradeDto(
        g.getId(),
        g.getExam().getId(),
        g.getStudentId(),
        g.getStudentName(),
        g.getScore(),
        g.getEnteredBy(),
        g.getEnteredByName(),
        g.getEnteredAt(),
        g.getLastModifiedBy(),
        g.getLastModifiedByName(),
        g.getLastModifiedAt(),
        g.getModificationReason(),
        g.getComment());
  }

  public record Request(
      @NotNull @Positive Long examId,
      @NotNull @Positive Long studentId,
      @NotNull @DecimalMin("0") @Digits(integer = 5, fraction = 2) BigDecimal score,
      @Size(max = 1000) String comment,
      @Size(max = 500) String reason) {}

  public record Change(
      BigDecimal oldScore,
      BigDecimal newScore,
      Long modifiedBy,
      String modifiedByName,
      LocalDateTime modifiedAt,
      String reason) {}
}
