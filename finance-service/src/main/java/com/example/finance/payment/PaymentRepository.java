package com.example.finance.payment;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository
    extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {
  @Query(
      "select p.tuitionId,sum(p.amount) from Payment p where p.tuitionId in :ids and"
          + " p.status=com.example.finance.payment.Payment.Status.PAID group by p.tuitionId")
  java.util.List<Object[]> receiptTotals(@Param("ids") java.util.Collection<Long> ids);

  @Query(
      "select p.tuitionId,count(p) from Payment p where p.tuitionId in :ids and"
          + " p.status<>com.example.finance.payment.Payment.Status.PAID group by p.tuitionId")
  java.util.List<Object[]> legacyCounts(@Param("ids") java.util.Collection<Long> ids);

  boolean existsByTuitionIdAndStatusNot(Long tuitionId, Payment.Status status);

  Optional<Payment> findByBranchIdAndRequestKey(Long branchId, String requestKey);

  @Query(
      "select coalesce(sum(p.amount),0) from Payment p where p.tuitionId=:id and"
          + " p.status=com.example.finance.payment.Payment.Status.PAID")
  BigDecimal collected(@Param("id") Long tuitionId);
}
