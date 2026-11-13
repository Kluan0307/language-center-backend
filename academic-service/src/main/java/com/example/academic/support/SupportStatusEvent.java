package com.example.academic.support;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Staff-only audit trail. Never included in StudentDetail. */
@Entity
@Table(name="student_support_status_events",indexes=@Index(name="idx_support_status_request",columnList="request_id,id"))
@Getter @Setter
public class SupportStatusEvent {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(nullable=false) private Long requestId;
  @Enumerated(EnumType.STRING) @Column(length=20) private SupportRequest.Status previousStatus;
  @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private SupportRequest.Status status;
  @Column(nullable=false) private Long actorId;
  @Column(nullable=false,length=100) private String actorName;
  @Column(nullable=false,length=500) private String reason;
  @Column(nullable=false) private LocalDateTime createdAt;
}
