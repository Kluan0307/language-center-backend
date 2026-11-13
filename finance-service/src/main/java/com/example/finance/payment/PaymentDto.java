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
    Long branchId,
    String receiptNo,
    Long recordedBy,
    String recordedByName,
    String note,
    BigDecimal voidedAmount,
    BigDecimal refundedAmount,
    BigDecimal availableAmount,
    String source, String onlineOrderId, Long studentId, String studentName, Long classId, String className) {
  public PaymentDto withContext(String source,String orderId,com.example.finance.tuition.Tuition t){
    return new PaymentDto(id,tuitionId,amount,paymentMethod,referenceCode,paidAt,status,branchId,receiptNo,recordedBy,recordedByName,note,voidedAmount,refundedAmount,availableAmount,source,orderId,t==null?null:t.getStudentId(),t==null?null:t.getStudentName(),t==null?null:t.getClassId(),t==null?null:t.getClassName());
  }
  public static PaymentDto of(Payment p) {
    return of(p, BigDecimal.ZERO, BigDecimal.ZERO);
  }

  public static PaymentDto of(Payment p, BigDecimal voided, BigDecimal refunded) {
    return new PaymentDto(
        p.getId(),
        p.getTuitionId(),
        p.getAmount(),
        p.getPaymentMethod(),
        p.getReferenceCode(),
        p.getPaidAt(),
        p.getStatus(),
        p.getBranchId(),
        p.getReceiptNo(),
        p.getRecordedBy(),
        p.getRecordedByName(),
        p.getNote(),
        voided,
        refunded,
        p.getStatus() == Payment.Status.PAID
            ? p.getAmount().subtract(voided).subtract(refunded)
            : BigDecimal.ZERO,"UNKNOWN",null,null,null,null,null);
  }

  public record Request(
      @NotNull @Positive Long tuitionId,
      @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 2)
          BigDecimal amount,
      @NotNull Payment.PaymentMethod paymentMethod,
      @Size(max = 100) String referenceCode,
      @NotBlank @Size(max = 100) String requestKey,
      @Size(max = 500) String note) {}
}
