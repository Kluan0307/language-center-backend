package com.example.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "teacher_profile_events",
    indexes = @Index(name = "idx_teacher_event", columnList = "teacher_id,id"))
@Getter
@Setter
public class TeacherProfileEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, updatable = false)
  private Long teacherId;

  @Column(nullable = false, updatable = false)
  private String action;

  @Column(columnDefinition = "longtext", nullable = false, updatable = false)
  private String beforeState;

  @Column(columnDefinition = "longtext", nullable = false, updatable = false)
  private String afterState;

  @Column(length = 500, updatable = false)
  private String reason;

  @Column(nullable = false, updatable = false)
  private Long actorId;

  @Column(nullable = false, updatable = false)
  private String actorName;

  @Column(nullable = false, updatable = false)
  private long teacherVersion;

  @Column(nullable = false, updatable = false)
  private LocalDateTime occurredAt;
}
