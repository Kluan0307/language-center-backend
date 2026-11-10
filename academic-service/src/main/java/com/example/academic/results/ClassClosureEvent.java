package com.example.academic.results;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "class_closure_events")
@Getter
@Setter
public class ClassClosureEvent {
  @Id
  @Column(length = 36)
  private String requestId;

  private Long classId;
  private long expectedRevision;
  private long revision;
  private boolean closed;
  private String actorName;
  private Long actorId;
  private LocalDateTime changedAt;

  @Column(length = 500)
  private String reason;
}
