package com.example.academic.admission;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "program_levels",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_program_level_code",
            columnNames = {"program_id", "code"}))
@Getter
@Setter
public class ProgramLevel {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "program_id", nullable = false)
  private Program program;

  @Column(nullable = false, length = 50)
  private String code;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(name = "sort_order", nullable = false)
  private int rank;

  @Column(nullable = false)
  private boolean active = true;
}
