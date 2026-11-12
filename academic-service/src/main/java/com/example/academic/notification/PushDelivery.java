package com.example.academic.notification;
import jakarta.persistence.*;import java.time.*;import lombok.*;
@Entity @Table(name="student_push_deliveries",uniqueConstraints=@UniqueConstraint(columnNames={"notification_id","installation_id"})) @Getter @Setter
public class PushDelivery {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long notificationId;
 @Column(nullable=false,length=36) private String installationId;
 private long bindingVersion;
 @Column(nullable=false,length=20) private String status="PENDING";
 @Column(length=100) private String ticketId;
 @Column(length=60) private String resultCode;
 private LocalDateTime attemptedAt; private LocalDateTime receiptCheckedAt;
}
