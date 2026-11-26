package com.example.academic.course;

import com.example.academic.common.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/academic/courses")
@RequiredArgsConstructor
public class CourseController {
  private final CourseService service;
  private final CourseClassificationService classification;
  @GetMapping("/classification-catalog")
  public CourseClassificationService.Catalog catalog(){return classification.catalog();}

  @GetMapping
  public PageResponse<CourseDto> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Course.Status status,
      @RequestParam(required = false) String level,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) java.math.BigDecimal minTuition,
      @RequestParam(required = false) java.math.BigDecimal maxTuition,
      @RequestParam(defaultValue = "true") boolean maxInclusive,
      @RequestParam(defaultValue = "false") boolean allAssigned,@RequestParam(required=false) Long programId,@RequestParam(required=false) Long audienceId) {
    return service.list(page, size, keyword, status, level, type, branchId,minTuition,maxTuition,maxInclusive,allAssigned,programId,audienceId);
  }

  @GetMapping("/{id}")
  public CourseDto get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
  public CourseDto create(@Valid @RequestBody CourseDto.Request r) {
    return classification.save(null, r);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
  public CourseDto update(@PathVariable Long id, @Valid @RequestBody CourseDto.Request r) {
    return classification.save(id, r);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
  public CourseDto status(@PathVariable Long id, @Valid @RequestBody CourseDto.StatusRequest r) {
    return service.status(id, r.status());
  }
}
