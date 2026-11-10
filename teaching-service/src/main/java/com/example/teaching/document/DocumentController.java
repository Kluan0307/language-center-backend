package com.example.teaching.document;

import com.example.teaching.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/teaching/documents")
@RequiredArgsConstructor
public class DocumentController {
  private final DocumentService service;
  private final DocumentStorage storage;

  @GetMapping("/{id}/history")
  public java.util.List<DocumentService.Event> history(@PathVariable Long id) {
    return service.history(id);
  }

  @GetMapping("/{id}/revisions/{revision}/download")
  public ResponseEntity<Resource> revision(@PathVariable Long id, @PathVariable int revision) {
    var event = service.revision(id, revision);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(
                    event.getFilename() == null ? "document" : event.getFilename(),
                    java.nio.charset.StandardCharsets.UTF_8)
                .build()
                .toString())
        .body(storage.load(event.getStorageKey()));
  }

  @PostMapping(value = "/{id}/resubmit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','TEACHER')")
  public DocumentDto resubmit(
      @PathVariable Long id,
      @RequestParam String title,
      @RequestParam(required = false) String description,
      @RequestParam String reason,
      @RequestParam Long version,
      @RequestPart(value = "file", required = false) MultipartFile file) {
    return service.resubmit(id, title, description, reason, version, file);
  }

  @GetMapping
  public PageResponse<DocumentDto> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) Long classId,
      @RequestParam(required = false) Long courseId,
      @RequestParam(required = false) Document.Status status) {
    return service.list(page, size, branchId, classId, courseId, status);
  }

  @GetMapping("/{id}")
  public DocumentDto get(@PathVariable Long id) {
    return DocumentDto.of(service.require(id));
  }

  @GetMapping("/{id}/download")
  public ResponseEntity<Resource> download(@PathVariable Long id) {
    Document d = service.require(id);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(d.getOriginalFilename(), java.nio.charset.StandardCharsets.UTF_8)
                .build()
                .toString())
        .body(storage.load(d.getStorageKey()));
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','TEACHER')")
  public DocumentDto upload(
      @RequestParam String title,
      @RequestParam(required = false) String description,
      @RequestParam(required = false) Long classId,
      @RequestParam(required = false) Long courseId,
      @RequestParam(required = false) Long branchId,
      @RequestPart("file") MultipartFile file) {
    return service.upload(title, description, classId, courseId, branchId, file);
  }

  @PatchMapping("/{id}/review")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public DocumentDto review(@PathVariable Long id, @Valid @RequestBody DocumentDto.Review r) {
    return service.review(id, r);
  }
}
