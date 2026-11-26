package com.example.teaching.grading;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @PreAuthorize("hasRole('STUDENT')")
public class StudentGradingController {
 private final GradingOutcomeService service;
 @GetMapping("/api/v1/teaching/student/me/enrollments/{id}/grading-result") public GradingOutcomeService.StudentView get(@PathVariable Long id){return service.student(id);}
}
