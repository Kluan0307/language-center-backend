package com.example.finance.tuition;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TuitionDto(
    Long id,
    Long studentId,
    Long classId,
    BigDecimal amount,
    String currency,
    LocalDate dueDate,
    Tuition.Status status,
    Long branchId) {
  public static TuitionDto of(Tuition t) {
    return new TuitionDto(
        t.getId(),
        t.getStudentId(),
        t.getClassId(),
        t.getAmount(),
        t.getCurrency(),
        t.getDueDate(),
        t.getStatus(),
        t.getBranchId());
  }

  public record Request(
      @NotNull Long studentId,
      @NotNull Long classId,
      @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 2)
          BigDecimal amount,
      @NotBlank @Size(max = 10) String currency,
      @NotNull LocalDate dueDate,
      Long branchId) {}

  public record StatusRequest(@NotNull Tuition.Status status) {}
}
