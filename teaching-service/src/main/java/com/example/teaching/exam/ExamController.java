package com.example.teaching.exam;

import com.example.teaching.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teaching/exams")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','TEACHER')")
public class ExamController {
  private final ExamService service;
  private final ExamRosterService rosters;

  @GetMapping("/{id}/roster")
  public ExamRosterService.Roster roster(@PathVariable Long id) {
    return rosters.get(service.require(id));
  }

  @GetMapping
  public PageResponse<ExamDto> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) Long classId) {
    return service.list(page, size, branchId, classId);
  }

  @GetMapping("/{id}")
  public ExamDto get(@PathVariable Long id) {
    return ExamDto.of(service.require(id));
  }

  @PostMapping
  public ExamDto create(@Valid @RequestBody ExamDto.Request r) {
    return service.save(null, r);
  }

  @PutMapping("/{id}")
  public ExamDto update(@PathVariable Long id, @Valid @RequestBody ExamDto.Request r) {
    return service.save(id, r);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}
