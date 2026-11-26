package com.example.academic.assistant;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/academic/assistant-query")
public class AcademicInsightController {
  private final AcademicInsights service;

  @GetMapping("/course-admissions")
  public ReadQuery.Result admissions(@ModelAttribute AcademicInsights.Request r) {
    return service.admissions(r);
  }

  @GetMapping("/classes")
  public ReadQuery.Result classes(@ModelAttribute AcademicInsights.Request r) {
    return service.classes(r);
  }

  @GetMapping("/students")
  public ReadQuery.Result students(@ModelAttribute AcademicInsights.Request r) {
    return service.students(r);
  }

  @GetMapping("/student-profile")
  public ReadQuery.Result profile(@ModelAttribute AcademicInsights.Request r) {
    return service.profile(r);
  }

  @GetMapping("/roster-statistics")
  public ReadQuery.Result roster(@ModelAttribute AcademicInsights.Request r) {
    return service.rosterCount(r);
  }

  @GetMapping("/sessions")
  public ReadQuery.Result sessions(@ModelAttribute AcademicInsights.Request r) {
    return service.sessions(r);
  }

  @GetMapping("/rooms")
  public ReadQuery.Result rooms(@ModelAttribute AcademicInsights.Request r) {
    return service.rooms(r);
  }

  @GetMapping("/available-teachers")
  public ReadQuery.Result teachers(@ModelAttribute AcademicInsights.Request r) {
    return service.availableTeachers(r);
  }

  @GetMapping("/academic-statistics")
  public ReadQuery.Result grouped(@ModelAttribute AcademicInsights.Request r) {
    return service.grouped(r);
  }

  @GetMapping("/attendance-summary")
  public ReadQuery.Result absences(@ModelAttribute AcademicInsights.Request r) {
    return service.absences(r);
  }

  @GetMapping("/missing-attendance")
  public ReadQuery.Result missing(@ModelAttribute AcademicInsights.Request r) {
    return service.missingAttendance(r);
  }
}
