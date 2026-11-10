package com.example.academic.results;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/academic/classes/{id}")
public class ClassClosureController {
  private final ClassClosureService service;

  @GetMapping("/closure")
  public ClassClosureService.View get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping("/close")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public ClassClosureService.View close(
      @PathVariable Long id, @Valid @RequestBody ClassClosureService.Request r) {
    return service.change(id, true, r);
  }

  @PostMapping("/reopen")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public ClassClosureService.View reopen(
      @PathVariable Long id, @Valid @RequestBody ClassClosureService.Request r) {
    return service.change(id, false, r);
  }
}
