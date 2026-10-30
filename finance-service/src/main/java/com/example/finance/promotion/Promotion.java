package com.example.finance.promotion;

import com.example.finance.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "promotions")
@Getter
@Setter
public class Promotion extends BaseEntity {
  @Column(nullable = false, length = 100)
  private String code;

  @Column(nullable = false, length = 255)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DiscountType discountType = DiscountType.PERCENT;

  @Column(nullable = false)
  private Integer discountValue;

  @Column(nullable = false)
  private LocalDate startDate;

  @Column(nullable = false)
  private LocalDate endDate;

  @Column(nullable = false)
  private Long branchId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.ACTIVE;

  public enum DiscountType {
    PERCENT,
    FIXED_AMOUNT
  }

  public enum Status {
    ACTIVE,
    INACTIVE,
    EXPIRED
  }
}
