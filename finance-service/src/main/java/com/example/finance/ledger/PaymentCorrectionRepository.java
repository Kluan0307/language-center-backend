package com.example.finance.ledger;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PaymentCorrectionRepository extends JpaRepository<PaymentCorrection, Long> {
  @Query(
      "select c.tuitionId,sum(c.amount) from PaymentCorrection c where c.tuitionId in :ids group by"
          + " c.tuitionId")
  List<Object[]> tuitionTotals(@Param("ids") Collection<Long> ids);

  Optional<PaymentCorrection> findByBranchIdAndRequestKey(Long branch, String key);

  List<PaymentCorrection> findByPaymentIdOrderByIdDesc(Long id);

  List<PaymentCorrection> findByTuitionIdOrderByIdDesc(Long id);

  @Query("select coalesce(sum(c.amount),0) from PaymentCorrection c where c.tuitionId=:id")
  BigDecimal total(@Param("id") Long id);

  @Query(
      "select c.paymentId,c.type,sum(c.amount) from PaymentCorrection c where c.paymentId in :ids"
          + " group by c.paymentId,c.type")
  List<Object[]> totals(@Param("ids") Collection<Long> ids);
}
