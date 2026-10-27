package com.example.academic.lead;

import com.example.academic.common.BaseEntity;
import com.example.academic.course.Course;
import com.example.academic.student.Student;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "leads")
@Getter
@Setter
public class Lead extends BaseEntity {
  @Column(nullable = false)
  private String fullName;

  @Column(nullable = false)
  private String phone;

  private String email;
  private String source;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "interested_course_id")
  private Course interestedCourse;

  private String desiredLevel;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.NEW;

  @Column(columnDefinition = "longtext")
  private String note;

  @Column(nullable = false)
  private Long assignedTo;

  @Column(nullable = false)
  private String assignedToName;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "converted_student_id", unique = true)
  private Student convertedStudent;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  private LocalDateTime lastContactedAt;

  @Column(nullable = false)
  private Long branchId;

  public enum Status {
    NEW,
    CONTACTED,
    CONSULTING,
    CONVERTED,
    LOST
  }
}
