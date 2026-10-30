package com.example.finance.promotion;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record PromotionDto(
    Long id,
    String code,
    String name,
    Promotion.DiscountType discountType,
    Integer discountValue,
    LocalDate startDate,
    LocalDate endDate,
    Long branchId,
    Promotion.Status status) {
  public static PromotionDto of(Promotion p) {
    return new PromotionDto(
        p.getId(),
        p.getCode(),
        p.getName(),
        p.getDiscountType(),
        p.getDiscountValue(),
        p.getStartDate(),
        p.getEndDate(),
        p.getBranchId(),
        p.getStatus());
  }

  public record Request(
      @NotBlank @Size(max = 100) String code,
      @NotBlank @Size(max = 255) String name,
      @NotNull Promotion.DiscountType discountType,
      @NotNull @Positive Integer discountValue,
      @NotNull LocalDate startDate,
      @NotNull LocalDate endDate,
      Long branchId,
      Promotion.Status status) {}

  public record StatusRequest(@NotNull Promotion.Status status) {}
}
