package com.example.academic.results;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "class_closures")
@Getter
@Setter
public class ClassClosure {
  @Id private Long classId;
  private long revision;
  private long resultRevision;
  private long sourceRevision;
  private boolean closed;
  private String actorName;
  private Long actorId;
  private LocalDateTime changedAt;

  @Column(length = 500)
  private String reason;
}
