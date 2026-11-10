package com.example.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 50)
  private String username;

  @Column(nullable = false)
  @ToString.Exclude
  private String password; // Chuỗi đã mã hóa BCrypt

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role;

  @Builder.Default
  @Column(nullable = false)
  private Boolean status = true;

  @Builder.Default
  @Column(nullable = false)
  private long sessionVersion = 0;

  @Builder.Default
  @Column(nullable = false)
  private boolean mustChangePassword = false;

  private LocalDateTime passwordChangedAt;

  @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
  @Builder.Default
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private java.util.List<AccountSecurityEvent> securityEvents = new java.util.ArrayList<>();

  public void recordSecurityEvent(String action, Long actorId, String reason) {
    var event = new AccountSecurityEvent();
    event.setAccount(this);
    event.setAction(action);
    event.setActorId(actorId);
    event.setReason(reason);
    event.setOccurredAt(LocalDateTime.now(com.example.auth.common.BusinessTime.ZONE));
    securityEvents.add(event);
  }

  @Column(updatable = false)
  private LocalDateTime createdAt;

  private LocalDateTime lastLogin;

  @Column(name = "branch_id")
  private Long branchId; // null với SUPER_ADMIN

  @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
  @Builder.Default
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private java.util.List<AccountBranch> branchAssignments = new java.util.ArrayList<>();

  public java.util.List<Long> activeBranchIds() {
    return branchAssignments.stream()
        .filter(AccountBranch::isStatus)
        .map(AccountBranch::getBranchId)
        .sorted()
        .toList();
  }

  @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
  @Builder.Default
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private java.util.List<AccountBranchEvent> branchEvents = new java.util.ArrayList<>();

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now(com.example.auth.common.BusinessTime.ZONE);
    if (this.status == null) {
      this.status = true;
    }
  }
}
