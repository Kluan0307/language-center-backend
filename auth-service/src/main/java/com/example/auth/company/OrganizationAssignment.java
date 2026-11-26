package com.example.auth.company;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "organization_assignments",
    uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "unit_id", "title_id"}))
@Getter
@Setter
public class OrganizationAssignment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @Column(nullable = false)
  private Long accountId;

  @Column(nullable = false)
  private Long unitId;

  @Column(nullable = false)
  private Long titleId;

  private Long managerAccountId;

  @Column(nullable = false)
  private boolean active = true;
}
