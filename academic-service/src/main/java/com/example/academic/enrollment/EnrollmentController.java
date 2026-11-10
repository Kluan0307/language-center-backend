package com.example.academic.enrollment;

import com.example.academic.common.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/academic/enrollments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
public class EnrollmentController {
  private final EnrollmentService service;
  private final com.example.academic.lifecycle.LearningHistory history;

  @GetMapping("/{id}/history")
  public com.example.academic.lifecycle.LearningHistory.EnrollmentHistory history(
      @PathVariable Long id) {
    return history.enrollment(id);
  }

  private final TuitionDeliveryService delivery;
  private final com.example.academic.common.RemoteApi remote;

  @GetMapping("/{id}/billing")
  public EnrollmentBilling billing(@PathVariable Long id) {
    return service.billing(id);
  }

  @PostMapping("/{id}/tuition/retry")
  public TuitionDeliveryService.Delivery retry(@PathVariable Long id) {
    return delivery.deliver(id, remote.token());
  }

  @GetMapping
  public PageResponse<EnrollmentDto> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Enrollment.StudyStatus studyStatus,
      @RequestParam(required = false) Long classId,
      @RequestParam(required = false) Long studentId,
      @RequestParam(required = false) Long branchId) {
    return service.list(page, size, keyword, studyStatus, classId, studentId, branchId);
  }

  @GetMapping("/{id}")
  public EnrollmentDto get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  public EnrollmentDto create(@Valid @RequestBody EnrollmentDto.Request r) {
    return service.enroll(r);
  }

  @PatchMapping("/{id}/status")
  public EnrollmentDto status(
      @PathVariable Long id, @Valid @RequestBody EnrollmentDto.StatusRequest r) {
    return service.status(id, r);
  }

  @PostMapping("/{id}/transfer")
  public EnrollmentDto transfer(
      @PathVariable Long id, @Valid @RequestBody EnrollmentDto.TransferRequest r) {
    return service.transfer(id, r);
  }
}
