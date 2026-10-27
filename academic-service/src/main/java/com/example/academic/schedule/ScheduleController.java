package com.example.academic.schedule;

import com.example.academic.common.*;
import com.example.academic.security.Users;
import jakarta.validation.Valid;
import java.time.DayOfWeek;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/academic/schedules")
@RequiredArgsConstructor
public class ScheduleController {
  private final ScheduleService service;

  @GetMapping
  public PageResponse<ScheduleDto> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) Long classId,
      @RequestParam(required = false) DayOfWeek dayOfWeek,
      @RequestParam(required = false) Long teacherId,
      @RequestParam(required = false) Long roomId,
      @RequestParam(required = false) Long branchId) {
    return service.list(page, size, classId, dayOfWeek, teacherId, roomId, branchId);
  }

  @GetMapping("/my")
  @PreAuthorize("hasRole('TEACHER')")
  public PageResponse<ScheduleDto> mine(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.list(page, size, null, null, Users.current().profileId(), null, null);
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public ScheduleDto create(@Valid @RequestBody ScheduleDto.Request r) {
    return service.save(null, r);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public ScheduleDto update(@PathVariable Long id, @Valid @RequestBody ScheduleDto.Request r) {
    return service.save(id, r);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }

  @PostMapping("/check-conflict")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public List<ScheduleDto.Conflict> preview(@Valid @RequestBody ScheduleDto.ConflictRequest r) {
    return service.preview(r);
  }
}
