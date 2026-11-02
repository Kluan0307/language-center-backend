package com.example.finance.invoice;

import com.example.finance.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "invoices")
@Getter
@Setter
public class Invoice extends BaseEntity {
  @Column(nullable = false)
  private Long studentId;

  @Column(nullable = false)
  private Long paymentId;

  @Column(nullable = false, length = 100)
  private String invoiceNumber;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal totalAmount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.PAID;

  @Column(nullable = false)
  private Long branchId;

  public enum Status {
    PAID,
    PENDING,
    CANCELLED
  }
}
