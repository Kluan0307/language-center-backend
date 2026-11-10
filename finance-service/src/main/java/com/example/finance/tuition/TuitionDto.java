package com.example.finance.tuition;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TuitionDto(
    Long id,
    Long enrollmentId,
    Long studentId,
    String studentName,
    Long classId,
    String className,
    Long courseId,
    String courseName,
    BigDecimal originalAmount,
    BigDecimal discountAmount,
    BigDecimal amount,
    BigDecimal paidAmount,
    BigDecimal balance,
    String currency,
    LocalDate dueDate,
    Tuition.Status status,
    boolean overdue,
    Long branchId,
    Long adjustedBy,
    String adjustedByName,
    java.time.LocalDateTime adjustedAt,
    String adjustmentReason,
    long version,
    BigDecimal credit) {
  public static TuitionDto of(Tuition t) {
    return new TuitionDto(
        t.getId(),
        t.getEnrollmentId(),
        t.getStudentId(),
        t.getStudentName(),
        t.getClassId(),
        t.getClassName(),
        t.getCourseId(),
        t.getCourseName(),
        t.getOriginalAmount() == null ? t.getAmount() : t.getOriginalAmount(),
        t.getDiscountAmount() == null ? BigDecimal.ZERO : t.getDiscountAmount(),
        t.getAmount(),
        t.paid(),
        t.balance(),
        t.getCurrency(),
        t.getDueDate(),
        t.paymentStatus(),
        t.overdue(),
        t.getBranchId(),
        t.getAdjustedBy(),
        t.getAdjustedByName(),
        t.getAdjustedAt(),
        t.getAdjustmentReason(),
        t.getVersion(),
        t.credit());
  }

  public record Request(@NotNull @Positive Long enrollmentId) {}

  public record Adjustment(
      @NotNull @DecimalMin("0") @Digits(integer = 13, fraction = 2) BigDecimal originalAmount,
      @NotNull @DecimalMin("0") @Digits(integer = 13, fraction = 2) BigDecimal discountAmount,
      @NotNull LocalDate dueDate,
      @NotBlank @Size(min = 3, max = 500) String reason,
      @NotNull @PositiveOrZero Long version,
      @NotBlank @Size(max = 100) String requestKey) {}
}
