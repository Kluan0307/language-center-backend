package com.example.finance.expense;

import com.example.finance.common.*;
import com.example.finance.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseService {
  private final ExpenseRepository repository;
  private final BranchScope scope;

  public Expense require(Long id) {
    Expense e = repository.findById(id).orElseThrow(ApiException::missing);
    scope.finance(e.getBranchId());
    return e;
  }

  public Specification<Expense> visible(Long requested) {
    Long branch = scope.filter(requested);
    return (r, q, b) ->
        branch == null ? b.conjunction() : b.or(b.isNull(r.get("branchId")), b.equal(r.get("branchId"), branch));
  }

  public PageResponse<ExpenseDto> list(int page, int size, String keyword, Expense.Category category, Expense.Status status, Long branchId) {
    return PageResponse.of(
        repository
            .findAll(
                visible(branchId)
                    .and(Query.keyword(keyword, "title", "description"))
                    .and(Query.eq("category", category))
                    .and(Query.eq("status", status)),
                Query.page(page, size))
            .map(ExpenseDto::of));
  }

  public ExpenseDto get(Long id) {
    return ExpenseDto.of(require(id));
  }

  @Transactional
  public ExpenseDto save(Long id, ExpenseDto.Request r) {
    Expense e = id == null ? new Expense() : require(id);
    Long branch = id == null ? scope.create(r.branchId()) : e.getBranchId();
    e.setCategory(r.category());
    e.setTitle(r.title().trim());
    e.setAmount(r.amount());
    e.setCurrency(r.currency().trim().toUpperCase());
    e.setBranchId(branch);
    e.setDescription(r.description());
    e.setStatus(r.status() == null ? Expense.Status.PENDING : r.status());
    return ExpenseDto.of(repository.save(e));
  }

  @Transactional
  public ExpenseDto status(Long id, Expense.Status status) {
    Expense e = require(id);
    e.setStatus(status);
    return ExpenseDto.of(repository.save(e));
  }
}
