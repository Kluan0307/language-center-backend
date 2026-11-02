package com.example.finance.payment;

import com.example.finance.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class PaymentController {
  private final PaymentService service;

  @GetMapping("/payments")
  public PageResponse<PaymentDto> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Payment.Status status,
      @RequestParam(required = false) Long branchId) {
    return service.list(page, size, keyword, status, branchId);
  }

  @GetMapping("/payments/{id}")
  public PaymentDto get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping("/payments")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF','ACCOUNTANT')")
  public PaymentDto create(@Valid @RequestBody PaymentDto.Request req) {
    return service.save(null, req);
  }

  @PutMapping("/payments/{id}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF','ACCOUNTANT')")
  public PaymentDto update(@PathVariable Long id, @Valid @RequestBody PaymentDto.Request req) {
    return service.save(id, req);
  }

  @PatchMapping("/payments/{id}/status")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF','ACCOUNTANT')")
  public PaymentDto status(@PathVariable Long id, @Valid @RequestBody PaymentDto.StatusRequest req) {
    return service.status(id, req.status());
  }
}
