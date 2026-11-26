package com.example.auth.company;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "organization_units")
@Getter
@Setter
public class OrganizationUnit {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @Column(nullable = false, length = 120)
  private String name;

  private Long parentId;
  private Long branchId;

  @Column(nullable = false)
  private boolean active = true;
}
