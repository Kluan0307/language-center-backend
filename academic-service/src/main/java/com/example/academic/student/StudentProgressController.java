package com.example.academic.student;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/academic/student/me/enrollments/{id}") @RequiredArgsConstructor @PreAuthorize("hasRole('STUDENT')")
public class StudentProgressController {
 private final StudentProgressService service;
 @GetMapping("/attendance") public StudentProgressService.AttendancePage attendance(@PathVariable Long id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){return service.attendance(id,page,size,from,to);}
 @GetMapping("/result-source") public StudentProgressService.Source source(@PathVariable Long id){return service.source(id);}
}
