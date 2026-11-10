package com.example.academic.student;

import com.example.academic.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(
    name = "students",
    indexes = @Index(name = "idx_student_phone_branch", columnList = "phone,branch_id"))
@Getter
@Setter
public class Student extends BaseEntity {
  @Version private long version;

  @Column(unique = true, length = 40, updatable = false)
  private String studentCode = "HV-" + java.util.UUID.randomUUID();

  private String guardianName;
  private String guardianPhone;

  @Column(nullable = false)
  private String fullName;

  private LocalDate dateOfBirth;

  @Enumerated(EnumType.STRING)
  private Gender gender;

  private String email;

  @Column(nullable = false)
  private String phone;

  private String address;
  private String contactPerson;
  private String currentLevel;

  @Column(nullable = false, updatable = false)
  private LocalDate registeredAt = LocalDate.now(com.example.academic.common.BusinessTime.ZONE);

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.STUDYING;

  @Column(nullable = false)
  private Long branchId;

  public enum Gender {
    MALE,
    FEMALE,
    OTHER
  }

  public enum Status {
    STUDYING,
    PAUSED,
    GRADUATED
  }
}
