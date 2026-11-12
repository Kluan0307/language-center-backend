package com.example.finance.online;
import jakarta.persistence.*;
import java.time.*;
import lombok.*;
@Entity @Table(name="payment_outbox",uniqueConstraints=@UniqueConstraint(columnNames="event_key")) @Getter @Setter
public class PaymentOutbox {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=100) private String eventKey;
 @Column(nullable=false,length=40) private String type;
 @Column(nullable=false,length=36) private String orderId;
 @Column(nullable=false) private Long studentId;
 private Long receiptId;
 @Column(nullable=false) private LocalDateTime createdAt;
 private LocalDateTime deliveredAt;
}
