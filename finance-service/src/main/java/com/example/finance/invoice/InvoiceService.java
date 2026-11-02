package com.example.finance.invoice;

import com.example.finance.common.*;
import com.example.finance.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceService {
  private final InvoiceRepository repository;
  private final BranchScope scope;

  public Invoice require(Long id) {
    Invoice i = repository.findById(id).orElseThrow(ApiException::missing);
    scope.finance(i.getBranchId());
    return i;
  }

  public Specification<Invoice> visible(Long requested) {
    Long branch = scope.filter(requested);
    return (r, q, b) ->
        branch == null ? b.conjunction() : b.or(b.isNull(r.get("branchId")), b.equal(r.get("branchId"), branch));
  }

  public PageResponse<InvoiceDto> list(int page, int size, String keyword, Invoice.Status status, Long branchId) {
    return PageResponse.of(
        repository
            .findAll(
                visible(branchId)
                    .and(Query.keyword(keyword, "invoiceNumber"))
                    .and(Query.eq("status", status)),
                Query.page(page, size))
            .map(InvoiceDto::of));
  }

  public InvoiceDto get(Long id) {
    return InvoiceDto.of(require(id));
  }

  @Transactional
  public InvoiceDto save(Long id, InvoiceDto.Request r) {
    Invoice i = id == null ? new Invoice() : require(id);
    Long branch = id == null ? scope.create(r.branchId()) : i.getBranchId();
    i.setStudentId(r.studentId());
    i.setPaymentId(r.paymentId());
    i.setInvoiceNumber(r.invoiceNumber().trim());
    i.setTotalAmount(r.totalAmount());
    i.setStatus(r.status());
    i.setBranchId(branch);
    return InvoiceDto.of(repository.save(i));
  }

  @Transactional
  public InvoiceDto status(Long id, Invoice.Status status) {
    Invoice i = require(id);
    i.setStatus(status);
    return InvoiceDto.of(repository.save(i));
  }
}
