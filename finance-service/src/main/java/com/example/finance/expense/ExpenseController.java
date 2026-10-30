package com.example.finance.expense;

import com.example.finance.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class ExpenseController {
  private final ExpenseService service;

  @GetMapping("/expenses")
  public PageResponse<ExpenseDto> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Expense.Category category,
      @RequestParam(required = false) Expense.Status status,
      @RequestParam(required = false) Long branchId) {
    return service.list(page, size, keyword, category, status, branchId);
  }

  @GetMapping("/expenses/{id}")
  public ExpenseDto get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping("/expenses")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF','ACCOUNTANT')")
  public ExpenseDto create(@Valid @RequestBody ExpenseDto.Request req) {
    return service.save(null, req);
  }

  @PutMapping("/expenses/{id}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF','ACCOUNTANT')")
  public ExpenseDto update(@PathVariable Long id, @Valid @RequestBody ExpenseDto.Request req) {
    return service.save(id, req);
  }

  @PatchMapping("/expenses/{id}/status")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF','ACCOUNTANT')")
  public ExpenseDto status(@PathVariable Long id, @Valid @RequestBody ExpenseDto.StatusRequest req) {
    return service.status(id, req.status());
  }
}
