package com.example.academic.schoolclass;

import com.example.academic.common.*;
import com.example.academic.schedule.ScheduleDto;
import com.example.academic.security.Users;
import com.example.academic.student.StudentDto;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/academic/classes")
@RequiredArgsConstructor
public class SchoolClassController {
  private final SchoolClassService service;

  @GetMapping
  public PageResponse<SchoolClassDto> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) SchoolClass.Status status,
      @RequestParam(required = false) Long courseId,
      @RequestParam(required = false) Long teacherId,
      @RequestParam(required = false) Long branchId) {
    return service.list(page, size, keyword, status, courseId, teacherId, branchId);
  }

  @GetMapping("/my")
  @PreAuthorize("hasRole('TEACHER')")
  public PageResponse<SchoolClassDto> mine(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.list(page, size, null, null, null, Users.current().profileId(), null);
  }

  @GetMapping("/{id}")
  public SchoolClassDto.Detail get(@PathVariable Long id) {
    return service.get(id);
  }

  @GetMapping("/{id}/students")
  public List<StudentDto> students(@PathVariable Long id) {
    return service.students(id);
  }

  @GetMapping("/{id}/schedules")
  public List<ScheduleDto> schedules(@PathVariable Long id) {
    return service.schedules(id);
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
  public SchoolClassDto create(@Valid @RequestBody SchoolClassDto.Request r) {
    return service.save(null, r);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
  public SchoolClassDto update(
      @PathVariable Long id, @Valid @RequestBody SchoolClassDto.Request r) {
    return service.save(id, r);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
  public SchoolClassDto status(
      @PathVariable Long id, @Valid @RequestBody SchoolClassDto.StatusRequest r) {
    return service.status(id, r.status());
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}
