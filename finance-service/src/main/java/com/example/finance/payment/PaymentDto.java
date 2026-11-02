package com.example.finance.payment;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentDto(
    Long id,
    Long tuitionId,
    BigDecimal amount,
    Payment.PaymentMethod paymentMethod,
    String referenceCode,
    LocalDateTime paidAt,
    Payment.Status status,
    Long branchId) {
  public static PaymentDto of(Payment p) {
    return new PaymentDto(
        p.getId(),
        p.getTuitionId(),
        p.getAmount(),
        p.getPaymentMethod(),
        p.getReferenceCode(),
        p.getPaidAt(),
        p.getStatus(),
        p.getBranchId());
  }

  public record Request(
      @NotNull Long tuitionId,
      @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 2)
          BigDecimal amount,
      @NotNull Payment.PaymentMethod paymentMethod,
      @Size(max = 100) String referenceCode,
      LocalDateTime paidAt,
      @NotNull Payment.Status status,
      Long branchId) {}

  public record StatusRequest(@NotNull Payment.Status status) {}
}
