package com.example.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "account_branch_events",
    indexes = @Index(name = "idx_branch_event_account_time", columnList = "account_id,occurred_at"))
@Getter
@Setter
@NoArgsConstructor
public class AccountBranchEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @Column(nullable = false)
  private Long branchId;

  @Column(nullable = false, length = 30)
  private String action;

  @Column(nullable = false)
  private LocalDateTime occurredAt;

  private Long performedBy;
}
