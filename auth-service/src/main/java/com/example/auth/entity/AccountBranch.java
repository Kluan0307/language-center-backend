package com.example.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "account_branches",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_account_branch",
            columnNames = {"account_id", "branch_id"}),
    indexes = @Index(name = "idx_membership_branch_status", columnList = "branch_id,status"))
@Getter
@Setter
@NoArgsConstructor
public class AccountBranch {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Column(nullable = false)
  private boolean status;

  @Column(name = "is_primary", nullable = false)
  private boolean primary;

  @Column(nullable = false)
  private LocalDateTime assignedAt;

  private Long assignedBy;
  private LocalDateTime revokedAt;
}
