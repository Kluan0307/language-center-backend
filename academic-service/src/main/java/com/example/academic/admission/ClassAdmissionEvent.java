package com.example.academic.admission;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "class_admission_events",
    indexes = @Index(name = "idx_class_admission_event", columnList = "class_id,id"))
@Getter
@Setter
public class ClassAdmissionEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, updatable = false)
  private Long classId;

  @Column(updatable = false)
  private Long oldPolicyId;

  @Column(updatable = false)
  private Long newPolicyId;

  @Column(nullable = false, updatable = false)
  private long revision;

  @Column(nullable = false, updatable = false)
  private Long actorId;

  @Column(nullable = false, updatable = false)
  private String actorName;

  @Column(nullable = false, updatable = false)
  private LocalDateTime occurredAt;

  @Column(nullable = false, updatable = false, length = 500)
  private String reason;
}
