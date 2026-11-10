package com.example.finance.ledger;

import com.example.finance.common.*;
import com.example.finance.payment.Payment;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@Table(
    name = "payment_corrections",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_correction_request",
            columnNames = {"branch_id", "request_key"}),
    indexes = {
      @Index(name = "idx_correction_payment", columnList = "payment_id,id"),
      @Index(name = "idx_correction_tuition", columnList = "tuition_id,id"),
      @Index(name = "idx_correction_branch_date", columnList = "branch_id,created_at")
    })
@Getter
@Setter
public class PaymentCorrection extends BaseEntity {
  public enum Type {
    VOID,
    REFUND
  }

  @Column(nullable = false, updatable = false)
  private Long paymentId;

  @Column(nullable = false, updatable = false)
  private Long tuitionId;

  @Column(nullable = false, updatable = false)
  private Long branchId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private Type type;

  @Column(nullable = false, precision = 15, scale = 2, updatable = false)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(updatable = false)
  private Payment.PaymentMethod paymentMethod;

  @Column(length = 100, updatable = false)
  private String referenceCode;

  @Column(nullable = false, length = 500, updatable = false)
  private String reason;

  @Column(nullable = false, length = 100, updatable = false)
  private String requestKey;

  @Column(nullable = false, updatable = false)
  private Long expectedVersion;

  @Column(nullable = false, updatable = false)
  private Long recordedBy;

  @Column(nullable = false, updatable = false)
  private String recordedByName;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now(BusinessTime.ZONE);
}
