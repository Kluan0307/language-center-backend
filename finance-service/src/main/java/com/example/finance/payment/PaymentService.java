package com.example.finance.payment;

import com.example.finance.common.*;
import com.example.finance.security.*;
import com.example.finance.tuition.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {
  private final PaymentRepository repository;
  private final TuitionService tuitions;
  private final BranchScope scope;
  private final com.example.finance.ledger.FinanceLedger ledger;

  public PageResponse<PaymentDto> list(
      int page, int size, String keyword, Long branchId, Long tuitionId) {
    if (tuitionId != null) tuitions.require(tuitionId);
    var rows =
        repository.findAll(
            Query.<Payment>branch(scope.filter(branchId))
                .and(Query.eq("tuitionId", tuitionId))
                .and(Query.keyword(keyword, "receiptNo", "referenceCode")),
            Query.page(page, size));
    var totals = ledger.totals(rows.getContent().stream().map(Payment::getId).toList());
    return PageResponse.of(rows.map(p -> ledger.dto(p, totals)));
  }

  public PaymentDto get(Long id) {
    Payment p = repository.findById(id).orElseThrow(ApiException::missing);
    scope.check(p.getBranchId());
    return ledger.dto(p);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public PaymentDto create(PaymentDto.Request r) {
    if (r.paymentMethod() != Payment.PaymentMethod.CASH
        && r.paymentMethod() != Payment.PaymentMethod.BANK_TRANSFER)
      throw ApiException.bad("Use CASH or BANK_TRANSFER");
    if (Users.current().role() == Role.TEACHER) throw ApiException.forbidden();
    Tuition t = tuitions.lock(r.tuitionId());
    var prior = repository.findByBranchIdAndRequestKey(t.getBranchId(), r.requestKey());
    if (prior.isPresent()) {
      Payment p = prior.get();
      if (!p.getTuitionId().equals(r.tuitionId())
          || p.getAmount().compareTo(r.amount()) != 0
          || p.getPaymentMethod() != r.paymentMethod()
          || !java.util.Objects.equals(p.getReferenceCode(), r.referenceCode())
          || !java.util.Objects.equals(p.getNote(), r.note()))
        throw ApiException.conflict("requestKey was used for a different payment");
      return PaymentDto.of(p);
    }
    if (t.getStatus() == Tuition.Status.CANCELLED)
      throw ApiException.conflict("Tuition is cancelled");
    ledger.requireSynced(t);
    BigDecimal collected = ledger.net(t.getId());
    if (r.amount().signum() <= 0 || r.amount().compareTo(t.getAmount().subtract(collected)) > 0)
      throw ApiException.conflict("Payment exceeds outstanding balance");
    Payment p = new Payment();
    p.setTuitionId(t.getId());
    p.setBranchId(t.getBranchId());
    p.setAmount(r.amount());
    p.setPaymentMethod(r.paymentMethod());
    p.setReferenceCode(r.referenceCode());
    p.setRequestKey(r.requestKey());
    p.setReceiptNo("PT-" + UUID.randomUUID());
    p.setRecordedBy(Users.current().accountId());
    p.setRecordedByName(Users.current().username());
    p.setNote(r.note());
    repository.saveAndFlush(p);
    ledger.setNet(t, collected.add(r.amount()));
    return PaymentDto.of(p);
  }
}
