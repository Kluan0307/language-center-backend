package com.example.teaching.document;

import com.example.teaching.common.*;
import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(
    name = "document_events",
    indexes = @Index(name = "idx_document_event", columnList = "document_id,revision,id"))
@Getter
@Setter
public class DocumentEvent extends BaseEntity {
  @Column(nullable = false, updatable = false)
  private Long documentId;

  @Column(nullable = false, updatable = false)
  private int revision;

  @Column(nullable = false, updatable = false)
  private String action;

  @Column(nullable = false, updatable = false)
  private String status;

  @Column(nullable = false, updatable = false)
  private String storageKey;

  @Column(nullable = false, updatable = false)
  private String title;

  @Column(updatable = false, length = 1000)
  private String description;

  @Column(updatable = false)
  private String filename;

  @Column(updatable = false)
  private Long fileSize;

  @Column(updatable = false, length = 500)
  private String reason;

  @Column(updatable = false)
  private Long actorId;

  @Column(updatable = false)
  private String actorName;

  @Column(nullable = false, updatable = false)
  private LocalDateTime occurredAt = LocalDateTime.now(BusinessTime.ZONE);
}
