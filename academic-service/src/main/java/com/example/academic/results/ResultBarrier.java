package com.example.academic.results;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "class_result_barriers")
@Getter
@Setter
@NoArgsConstructor
public class ResultBarrier {
  @Id private Long classId;
  private Long branchId;

  @Column(nullable = false, length = 36)
  private String token;

  private long sourceRevision;
  private long resultRevision;
  private long confirmedCount;
}
