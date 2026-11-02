package com.example.finance.payment;

import com.example.finance.common.*;
import com.example.finance.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {
  private final PaymentRepository repository;
  private final BranchScope scope;

  public Payment require(Long id) {
    Payment p = repository.findById(id).orElseThrow(ApiException::missing);
    scope.finance(p.getBranchId());
    return p;
  }

  public Specification<Payment> visible(Long requested) {
    Long branch = scope.filter(requested);
    return (r, q, b) ->
        branch == null ? b.conjunction() : b.or(b.isNull(r.get("branchId")), b.equal(r.get("branchId"), branch));
  }

  public PageResponse<PaymentDto> list(int page, int size, String keyword, Payment.Status status, Long branchId) {
    return PageResponse.of(
        repository
            .findAll(
                visible(branchId)
                    .and(Query.keyword(keyword, "referenceCode"))
                    .and(Query.eq("status", status)),
                Query.page(page, size))
            .map(PaymentDto::of));
  }

  public PaymentDto get(Long id) {
    return PaymentDto.of(require(id));
  }

  @Transactional
  public PaymentDto save(Long id, PaymentDto.Request r) {
    Payment p = id == null ? new Payment() : require(id);
    Long branch = id == null ? scope.create(r.branchId()) : p.getBranchId();
    p.setTuitionId(r.tuitionId());
    p.setAmount(r.amount());
    p.setPaymentMethod(r.paymentMethod());
    p.setReferenceCode(r.referenceCode());
    p.setPaidAt(r.paidAt() == null ? java.time.LocalDateTime.now() : r.paidAt());
    p.setStatus(r.status());
    p.setBranchId(branch);
    return PaymentDto.of(repository.save(p));
  }

  @Transactional
  public PaymentDto status(Long id, Payment.Status status) {
    Payment p = require(id);
    p.setStatus(status);
    return PaymentDto.of(repository.save(p));
  }
}
