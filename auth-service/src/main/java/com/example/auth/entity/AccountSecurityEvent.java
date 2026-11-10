package com.example.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "account_security_events",
    indexes =
        @Index(name = "idx_security_event_account_time", columnList = "account_id,occurred_at"))
@Getter
@Setter
@NoArgsConstructor
public class AccountSecurityEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @Column(nullable = false, length = 40)
  private String action;

  @Column(nullable = false)
  private Long actorId;

  @Column(nullable = false)
  private LocalDateTime occurredAt;

  @Column(length = 255)
  private String reason;
}
