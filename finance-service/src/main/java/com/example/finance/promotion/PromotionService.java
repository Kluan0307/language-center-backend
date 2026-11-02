package com.example.finance.promotion;

import com.example.finance.common.*;
import com.example.finance.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromotionService {
  private final PromotionRepository repository;
  private final BranchScope scope;

  public Promotion require(Long id) {
    Promotion p = repository.findById(id).orElseThrow(ApiException::missing);
    scope.finance(p.getBranchId());
    return p;
  }

  public Specification<Promotion> visible(Long requested) {
    Long branch = scope.filter(requested);
    return (r, q, b) ->
        branch == null ? b.conjunction() : b.or(b.isNull(r.get("branchId")), b.equal(r.get("branchId"), branch));
  }

  public PageResponse<PromotionDto> list(int page, int size, String keyword, Promotion.Status status, Long branchId) {
    return PageResponse.of(
        repository
            .findAll(
                visible(branchId)
                    .and(Query.keyword(keyword, "code", "name"))
                    .and(Query.eq("status", status)),
                Query.page(page, size))
            .map(PromotionDto::of));
  }

  public PromotionDto get(Long id) {
    return PromotionDto.of(require(id));
  }

  @Transactional
  public PromotionDto save(Long id, PromotionDto.Request r) {
    Promotion p = id == null ? new Promotion() : require(id);
    Long branch = id == null ? scope.create(r.branchId()) : p.getBranchId();
    p.setCode(r.code().trim());
    p.setName(r.name().trim());
    p.setDiscountType(r.discountType());
    p.setDiscountValue(r.discountValue());
    p.setStartDate(r.startDate());
    p.setEndDate(r.endDate());
    p.setBranchId(branch);
    p.setStatus(r.status() == null ? Promotion.Status.ACTIVE : r.status());
    return PromotionDto.of(repository.save(p));
  }

  @Transactional
  public PromotionDto status(Long id, Promotion.Status status) {
    Promotion p = require(id);
    p.setStatus(status);
    return PromotionDto.of(repository.save(p));
  }
}
