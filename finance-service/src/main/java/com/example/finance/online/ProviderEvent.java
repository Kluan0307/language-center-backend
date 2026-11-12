package com.example.finance.online;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;
@Entity @Table(name="payment_provider_events",uniqueConstraints=@UniqueConstraint(columnNames="event_key")) @Getter @Setter
public class ProviderEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=64) private String eventKey;
 @Column(nullable=false,length=12) private String source;
 @Column(nullable=false,length=40) private String merchantRef;
 @Column(nullable=false,length=32) private String merchantCode;
 @Column(length=32) private String transactionNo;
 @Column(nullable=false,precision=15,scale=2) private BigDecimal amount;
 @Column(nullable=false,length=12) private String outcome;
 private LocalDateTime paidAt;
 @Column(nullable=false,length=20) private String status="RECEIVED";
 @Column(length=60) private String resultCode;
 @Column(length=36) private String orderId;
 private Long branchId;
 @Column(nullable=false) private LocalDateTime receivedAt;
 private LocalDateTime processedAt;
 @Version private long version;
}
