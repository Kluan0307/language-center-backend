package com.example.academic.lead;

import com.example.academic.common.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/academic/leads")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
public class LeadController {
  private final LeadService service;

  @GetMapping
  public PageResponse<LeadDto> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Lead.Status status,
      @RequestParam(required = false) Long assignedTo,
      @RequestParam(required = false) Long branchId) {
    return service.list(page, size, keyword, status, assignedTo, branchId);
  }

  @GetMapping("/{id}")
  public LeadDto get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  public LeadDto create(@Valid @RequestBody LeadDto.Request r) {
    return service.save(null, r);
  }

  @PutMapping("/{id}")
  public LeadDto update(@PathVariable Long id, @Valid @RequestBody LeadDto.Request r) {
    return service.save(id, r);
  }

  @PatchMapping("/{id}/status")
  public LeadDto status(@PathVariable Long id, @Valid @RequestBody LeadDto.StatusRequest r) {
    return service.status(id, r);
  }

  @PostMapping("/{id}/convert")
  public LeadDto convert(@PathVariable Long id) {
    return service.convert(id);
  }
}
