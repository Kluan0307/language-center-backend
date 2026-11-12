package com.example.academic.notification;
import jakarta.persistence.*;
import java.time.*;
import lombok.*;
@Entity @Table(name="student_notification_outbox",uniqueConstraints=@UniqueConstraint(columnNames="source_key")) @Getter @Setter
public class StudentEvent {
 @Id @Column(length=36) private String id;
 @Column(nullable=false,length=160) private String sourceKey;
 @Column(nullable=false,length=20) private String category;
 @Column(nullable=false,length=40) private String type;
 @Column(nullable=false,length=120) private String title;
 @Column(nullable=false,length=300) private String body;
 private Long studentId; private Long enrollmentId; private Long classId; private Long courseId; private Long branchId;
 @Column(nullable=false,length=20) private String resourceType;
 @Column(nullable=false,length=80) private String resourceId;
 private LocalDate sessionDate; private LocalDate previousDate;
 @Column(nullable=false) private LocalDateTime occurredAt;
 private LocalDateTime deliveredAt;
 @Column(nullable=false,length=12) private String status="PENDING";
 private int recipientCount;
}
