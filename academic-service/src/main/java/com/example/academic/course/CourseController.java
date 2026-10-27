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

  @GetMapping
  public PageResponse<CourseDto> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Course.Status status,
      @RequestParam(required = false) String level,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) Long branchId) {
    return service.list(page, size, keyword, status, level, type, branchId);
  }

  @GetMapping("/{id}")
  public CourseDto get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
  public CourseDto create(@Valid @RequestBody CourseDto.Request r) {
    return service.save(null, r);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
  public CourseDto update(@PathVariable Long id, @Valid @RequestBody CourseDto.Request r) {
    return service.save(id, r);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
  public CourseDto status(@PathVariable Long id, @Valid @RequestBody CourseDto.StatusRequest r) {
    return service.status(id, r.status());
  }
}
