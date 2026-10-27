package com.example.academic.stats;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/academic/stats")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
public class StatsController {
  private final StatsService service;

  @GetMapping("/overview")
  public StatsDto.Response overview(
      @RequestParam(required = false) Long branchId,
      @RequestParam(defaultValue = "false") boolean groupByBranch) {
    return service.overview(branchId, groupByBranch);
  }
}
