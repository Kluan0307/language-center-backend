package com.example.academic.lead;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
@Entity @Table(name="lead_intake_receipts",uniqueConstraints=@UniqueConstraint(name="uk_intake_lead",columnNames="lead_id")) @Getter @Setter
public class LeadIntakeReceipt {
 @Id @Column(length=36) private String requestId;
 @Column(nullable=false) private Long leadId;
 @Column(nullable=false) private Long studentId;
 private Long enrollmentId;
 @Column(nullable=false) private boolean createdStudent;
 @Column(nullable=false,length=64) private String intent;
 @Column(nullable=false) private Long actorId;
 @Column(nullable=false) private LocalDateTime createdAt;
 @Column(nullable=false,length=500) private String reason;
}
