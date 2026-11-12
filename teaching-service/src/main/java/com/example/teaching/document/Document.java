package com.example.teaching.document;

import com.example.teaching.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "documents",
    indexes = @Index(name = "idx_document_branch_status", columnList = "branch_id,status"))
@Getter
@Setter
public class Document extends BaseEntity {
  @Column(nullable=false) private boolean sharedToStudents;
  private LocalDateTime sharedAt;
  @Column(nullable = false)
  private int revision = 1;

  private LocalDateTime lastSubmittedAt;

  @Column(nullable = false)
  private Long branchId;

  @Column(nullable = false)
  private String title;

  private Long classId;
  private Long courseId;

  @Column(nullable = false)
  private String storageKey;

  private String originalFilename;
  private Long fileSize;

  @Column(length = 1000)
  private String description;

  @Column(nullable = false, updatable = false)
  private Long uploadedBy;

  private String uploadedByName;

  @Column(nullable = false, updatable = false)
  private LocalDateTime uploadedAt =
      LocalDateTime.now(com.example.teaching.common.BusinessTime.ZONE);

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.PENDING;

  private Long reviewedBy;
  private String reviewedByName;
  private LocalDateTime reviewedAt;

  @Column(length = 500)
  private String rejectReason;

  @Version private Long version;

  public enum Status {
    PENDING,
    APPROVED,
    REJECTED
  }
}
