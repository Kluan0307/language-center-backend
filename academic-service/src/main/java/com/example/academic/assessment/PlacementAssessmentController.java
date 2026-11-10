package com.example.academic.assessment;

import com.example.academic.common.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/academic/students/{studentId}/assessments")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
public class PlacementAssessmentController {
  private final PlacementAssessmentService service;

  @GetMapping
  public PageResponse<AssessmentDto> list(
      @PathVariable Long studentId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) Long programId,
      @RequestParam(required = false) PlacementAssessment.Status status) {
    return service.list(studentId, page, size, programId, status);
  }

  @GetMapping("/current")
  public List<AssessmentDto> current(@PathVariable Long studentId) {
    return service.current(studentId);
  }

  @GetMapping("/{id}")
  public AssessmentDto get(@PathVariable Long studentId, @PathVariable Long id) {
    return service.get(studentId, id);
  }

  @PostMapping
  public AssessmentDto create(
      @PathVariable Long studentId, @Valid @RequestBody AssessmentDto.Draft r) {
    return service.save(studentId, null, r);
  }

  @PutMapping("/{id}")
  public AssessmentDto update(
      @PathVariable Long studentId,
      @PathVariable Long id,
      @Valid @RequestBody AssessmentDto.Draft r) {
    return service.save(studentId, id, r);
  }

  @PostMapping("/{id}/confirm")
  public AssessmentDto confirm(
      @PathVariable Long studentId,
      @PathVariable Long id,
      @Valid @RequestBody AssessmentDto.Action r) {
    return service.confirm(studentId, id, r);
  }

  @PostMapping("/{id}/revoke")
  public AssessmentDto revoke(
      @PathVariable Long studentId,
      @PathVariable Long id,
      @Valid @RequestBody AssessmentDto.Action r) {
    return service.revoke(studentId, id, r);
  }

  @GetMapping("/{id}/history")
  public List<AssessmentDto.Event> history(@PathVariable Long studentId, @PathVariable Long id) {
    return service.history(studentId, id);
  }
}
