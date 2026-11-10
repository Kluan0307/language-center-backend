package com.example.academic.duration;

import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/academic")
public class DurationController {
  private final DurationPlanService plans;
  private final DurationProgressService progress;

  @GetMapping("/courses/{id}/duration-plan")
  public DurationDto.CourseView course(@PathVariable Long id) {
    return plans.getCourse(id);
  }

  @PutMapping("/courses/{id}/duration-plan")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public DurationDto.CourseView confirmCourse(
      @PathVariable Long id, @Valid @RequestBody DurationDto.CourseRequest r) {
    return plans.confirmCourse(id, r);
  }

  @GetMapping("/courses/{id}/duration-history")
  public List<CourseDurationPlan> courseHistory(@PathVariable Long id) {
    return plans.courseHistory(id);
  }

  @GetMapping("/classes/{id}/duration-plan")
  public DurationDto.ClassView school(@PathVariable Long id) {
    return plans.getClass(id);
  }

  @PutMapping("/classes/{id}/duration-plan")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public DurationDto.ClassView confirmClass(
      @PathVariable Long id, @Valid @RequestBody DurationDto.ClassRequest r) {
    return plans.confirmClass(id, r);
  }

  @GetMapping("/classes/{id}/duration-history")
  public List<ClassDurationPlan> classHistory(@PathVariable Long id) {
    return plans.classHistory(id);
  }

  @GetMapping("/classes/{id}/duration-progress")
  public DurationDto.Progress get(@PathVariable Long id) {
    return progress.get(id);
  }

  @GetMapping("/classes/duration-progress")
  public List<DurationDto.Progress> batch(@RequestParam List<Long> classIds) {
    return progress.batch(classIds);
  }
}
