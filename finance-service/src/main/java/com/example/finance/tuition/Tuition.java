package com.example.finance.tuition;

import com.example.finance.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@Table(
    name = "tuition",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_tuition_enrollment", columnNames = "enrollment_id"),
    indexes = {
      @Index(name = "idx_tuition_branch_status", columnList = "branch_id,status"),
      @Index(name = "idx_tuition_branch_due", columnList = "branch_id,due_date")
    })
@Getter
@Setter
public class Tuition extends BaseEntity {
  @Version private long version;
  private Long enrollmentId;

  @Column(nullable = false)
  private Long studentId;

  private String studentName;

  @Column(nullable = false)
  private Long classId;

  private String className;
  private Long courseId;
  private String courseName;

  @Column(precision = 15, scale = 2)
  private BigDecimal originalAmount;

  @Column(precision = 15, scale = 2)
  private BigDecimal discountAmount = BigDecimal.ZERO;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  @Column(precision = 15, scale = 2)
  private BigDecimal paidAmount = BigDecimal.ZERO;

  @Column(nullable = false, length = 10)
  private String currency = "VND";

  @Column(nullable = false)
  private LocalDate dueDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.UNPAID;

  @Column(nullable = false)
  private Long branchId;

  private Long adjustedBy;
  private String adjustedByName;
  private LocalDateTime adjustedAt;

  @Column(length = 500)
  private String adjustmentReason;

  public enum Status {
    UNPAID,
    PARTIAL,
    PAID,
    PENDING,
    OVERDUE,
    CANCELLED
  }

  public BigDecimal paid() {
    return paidAmount == null ? BigDecimal.ZERO : paidAmount;
  }

  public BigDecimal balance() {
    return amount.subtract(paid()).max(BigDecimal.ZERO);
  }

  public BigDecimal credit() {
    return paid().subtract(amount).max(BigDecimal.ZERO);
  }

  public Status paymentStatus() {
    if (status == Status.CANCELLED) return status;
    return balance().signum() == 0
        ? Status.PAID
        : paid().signum() > 0 ? Status.PARTIAL : Status.UNPAID;
  }

  public boolean overdue() {
    return status != Status.CANCELLED
        && dueDate.isBefore(LocalDate.now(com.example.finance.common.BusinessTime.ZONE))
        && balance().signum() > 0;
  }
}
