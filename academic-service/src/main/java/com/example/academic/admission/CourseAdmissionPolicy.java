package com.example.academic.admission;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import lombok.*;

@Entity
@Table(
    name = "course_admission_policies",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_course_admission_revision",
            columnNames = {"course_id", "revision"}))
@Getter
@Setter
public class CourseAdmissionPolicy {
  public enum Mode {
    UNVERIFIED,
    OPEN_ENTRY,
    REQUIRES_ASSESSMENT
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, updatable = false)
  private Long courseId;

  @Column(nullable = false, updatable = false)
  private long revision;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 40)
  private Mode mode;

  @Column(nullable = false, updatable = false)
  private Long programId;

  @Column(nullable = false, updatable = false, length = 50)
  private String programCode;

  @Column(nullable = false, updatable = false)
  private String programName;

  @Column(nullable = false, updatable = false)
  private Long outputLevelId;

  @Column(nullable = false, updatable = false, length = 50)
  private String outputLevelCode;

  @Column(nullable = false, updatable = false)
  private String outputLevelName;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "course_admission_levels",
      joinColumns = @JoinColumn(name = "policy_id"),
      uniqueConstraints =
          @UniqueConstraint(
              name = "uk_admission_level",
              columnNames = {"policy_id", "level_id"}))
  @OrderBy("levelId ASC")
  private List<LevelSnapshot> acceptedLevels = new ArrayList<>();

  @Column(nullable = false, updatable = false)
  private Long confirmedBy;

  @Column(nullable = false, updatable = false)
  private String confirmedByName;

  @Column(nullable = false, updatable = false)
  private LocalDateTime confirmedAt;

  @Column(nullable = false, updatable = false, length = 500)
  private String reason;

  @Embeddable
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LevelSnapshot {
    @Column(name = "level_id", nullable = false)
    private Long levelId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;
  }
}
