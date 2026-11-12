package com.example.academic.notification;
import jakarta.persistence.*;import java.time.*;import lombok.*;
@Entity @Table(name="student_push_devices",uniqueConstraints=@UniqueConstraint(columnNames="push_token")) @Getter @Setter
public class PushDevice {
 @Id @Column(length=36) private String installationId;
 @Column(nullable=false) private Long accountId;
 private long sessionVersion;
 @Column(nullable=false,length=255) private String pushToken;
 @Column(nullable=false,length=10) private String platform;
 @Column(nullable=false,length=36) private String projectId;
 private boolean enabled=true;
 @Column(nullable=false) private LocalDateTime updatedAt;
 @Version private long bindingVersion;
}
