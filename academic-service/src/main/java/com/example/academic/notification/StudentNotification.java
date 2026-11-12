package com.example.academic.notification;
import jakarta.persistence.*;import java.time.*;import lombok.*;
@Entity @Table(name="student_notifications",uniqueConstraints=@UniqueConstraint(columnNames={"event_id","account_id","student_id"})) @Getter @Setter
public class StudentNotification {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=36) private String eventId;
 @Column(nullable=false) private Long accountId;
 @Column(nullable=false) private Long studentId;
 @Column(nullable=false,length=20) private String category;
 @Column(nullable=false,length=40) private String type;
 @Column(nullable=false,length=120) private String title;
 @Column(nullable=false,length=300) private String body;
 @Column(nullable=false,length=20) private String resourceType;
 @Column(nullable=false,length=80) private String resourceId;
 @Column(nullable=false) private LocalDateTime createdAt;
 private LocalDateTime readAt;
}
