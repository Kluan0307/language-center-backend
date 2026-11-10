package com.example.academic.lead;

import com.example.academic.common.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/academic/leads")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
public class LeadController {
  private final LeadService service;
  private final LeadAssigneeDirectory assignees;

  @GetMapping("/assignees")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public java.util.List<LeadAssigneeDirectory.Assignee> assignees(@RequestParam Long branchId) {
    return assignees.list(branchId);
  }

  @GetMapping("/{id}/history")
  public java.util.List<LeadActivity> history(@PathVariable Long id) {
    return service.history(id);
  }

  @PostMapping("/{id}/interactions")
  public LeadDto interact(@PathVariable Long id, @Valid @RequestBody LeadDto.Interaction r) {
    return service.interact(id, r);
  }

  @PutMapping("/{id}/assignment")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public LeadDto assign(@PathVariable Long id, @Valid @RequestBody LeadDto.Assignment r) {
    return service.assign(id, r);
  }

  @GetMapping
  public PageResponse<LeadDto> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Lead.Status status,
      @RequestParam(required = false) Long assignedTo,
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) Lead.FollowUp followUp,
      @RequestParam(required = false) Boolean active) {
    return service.list(page, size, keyword, status, assignedTo, branchId, followUp, active);
  }

  @GetMapping("/{id}")
  public LeadDto get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  public LeadDto create(@Valid @RequestBody LeadDto.Request r) {
    return service.save(null, r);
  }

  @PutMapping("/{id}")
  public LeadDto update(@PathVariable Long id, @Valid @RequestBody LeadDto.Request r) {
    return service.save(id, r);
  }

  @PatchMapping("/{id}/status")
  public LeadDto status(@PathVariable Long id, @Valid @RequestBody LeadDto.StatusRequest r) {
    return service.status(id, r);
  }

  @PostMapping("/{id}/convert")
  public LeadDto convert(@PathVariable Long id, @Valid @RequestBody LeadDto.Conversion r) {
    return service.convert(id, r.version());
  }
}
