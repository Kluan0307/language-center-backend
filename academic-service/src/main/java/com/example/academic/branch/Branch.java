package com.example.academic.branch;

import com.example.academic.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "branches",
    uniqueConstraints = @UniqueConstraint(name = "uk_branch_code", columnNames = "code"))
@Getter
@Setter
public class Branch extends BaseEntity {
  @Column(nullable = false)
  private String code;

  @Column(nullable = false)
  private String name;

  private String address;

  @Column(nullable = false)
  private String phone;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.ACTIVE;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  public enum Status {
    ACTIVE,
    INACTIVE
  }
}
