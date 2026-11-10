package com.example.academic.results;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class LearningProfileController {
  private final LearningProfileService service;

  @GetMapping("/api/v1/academic/students/{id}/learning-profile")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
  public LearningProfileService.Profile get(@PathVariable Long id) {
    return service.get(id);
  }
}
