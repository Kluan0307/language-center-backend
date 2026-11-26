package com.example.teaching.assistant;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teaching/assistant-query")
public class TeachingReadController {
  private final TeachingReadService service;

  @GetMapping("/current-grades")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','TEACHER')")
  public ReadQuery.Result current(
      @RequestParam(defaultValue = "ACTIVE_BRANCH") ReadQuery.Mode scopeMode,
      @RequestParam(required = false) Long branchId,
      @RequestParam Long examId,
      @RequestParam(required = false) Long studentId,
      @RequestParam(defaultValue = "ALL") String scoreFilter,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size) {
    return service.current(scopeMode, branchId, examId, studentId, scoreFilter, page, size);
  }

  @GetMapping("/exams")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','TEACHER')")
  public ReadQuery.Result exams(
      @RequestParam(defaultValue = "ACTIVE_BRANCH") ReadQuery.Mode scopeMode,
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) Long classId,
      @RequestParam(required = false) Long examId,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "false") boolean missingOnly,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size) {
    return service.examList(scopeMode, branchId, classId, examId, keyword, missingOnly, page, size);
  }

  @GetMapping("/published-grades")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
  public ReadQuery.Result published(
      @RequestParam(defaultValue = "ACTIVE_BRANCH") ReadQuery.Mode scopeMode,
      @RequestParam(required = false) Long branchId,
      @RequestParam Long studentId,
      @RequestParam Long enrollmentId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size) {
    return service.published(scopeMode, branchId, studentId, enrollmentId, page, size);
  }
}
