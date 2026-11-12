package com.example.auth.student;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name="student_account_provisionings", uniqueConstraints={@UniqueConstraint(name="uk_student_provision_student", columnNames="student_id"),@UniqueConstraint(name="uk_student_provision_account", columnNames="account_id")})
@Getter @Setter
public class StudentProvisioning {
  @Id @Column(length=36) private String requestId;
  @Column(nullable=false) private Long studentId;
  @Column(nullable=false) private Long accountId;
  @Column(nullable=false,length=50) private String username;
  @Column(nullable=false) private long expectedStudentVersion;
  @Column(nullable=false) private String initialPasswordHash;
  @Column(nullable=false) private Long createdBy;
  @Column(nullable=false) private LocalDateTime createdAt;
  @Version private long version;
  @Column(nullable=false) private long operationCount;
}
