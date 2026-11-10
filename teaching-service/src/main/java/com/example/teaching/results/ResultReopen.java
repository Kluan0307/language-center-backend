package com.example.teaching.results;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "result_reopens")
@Getter
@Setter
public class ResultReopen {
  @Id
  @Column(length = 36)
  private String requestId;

  private Long classId;
  private long previousRevision;
  private Long actorId;
  private String actorName;
  private LocalDateTime createdAt;

  @Column(length = 500)
  private String reason;
}
