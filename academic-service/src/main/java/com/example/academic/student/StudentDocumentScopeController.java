package com.example.academic.student;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/academic/student/me") @RequiredArgsConstructor @PreAuthorize("hasRole('STUDENT')")
public class StudentDocumentScopeController {
 private final StudentAccess access;
 @GetMapping("/document-scopes") public List<StudentAccess.DocumentScope> documents(@RequestParam(required=false) Long enrollmentId){return access.documents(enrollmentId);}
}
