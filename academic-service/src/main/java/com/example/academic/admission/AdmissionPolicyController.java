package com.example.academic.admission;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/academic")
public class AdmissionPolicyController {
  private final AdmissionPolicyService service;

  @GetMapping("/courses/{id}/admission")
  public AdmissionDto.CourseView course(@PathVariable Long id) {
    return service.getCourse(id);
  }

  @PutMapping("/courses/{id}/admission")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public AdmissionDto.CourseView confirm(
      @PathVariable Long id, @Valid @RequestBody AdmissionDto.Confirm r) {
    return service.confirm(id, r);
  }

  @GetMapping("/courses/{id}/admission/history")
  public List<AdmissionDto.Policy> history(@PathVariable Long id) {
    return service.history(id);
  }

  @GetMapping("/classes/{id}/admission")
  public AdmissionDto.ClassView schoolClass(@PathVariable Long id) {
    return service.getClass(id);
  }

  @PutMapping("/classes/{id}/admission")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public AdmissionDto.ClassView apply(
      @PathVariable Long id, @Valid @RequestBody AdmissionDto.Apply r) {
    return service.apply(id, r);
  }

  @GetMapping("/classes/{id}/admission/history")
  public List<AdmissionDto.Change> classHistory(@PathVariable Long id) {
    return service.classHistory(id);
  }
}
