package com.example.finance.expense;

import com.example.finance.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "expenses")
@Getter
@Setter
public class Expense extends BaseEntity {
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Category category = Category.OPERATING_COST;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 10)
  private String currency = "VND";

  @Column(nullable = false)
  private Long branchId;

  @Column(columnDefinition = "text")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.PENDING;

  public enum Category {
    OPERATING_COST,
    TEACHER_SALARY,
    MARKETING,
    UTILITY,
    ASSET
  }

  public enum Status {
    PENDING,
    APPROVED,
    REJECTED,
    PAID
  }
}
