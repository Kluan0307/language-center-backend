package com.example.teaching.grading;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor
public class GradingTransitionController {
 private final GradingLegacyService legacy;private final GradingCompletionService completion;private final GradingReadService read;
 @GetMapping("/api/v1/teaching/grading/classes/{id}/legacy/source") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')") public GradingLegacyService.Source source(@PathVariable Long id){return legacy.source(id);}
 @PostMapping("/api/v1/teaching/grading/classes/{id}/legacy/preview") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')") public GradingLegacyService.Preview preview(@PathVariable Long id,@Valid @RequestBody GradingLegacyService.Plan r){return legacy.preview(id,r);}
 @PostMapping("/api/v1/teaching/grading/classes/{id}/legacy/apply") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')") public GradingLegacyService.Imported apply(@PathVariable Long id,@Valid @RequestBody GradingLegacyService.Apply r){return legacy.apply(id,r);}
 @PostMapping("/api/v1/teaching/grading/classes/{id}/completion/preview") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')") public GradingCompletionService.Preview completion(@PathVariable Long id){return completion.preview(id);}
 @PostMapping("/api/v1/teaching/grading/classes/{id}/completion/confirm") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')") public com.example.teaching.results.FinalResultDto.ClassResults confirm(@PathVariable Long id,@Valid @RequestBody GradingCompletionService.Command r){return completion.confirm(id,r);}
 @GetMapping("/api/v1/teaching/grading/classes/{id}/export") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','TEACHER')") public ResponseEntity<byte[]> export(@PathVariable Long id,@RequestParam(defaultValue="true") boolean published){return csv(read.export(id,published),"bang-diem-"+id+".csv");}
 @GetMapping("/api/v1/teaching/student/me/enrollments/{id}/grading-result/export") @PreAuthorize("hasRole('STUDENT')") public ResponseEntity<byte[]> studentExport(@PathVariable Long id){return csv(read.studentExport(id),"bang-diem-ca-nhan.csv");}
 @GetMapping("/api/v1/teaching/students/{studentId}/enrollments/{enrollmentId}/grading-result") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')") public GradingOutcomeService.StudentView published(@PathVariable Long studentId,@PathVariable Long enrollmentId){return read.management(studentId,enrollmentId);}
 private ResponseEntity<byte[]> csv(byte[] bytes,String name){return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+name+"\"").header("X-Content-Type-Options","nosniff").body(bytes);}
}
