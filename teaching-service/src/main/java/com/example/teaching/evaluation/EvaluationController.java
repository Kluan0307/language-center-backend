package com.example.teaching.evaluation;

import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teaching/classes/{classId}")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','TEACHER')")
public class EvaluationController {
  private final EvaluationPolicyService policies;
  private final EvaluationService evaluation;

  @GetMapping("/evaluation-policy")
  public EvaluationDto.PolicyView get(@PathVariable Long classId) {
    return policies.get(classId);
  }

  @PutMapping("/evaluation-policy")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public EvaluationDto.PolicyView save(
      @PathVariable Long classId, @Valid @RequestBody EvaluationDto.Confirm r) {
    return policies.confirm(classId, r);
  }

  @GetMapping("/evaluation-policy/history")
  public List<EvaluationPolicy> history(@PathVariable Long classId) {
    return policies.history(classId);
  }

  @GetMapping("/evaluation-preview")
  public EvaluationDto.Report preview(@PathVariable Long classId) {
    return evaluation.get(classId);
  }
}
