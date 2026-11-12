package com.example.finance.online;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;
@Entity @Table(name="payment_orders",uniqueConstraints={@UniqueConstraint(columnNames={"account_id","request_id"}),@UniqueConstraint(columnNames="merchant_ref")}) @Getter @Setter
public class OnlineOrder {
 @Id @Column(length=36) private String id;
 @Column(nullable=false) private Long accountId;
 @Column(nullable=false) private Long studentId;
 @Column(nullable=false) private Long tuitionId;
 @Column(nullable=false) private Long billingRootId;
 @Column(nullable=false) private Long branchId;
 @Column(nullable=false,length=36) private String requestId;
 @Column(nullable=false,length=64) private String fingerprint;
 @Column(nullable=false,length=40) private String merchantRef;
 @Column(nullable=false,length=32) private String merchantCode;
 @Column(nullable=false,length=20) private String provider="VNPAY";
 @Column(nullable=false,length=20) private String environment="SANDBOX";
 @Column(nullable=false,precision=15,scale=2) private BigDecimal amount;
 @Column(nullable=false,length=3) private String currency="VND";
 @Column(nullable=false,length=20) private String state="CREATED";
 @Column(length=60) private String reviewCode;
 private Long receiptId;
 @Column(nullable=false) private LocalDateTime createdAt;
 @Column(nullable=false) private LocalDateTime expiresAt;
 @Column(nullable=false) private LocalDateTime updatedAt;
 @Column(nullable=false,length=45) private String clientIp;
 @Version private long version;
}
