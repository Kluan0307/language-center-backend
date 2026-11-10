package com.example.finance.payment;

import com.example.finance.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "payments",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_payment_receipt", columnNames = "receipt_no"),
      @UniqueConstraint(
          name = "uk_payment_request",
          columnNames = {"branch_id", "request_key"})
    },
    indexes = {
      @Index(name = "idx_payment_tuition", columnList = "tuition_id"),
      @Index(name = "idx_payment_branch_date", columnList = "branch_id,paid_at")
    })
@Getter
@Setter
public class Payment extends BaseEntity {
  @Column(nullable = false, updatable = false)
  private Long tuitionId;

  @Column(nullable = false, precision = 15, scale = 2, updatable = false)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private PaymentMethod paymentMethod;

  @Column(length = 100, updatable = false)
  private String referenceCode;

  @Column(updatable = false)
  private String receiptNo;

  @Column(length = 100, updatable = false)
  private String requestKey;

  @Column(nullable = false, updatable = false)
  private LocalDateTime paidAt = LocalDateTime.now(com.example.finance.common.BusinessTime.ZONE);

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private Status status = Status.PAID;

  @Column(nullable = false, updatable = false)
  private Long branchId;

  @Column(updatable = false)
  private Long recordedBy;

  @Column(updatable = false)
  private String recordedByName;

  @Column(length = 500, updatable = false)
  private String note;

  public enum PaymentMethod {
    CASH,
    BANK_TRANSFER,
    CARD,
    WALLET
  }

  public enum Status {
    PENDING,
    PAID,
    FAILED,
    REFUNDED,
    PARTIAL
  }
}
