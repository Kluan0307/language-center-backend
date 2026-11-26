package com.example.auth.company;

import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/company")
public class CompanyController {
  private final CompanyService service;

  @GetMapping
  public Object organization(Principal p) {
    return service.organization(p.getName());
  }

  @GetMapping("/directory")
  public Object directory(
      Principal p,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) Long unitId,
      @RequestParam(required = false) Long titleId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.directory(p.getName(), keyword, branchId, unitId, titleId, page, size);
  }

  @PostMapping("/units")
  public Object unit(@Valid @RequestBody CompanyService.UnitRequest r, Principal p) {
    return service.unit(null, r, p.getName());
  }

  @PutMapping("/units/{id}")
  public Object unit(
      @PathVariable Long id, @Valid @RequestBody CompanyService.UnitRequest r, Principal p) {
    return service.unit(id, r, p.getName());
  }

  @PostMapping("/titles")
  public Object title(@Valid @RequestBody CompanyService.TitleRequest r, Principal p) {
    return service.title(null, r, p.getName());
  }

  @PutMapping("/titles/{id}")
  public Object title(
      @PathVariable Long id, @Valid @RequestBody CompanyService.TitleRequest r, Principal p) {
    return service.title(id, r, p.getName());
  }

  @PostMapping("/assignments")
  public Object assign(@Valid @RequestBody CompanyService.AssignmentRequest r, Principal p) {
    return service.assign(null, r, p.getName());
  }

  @PutMapping("/assignments/{id}")
  public Object assign(
      @PathVariable Long id, @Valid @RequestBody CompanyService.AssignmentRequest r, Principal p) {
    return service.assign(id, r, p.getName());
  }
}
