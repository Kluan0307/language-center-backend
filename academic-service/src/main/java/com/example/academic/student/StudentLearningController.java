package com.example.academic.student;
import com.example.academic.common.PageResponse;
import com.example.academic.enrollment.Enrollment;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/academic/student/me") @RequiredArgsConstructor @PreAuthorize("hasRole('STUDENT')")
public class StudentLearningController {
 private final StudentLearningService service;
 @GetMapping("/enrollments") public PageResponse<StudentLearningService.EnrollmentView> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="ALL") StudentLearningService.Group group,@RequestParam(required=false) Enrollment.StudyStatus status){return service.enrollments(page,size,group,status);}
 @GetMapping("/enrollments/{id}") public StudentLearningService.Detail detail(@PathVariable Long id){return service.detail(id);}
 @GetMapping("/schedule") public PageResponse<StudentLearningService.SessionView> schedule(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,@RequestParam(required=false) Long enrollmentId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.schedule(from,to,enrollmentId,page,size);}
 @GetMapping("/sessions/{id}") public StudentLearningService.SessionView session(@PathVariable Long id){return service.session(id);}
 @GetMapping("/next-session") public StudentLearningService.Next next(){return service.next();}
}
