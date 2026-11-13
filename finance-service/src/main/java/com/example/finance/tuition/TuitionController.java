package com.example.finance.tuition;

import com.example.finance.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
public class TuitionController {
  private final TuitionService service;

  @GetMapping({"/tuitions", "/tuition"})
  public PageResponse<TuitionDto> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Tuition.Status status,
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) Long classId,
      @RequestParam(required = false) Long studentId,
      @RequestParam(required = false) Boolean overdue,@RequestParam(defaultValue="false") boolean allAssigned) {
    return service.list(page, size, keyword, status, branchId, classId, studentId, overdue, allAssigned);
  }

  @GetMapping({"/tuitions/{id}", "/tuition/{id}"})
  public TuitionDto get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping({"/tuitions", "/tuition"})
  public TuitionDto create(@Valid @RequestBody TuitionDto.Request r) {
    try {
      return service.create(r);
    } catch (org.springframework.dao.DataIntegrityViolationException e) {
      return service.existing(r.enrollmentId());
    }
  }

  @PutMapping("/tuitions/{id}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public TuitionDto adjust(@PathVariable Long id, @Valid @RequestBody TuitionDto.Adjustment r) {
    return service.adjust(id, r);
  }
}
