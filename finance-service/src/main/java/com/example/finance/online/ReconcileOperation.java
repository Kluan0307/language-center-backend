package com.example.finance.online;
import jakarta.persistence.*;
import java.time.*;
import lombok.*;
@Entity @Table(name="payment_reconcile_operations") @Getter @Setter
public class ReconcileOperation {
 @Id @Column(length=36) private String id;
 @Column(nullable=false,length=36) private String orderId;
 @Column(nullable=false) private Long actorId;
 @Column(nullable=false) private Long branchId;
 @Column(nullable=false,length=64) private String fingerprint;
 @Column(nullable=false,length=500) private String reason;
 @Column(nullable=false,length=20) private String status="PENDING";
 private Long eventId;
 @Column(nullable=false) private LocalDateTime createdAt;
 private LocalDateTime completedAt;
}
