package com.example.finance.expense;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ExpenseDto(
    Long id,
    Expense.Category category,
    String title,
    BigDecimal amount,
    String currency,
    Long branchId,
    String description,
    Expense.Status status) {
  public static ExpenseDto of(Expense e) {
    return new ExpenseDto(
        e.getId(),
        e.getCategory(),
        e.getTitle(),
        e.getAmount(),
        e.getCurrency(),
        e.getBranchId(),
        e.getDescription(),
        e.getStatus());
  }

  public record Request(
      @NotNull Expense.Category category,
      @NotBlank @Size(max = 255) String title,
      @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 2)
          BigDecimal amount,
      @NotBlank @Size(max = 10) String currency,
      Long branchId,
      String description,
      Expense.Status status) {}

  public record StatusRequest(@NotNull Expense.Status status) {}
}
