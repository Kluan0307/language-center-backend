package com.example.academic.session;

import com.example.academic.common.PageResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/academic/sessions")
@RequiredArgsConstructor
public class SessionController {
  private final SessionPlanner service;
  private final com.example.academic.attendance.AttendanceService attendance;

  @GetMapping("/{id}/attendance-changes")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public List<com.example.academic.attendance.AttendanceDto.Change> attendanceChanges(
      @PathVariable Long id) {
    return attendance.changes(id);
  }

  @GetMapping
  public PageResponse<SessionDto> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) Long classId,
      @RequestParam(required = false) Long teacherId,
      @RequestParam(required = false) ClassSession.Status status,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,@RequestParam(defaultValue="false") boolean allAssigned) {
    return service.list(page, size, branchId, classId, teacherId, status, from, to, allAssigned);
  }

  @GetMapping("/{id}")
  public SessionDto get(@PathVariable Long id) {
    return service.dto(service.require(id, false));
  }

  @GetMapping("/{id}/history")
  public List<SessionDto.Event> history(@PathVariable Long id) {
    return service.history(id);
  }

  @PostMapping("/generate")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public SessionDto.Generated generate(@Valid @RequestBody SessionDto.Generate request) {
    return service.generate(request);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public SessionDto edit(@PathVariable Long id, @Valid @RequestBody SessionDto.Edit request) {
    return service.edit(id, request);
  }

  @PostMapping("/{id}/correct-time") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')") public SessionDto correctTime(@PathVariable Long id,@Valid @RequestBody SessionDto.TimeCorrection r){return service.correctTime(id,r);}
  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public SessionDto cancel(@PathVariable Long id, @Valid @RequestBody SessionDto.Action request) {
    return service.cancel(id, request);
  }

  @PostMapping("/{id}/restore")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public SessionDto restore(@PathVariable Long id, @Valid @RequestBody SessionDto.Action request) {
    return service.restore(id, request);
  }

  @PostMapping("/{id}/makeup")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public SessionDto makeup(@PathVariable Long id, @Valid @RequestBody SessionDto.Edit request) {
    return service.makeup(id, request);
  }
}
