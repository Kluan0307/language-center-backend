package com.example.finance.tuition;

import com.example.finance.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class TuitionController {
  private final TuitionService service;

  @GetMapping("/tuition")
  public PageResponse<TuitionDto> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Tuition.Status status,
      @RequestParam(required = false) Long branchId) {
    return service.list(page, size, keyword, status, branchId);
  }

  @GetMapping("/tuition/{id}")
  public TuitionDto get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping("/tuition")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF','ACCOUNTANT')")
  public TuitionDto create(@Valid @RequestBody TuitionDto.Request req) {
    return service.save(null, req);
  }

  @PutMapping("/tuition/{id}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF','ACCOUNTANT')")
  public TuitionDto update(@PathVariable Long id, @Valid @RequestBody TuitionDto.Request req) {
    return service.save(id, req);
  }

  @PatchMapping("/tuition/{id}/status")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF','ACCOUNTANT')")
  public TuitionDto status(@PathVariable Long id, @Valid @RequestBody TuitionDto.StatusRequest req) {
    return service.status(id, req.status());
  }
}
