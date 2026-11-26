package com.example.auth.company;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "job_titles")
@Getter
@Setter
public class JobTitle {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false)
  private boolean active = true;
}
