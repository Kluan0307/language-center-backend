package com.example.teaching.document;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record DocumentDto(
    Long id,
    Long branchId,
    String title,
    Long classId,
    Long courseId,
    String filename,
    Long fileSize,
    String description,
    Long uploadedBy,
    String uploadedByName,
    LocalDateTime uploadedAt,
    Document.Status status,
    Long reviewedBy,
    String reviewedByName,
    LocalDateTime reviewedAt,
    String rejectReason,
    Long version,
    int revision,
    LocalDateTime lastSubmittedAt) {
  public static DocumentDto of(Document d) {
    return new DocumentDto(
        d.getId(),
        d.getBranchId(),
        d.getTitle(),
        d.getClassId(),
        d.getCourseId(),
        d.getOriginalFilename(),
        d.getFileSize(),
        d.getDescription(),
        d.getUploadedBy(),
        d.getUploadedByName(),
        d.getUploadedAt(),
        d.getStatus(),
        d.getReviewedBy(),
        d.getReviewedByName(),
        d.getReviewedAt(),
        d.getRejectReason(),
        d.getVersion(),
        d.getRevision(),
        d.getLastSubmittedAt() == null ? d.getUploadedAt() : d.getLastSubmittedAt());
  }

  public record Review(
      @NotNull Document.Status status,
      @Size(max = 500) String reason,
      @NotNull @PositiveOrZero Long version) {}
}
