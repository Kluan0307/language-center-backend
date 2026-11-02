package com.example.academic.room;

import com.example.academic.common.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/academic/rooms")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
public class RoomController {
  private final RoomService service;

  @GetMapping
  public PageResponse<RoomDto> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Room.Status status,
      @RequestParam(required = false) Long branchId) {
    return service.list(page, size, keyword, status, branchId);
  }

  @GetMapping("/{id}")
  public RoomDto get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  public RoomDto create(@Valid @RequestBody RoomDto.Request r) {
    return service.save(null, r);
  }

  @PutMapping("/{id}")
  public RoomDto update(@PathVariable Long id, @Valid @RequestBody RoomDto.Request r) {
    return service.save(id, r);
  }

  @PatchMapping("/{id}/status")
  public RoomDto status(@PathVariable Long id, @Valid @RequestBody RoomDto.StatusRequest r) {
    return service.status(id, r.status());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}
