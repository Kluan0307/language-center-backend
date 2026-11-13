package com.example.finance.payment;

import com.example.finance.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/finance/payments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
public class PaymentController {
  private final PaymentService service;

  @GetMapping
  public PageResponse<PaymentDto> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) Long tuitionId,
      @RequestParam(required = false) Long studentId,
      @RequestParam(required = false) Long classId,
      @RequestParam(defaultValue = "false") boolean allAssigned) {
    return service.search(page, size, keyword, branchId, tuitionId, studentId, classId,allAssigned);
  }

  @GetMapping("/{id}")
  public PaymentDto get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  public PaymentDto create(@Valid @RequestBody PaymentDto.Request r) {
    return service.create(r);
  }
}
