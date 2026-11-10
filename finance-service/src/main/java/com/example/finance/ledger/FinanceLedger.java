package com.example.finance.ledger;

import com.example.finance.common.*;
import com.example.finance.payment.*;
import com.example.finance.tuition.*;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FinanceLedger {
  private final jakarta.persistence.EntityManager em;
  private final PaymentRepository payments;
  private final PaymentCorrectionRepository corrections;

  public BigDecimal net(Long tuition) {
    return payments.collected(tuition).subtract(corrections.total(tuition));
  }

  public void setNet(Tuition t, BigDecimal value) {
    t.setPaidAmount(value);
    t.setStatus(t.paymentStatus());
    em.flush();
  }

  public void requireSynced(Tuition t) {
    if (payments.existsByTuitionIdAndStatusNot(t.getId(), Payment.Status.PAID))
      throw ApiException.conflict("Có phiếu cũ chưa xác minh; cần đối soát trước khi thao tác");
    if (net(t.getId()).compareTo(t.paid()) != 0)
      throw ApiException.conflict(
          "Sổ thu không khớp số dư học phí; cần đối soát trước khi thao tác");
  }

  public static String reason(String v) {
    if (v == null || v.trim().length() < 3 || v.trim().length() > 500)
      throw ApiException.bad("Nhập lý do từ 3 đến 500 ký tự");
    return v.trim();
  }

  public record Totals(BigDecimal voided, BigDecimal refunded) {
    public BigDecimal total() {
      return voided.add(refunded);
    }
  }

  public Map<Long, Totals> totals(Collection<Long> ids) {
    var result = new HashMap<Long, Totals>();
    if (ids.isEmpty()) return result;
    for (var row : corrections.totals(ids)) {
      var id = (Long) row[0];
      var old = result.getOrDefault(id, new Totals(BigDecimal.ZERO, BigDecimal.ZERO));
      result.put(
          id,
          row[1] == PaymentCorrection.Type.VOID
              ? new Totals((BigDecimal) row[2], old.refunded())
              : new Totals(old.voided(), (BigDecimal) row[2]));
    }
    return result;
  }

  public PaymentDto dto(Payment p, Map<Long, Totals> totals) {
    var t = totals.getOrDefault(p.getId(), new Totals(BigDecimal.ZERO, BigDecimal.ZERO));
    return PaymentDto.of(p, t.voided(), t.refunded());
  }

  public PaymentDto dto(Payment p) {
    return dto(p, totals(java.util.Collections.singletonList(p.getId())));
  }
}
