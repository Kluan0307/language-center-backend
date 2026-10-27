package com.example.academic.student;

import com.example.academic.common.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/academic/students")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
public class StudentController {
  private final StudentService service;

  @GetMapping
  public PageResponse<StudentDto> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String level,
      @RequestParam(required = false) Student.Status status,
      @RequestParam(required = false) Long branchId) {
    return service.list(page, size, keyword, level, status, branchId);
  }

  @GetMapping("/{id}")
  public StudentDto.Detail get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  public StudentDto create(@Valid @RequestBody StudentDto.Request r) {
    return service.save(null, r);
  }

  @PutMapping("/{id}")
  public StudentDto update(@PathVariable Long id, @Valid @RequestBody StudentDto.Request r) {
    return service.save(id, r);
  }

  @PatchMapping("/{id}/status")
  public StudentDto status(@PathVariable Long id, @Valid @RequestBody StudentDto.StatusRequest r) {
    return service.status(id, r.status());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}
