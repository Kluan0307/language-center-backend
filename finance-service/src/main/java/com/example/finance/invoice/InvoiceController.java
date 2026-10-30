package com.example.finance.invoice;

import com.example.finance.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class InvoiceController {
  private final InvoiceService service;

  @GetMapping("/invoices")
  public PageResponse<InvoiceDto> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Invoice.Status status,
      @RequestParam(required = false) Long branchId) {
    return service.list(page, size, keyword, status, branchId);
  }

  @GetMapping("/invoices/{id}")
  public InvoiceDto get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping("/invoices")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF','ACCOUNTANT')")
  public InvoiceDto create(@Valid @RequestBody InvoiceDto.Request req) {
    return service.save(null, req);
  }

  @PatchMapping("/invoices/{id}/status")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF','ACCOUNTANT')")
  public InvoiceDto status(@PathVariable Long id, @Valid @RequestBody InvoiceDto.StatusRequest req) {
    return service.status(id, req.status());
  }
}
