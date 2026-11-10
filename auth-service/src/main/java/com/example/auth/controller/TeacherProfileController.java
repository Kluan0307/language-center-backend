package com.example.auth.controller;

import com.example.auth.dto.TeacherProfileDto;
import com.example.auth.service.TeacherProfileService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/teacher-profiles")
public class TeacherProfileController {
  private final TeacherProfileService service;

  @GetMapping("/{id}")
  public TeacherProfileDto get(@PathVariable Long id, Principal actor) {
    return service.get(id, actor.getName());
  }

  @PutMapping("/{id}")
  public TeacherProfileDto update(
      @PathVariable Long id,
      @Valid @RequestBody TeacherProfileDto.Update request,
      Principal actor) {
    return service.update(id, request, actor.getName());
  }

  @GetMapping("/{id}/history")
  public List<TeacherProfileDto.Event> history(@PathVariable Long id, Principal actor) {
    return service.history(id, actor.getName());
  }

  @GetMapping("/{id}/responsibilities")
  public List<TeacherProfileDto.Responsibility> responsibilities(
      @PathVariable Long id, Principal actor) {
    return service.responsibilities(id, actor.getName());
  }

  @PatchMapping("/{id}/work-status")
  public TeacherProfileDto status(
      @PathVariable Long id,
      @Valid @RequestBody TeacherProfileDto.WorkStatus request,
      Principal actor) {
    return service.workStatus(id, request, actor.getName());
  }
}
