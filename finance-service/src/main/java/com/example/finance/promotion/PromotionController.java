package com.example.finance.promotion;

import com.example.finance.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class PromotionController {
  private final PromotionService service;

  @GetMapping("/promotions")
  public PageResponse<PromotionDto> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Promotion.Status status,
      @RequestParam(required = false) Long branchId) {
    return service.list(page, size, keyword, status, branchId);
  }

  @GetMapping("/promotions/{id}")
  public PromotionDto get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping("/promotions")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public PromotionDto create(@Valid @RequestBody PromotionDto.Request req) {
    return service.save(null, req);
  }

  @PutMapping("/promotions/{id}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public PromotionDto update(@PathVariable Long id, @Valid @RequestBody PromotionDto.Request req) {
    return service.save(id, req);
  }

  @PatchMapping("/promotions/{id}/status")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public PromotionDto status(@PathVariable Long id, @Valid @RequestBody PromotionDto.StatusRequest req) {
    return service.status(id, req.status());
  }
}
