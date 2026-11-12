package com.example.teaching.publication;
import com.example.teaching.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/teaching/student/me/enrollments/{id}") @RequiredArgsConstructor @PreAuthorize("hasRole('STUDENT')")
public class StudentProgressController {
 private final StudentProgressService service;
 @GetMapping("/grades") public PageResponse<StudentProgressService.GradeView> grades(@PathVariable Long id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.grades(id,page,size);}
 @GetMapping("/final-result") public StudentProgressService.Outcome outcome(@PathVariable Long id){return service.outcome(id);}
}
