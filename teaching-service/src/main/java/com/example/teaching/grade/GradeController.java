package com.example.teaching.grade;

import com.example.teaching.common.PageResponse;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teaching")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','TEACHER')")
public class GradeController {
  private final GradeService service;

  @GetMapping("/grades")
  public PageResponse<GradeDto> list(
      @RequestParam Long examId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.list(examId, page, size);
  }

  @PutMapping("/grades")
  public GradeDto save(@Valid @RequestBody GradeDto.Request r) {
    return service.save(r);
  }

  @GetMapping("/grades/{id}/history")
  public List<GradeDto.Change> history(@PathVariable Long id) {
    return service.history(id);
  }

  private final com.example.teaching.evaluation.EvaluationService evaluation;
 @GetMapping("/reports/evaluations") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')") public com.example.teaching.evaluation.EvaluationDto.Report evaluations(@RequestParam Long classId){return evaluation.get(classId);}
}
