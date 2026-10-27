package com.example.academic.attendance;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/academic/attendances")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF','TEACHER')")
public class AttendanceController {
  private final AttendanceService service;

  @GetMapping("/sheet")
  public AttendanceDto.Sheet sheet(
      @RequestParam Long classId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return service.sheet(classId, date);
  }

  @PostMapping("/bulk")
  public AttendanceDto.Sheet bulk(@Valid @RequestBody AttendanceDto.Bulk r) {
    return service.bulk(r);
  }

  @GetMapping("/history")
  public List<AttendanceDto.History> history(
      @RequestParam Long classId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return service.history(classId, from, to);
  }

  @GetMapping("/summary")
  public List<AttendanceDto.Summary> summary(@RequestParam Long classId) {
    return service.summary(classId);
  }
}
