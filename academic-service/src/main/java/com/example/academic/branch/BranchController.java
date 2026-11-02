package com.example.academic.branch;

import com.example.academic.common.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/academic/branches")
@RequiredArgsConstructor
public class BranchController {
  private final BranchService service;

  @GetMapping
  public PageResponse<BranchDto> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long branchId) {
    return service.list(page, size, keyword, branchId);
  }

  @GetMapping("/{id}")
  public BranchDto get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public BranchDto create(@Valid @RequestBody BranchDto.Request r) {
    return service.save(null, r);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public BranchDto update(@PathVariable Long id, @Valid @RequestBody BranchDto.Request r) {
    return service.save(id, r);
  }

  @PatchMapping("/{id}/toggle-status")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public BranchDto toggle(@PathVariable Long id) {
    return service.toggle(id);
  }
}
