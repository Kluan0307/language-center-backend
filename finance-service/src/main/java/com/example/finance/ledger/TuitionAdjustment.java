package com.example.finance.ledger;

import com.example.finance.common.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@Table(
    name = "tuition_adjustments",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_adjustment_request",
            columnNames = {"branch_id", "request_key"}),
    indexes = @Index(name = "idx_adjustment_tuition", columnList = "tuition_id,id"))
@Getter
@Setter
public class TuitionAdjustment extends BaseEntity {
  @Column(nullable = false, updatable = false)
  private Long tuitionId;

  @Column(nullable = false, updatable = false)
  private Long branchId;

  @Column(precision = 15, scale = 2, nullable = false, updatable = false)
  private BigDecimal oldOriginal;

  @Column(precision = 15, scale = 2, nullable = false, updatable = false)
  private BigDecimal oldDiscount;

  @Column(precision = 15, scale = 2, nullable = false, updatable = false)
  private BigDecimal oldAmount;

  @Column(nullable = false, updatable = false)
  private LocalDate oldDueDate;

  @Column(precision = 15, scale = 2, nullable = false, updatable = false)
  private BigDecimal newOriginal;

  @Column(precision = 15, scale = 2, nullable = false, updatable = false)
  private BigDecimal newDiscount;

  @Column(precision = 15, scale = 2, nullable = false, updatable = false)
  private BigDecimal newAmount;

  @Column(nullable = false, updatable = false)
  private LocalDate newDueDate;

  @Column(precision = 15, scale = 2, nullable = false, updatable = false)
  private BigDecimal paidAtChange;

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
