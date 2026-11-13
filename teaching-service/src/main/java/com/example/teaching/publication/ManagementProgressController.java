package com.example.teaching.publication;

import com.example.teaching.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
@RequestMapping("/api/v1/teaching/students/{studentId}/enrollments/{enrollmentId}/published-grades")
public class ManagementProgressController {
  private final ManagementProgressService service;
  @GetMapping public PageResponse<StudentProgressService.GradeView> grades(@PathVariable Long studentId,@PathVariable Long enrollmentId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.grades(studentId,enrollmentId,page,size);}
}
