package com.example.academic.placement;

import com.example.academic.common.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/academic")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
public class PlacementController {
  private final PlacementService service;

  @PostMapping("/enrollments/preview")
  public PlacementDto.Preview preview(@Valid @RequestBody PlacementDto.Context r) {
    return service.preview(r);
  }

  @GetMapping("/enrollments/suggestions")
  public PageResponse<PlacementDto.Preview> suggestions(
      @RequestParam Long studentId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.suggestions(studentId, page, size);
  }

  @GetMapping("/enrollments/{id}/admission-history")
  public List<EnrollmentAdmission> history(@PathVariable Long id) {
    return service.history(id);
  }

  @PostMapping("/placement-exceptions")
  public PlacementDto.ExceptionView request(@Valid @RequestBody PlacementDto.ExceptionRequest r) {
    return service.request(r);
  }

  @GetMapping("/placement-exceptions")
  public PageResponse<PlacementDto.ExceptionView> list(
      @RequestParam(required = false) Long studentId,
      @RequestParam(required = false) Long classId,
      @RequestParam(required = false) Long branchId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.list(studentId, classId, branchId, page, size);
  }

  @GetMapping("/placement-exceptions/{id}")
  public PlacementDto.ExceptionView get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping("/placement-exceptions/{id}/decision")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public PlacementDto.ExceptionView decide(
      @PathVariable Long id, @Valid @RequestBody PlacementDto.Decision r) {
    return service.decide(id, r);
  }
}
