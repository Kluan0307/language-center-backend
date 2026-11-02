package com.example.finance.tuition;

import com.example.finance.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tuition")
@Getter
@Setter
public class Tuition extends BaseEntity {
  @Column(nullable = false)
  private Long studentId;

  @Column(nullable = false)
  private Long classId;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 10)
  private String currency = "VND";

  @Column(nullable = false)
  private LocalDate dueDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.PENDING;

  @Column(nullable = false)
  private Long branchId;

  public enum Status {
    PENDING,
    PARTIAL,
    PAID,
    OVERDUE,
    CANCELLED
  }
}
