package com.example.teaching.evaluation;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "evaluation_states")
@Getter
@Setter
public class EvaluationState {
  @Id private Long classId;

  @Column(nullable = false)
  private Long branchId;

  @Column(nullable = false)
  private long revision;

  private long sourceRevision;
}
