package com.example.finance.payment;

import com.example.finance.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment extends BaseEntity {
  @Column(nullable = false)
  private Long tuitionId;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentMethod paymentMethod = PaymentMethod.BANK_TRANSFER;

  @Column(length = 100)
  private String referenceCode;

  @Column(nullable = false)
  private LocalDateTime paidAt = LocalDateTime.now();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.PENDING;

  @Column(nullable = false)
  private Long branchId;

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
