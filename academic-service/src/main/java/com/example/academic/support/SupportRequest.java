package com.example.academic.support;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
@Entity @Table(name="student_support_requests",indexes=@Index(name="idx_support_owner",columnList="student_id,branch_id,status")) @Getter @Setter
public class SupportRequest {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long studentId;
 @Column(nullable=false) private Long branchId;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Category category;
 @Column(nullable=false,length=120) private String subject;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status=Status.OPEN;
 @Column(length=20) private String resourceType;
 private Long resourceId;
 @Column(length=36) private String resourceKey;
 @Column(nullable=false) private LocalDateTime createdAt;
 @Column(nullable=false) private LocalDateTime updatedAt;
 @Version private long version;
 public enum Category {PROFILE,GRADE,ATTENDANCE,PAYMENT,LEAVE,MAKEUP,OTHER}
 public enum Status {OPEN,IN_PROGRESS,RESOLVED,CLOSED}
}
