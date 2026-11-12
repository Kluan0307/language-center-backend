package com.example.teaching.document;
import com.example.teaching.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor
public class StudentDocumentController {
 private final StudentDocumentService service; private final DocumentStorage storage;
 @PatchMapping("/api/v1/teaching/documents/{id}/student-sharing") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','TEACHER')") public DocumentDto share(@PathVariable Long id,@Valid @RequestBody StudentDocumentService.Sharing r){return service.share(id,r);}
 @GetMapping("/api/v1/teaching/student/me/documents") @PreAuthorize("hasRole('STUDENT')") public PageResponse<StudentDocumentService.View> list(@RequestParam(required=false) Long enrollmentId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.list(enrollmentId,page,size);}
 @GetMapping("/api/v1/teaching/student/me/documents/{id}/download") @PreAuthorize("hasRole('STUDENT')") public ResponseEntity<Resource> download(@PathVariable Long id){var d=service.download(id);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_OCTET_STREAM).header("X-Content-Type-Options","nosniff").header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(d.getOriginalFilename(),java.nio.charset.StandardCharsets.UTF_8).build().toString()).body(storage.load(d.getStorageKey()));}
}
