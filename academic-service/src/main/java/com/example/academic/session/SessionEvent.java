package com.example.academic.session;

import com.example.academic.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "session_events",
    indexes = @Index(name = "idx_session_event", columnList = "session_id,id"))
@Getter
@Setter
public class SessionEvent extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "session_id", nullable = false)
  private ClassSession session;

  @Column(nullable = false, length = 40)
  private String action;

  @Column(length = 500)
  private String reason;

  @Column(columnDefinition = "text")
  private String beforeState;

  @Column(columnDefinition = "text")
  private String afterState;

  @Column(nullable = false)
  private Long actorId;

  @Column(nullable = false)
  private String actorName;

  @Column(nullable = false)
  private LocalDateTime occurredAt;
}
