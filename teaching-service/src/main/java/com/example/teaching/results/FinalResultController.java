package com.example.teaching.results;

import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teaching/classes/{classId}/final-results")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF','TEACHER')")
public class FinalResultController {
  private final FinalResultService service;

  @GetMapping
  public FinalResultDto.ClassResults list(@PathVariable Long classId) {
    return service.classResults(classId);
  }

  @GetMapping("/requests/{requestId}")
  public FinalResultDto.Summary request(
      @PathVariable Long classId, @PathVariable String requestId) {
    return service.byRequest(classId, requestId);
  }

  @GetMapping("/{enrollmentId}/history")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','TEACHER')")
  public List<FinalResult> history(@PathVariable Long classId, @PathVariable Long enrollmentId) {
    return service.history(classId, enrollmentId);
  }

  @PostMapping("/{enrollmentId}/preview")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public FinalResultDto.Preview preview(
      @PathVariable Long classId, @PathVariable Long enrollmentId) {
    return service.preview(classId, enrollmentId);
  }

  @PostMapping("/{enrollmentId}/confirm")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public FinalResultDto.Summary confirm(
      @PathVariable Long classId,
      @PathVariable Long enrollmentId,
      @Valid @RequestBody FinalResultDto.Confirm r) {
    return service.confirm(classId, enrollmentId, r);
  }

  @PostMapping("/reopen")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public FinalResultDto.ClassResults reopen(
      @PathVariable Long classId, @Valid @RequestBody FinalResultDto.Reopen r) {
    return service.reopen(classId, r);
  }
}
