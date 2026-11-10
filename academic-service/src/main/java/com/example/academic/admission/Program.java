package com.example.academic.admission;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "programs",
    uniqueConstraints = @UniqueConstraint(name = "uk_program_code", columnNames = "code"))
@Getter
@Setter
public class Program {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @Column(nullable = false, length = 50)
  private String code;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(nullable = false)
  private boolean active = true;
}
