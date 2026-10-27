package com.example.academic.enrollment;

import com.example.academic.common.BaseEntity;
import com.example.academic.schoolclass.SchoolClass;
import com.example.academic.student.Student;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "enrollments",
    indexes = {
      @Index(name = "idx_enrollment_class_status", columnList = "class_id,study_status"),
      @Index(name = "idx_enrollment_student_status", columnList = "student_id,study_status")
    })
@Getter
@Setter
public class Enrollment extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "student_id", nullable = false)
  private Student student;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "class_id", nullable = false)
  private SchoolClass schoolClass;

  @Column(nullable = false, updatable = false)
  private LocalDateTime enrolledAt = LocalDateTime.now();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StudyStatus studyStatus = StudyStatus.STUDYING;

  public enum StudyStatus {
    STUDYING,
    DROPPED,
    COMPLETED,
    TRANSFERRED,
    RESERVED
  }
}
