package com.example.finance.invoice;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record InvoiceDto(
    Long id,
    Long studentId,
    Long paymentId,
    String invoiceNumber,
    BigDecimal totalAmount,
    Invoice.Status status,
    Long branchId) {
  public static InvoiceDto of(Invoice i) {
    return new InvoiceDto(
        i.getId(),
        i.getStudentId(),
        i.getPaymentId(),
        i.getInvoiceNumber(),
        i.getTotalAmount(),
        i.getStatus(),
        i.getBranchId());
  }

  public record Request(
      @NotNull Long studentId,
      @NotNull Long paymentId,
      @NotBlank @Size(max = 100) String invoiceNumber,
      @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 2)
          BigDecimal totalAmount,
      @NotNull Invoice.Status status,
      Long branchId) {}

  public record StatusRequest(@NotNull Invoice.Status status) {}
}
