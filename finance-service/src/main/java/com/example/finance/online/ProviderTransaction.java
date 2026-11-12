package com.example.finance.online;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;
@Entity @Table(name="payment_provider_transactions",uniqueConstraints={@UniqueConstraint(columnNames={"merchant_code","transaction_no"}),@UniqueConstraint(columnNames="receipt_id")}) @Getter @Setter
public class ProviderTransaction {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=32) private String merchantCode;
 @Column(nullable=false,length=32) private String transactionNo;
 @Column(length=36) private String orderId;
 @Column(nullable=false,precision=15,scale=2) private BigDecimal amount;
 private LocalDateTime paidAt;
 @Column(nullable=false,length=20) private String state="REVIEW_REQUIRED";
 @Column(length=60) private String reviewCode;
 private Long receiptId;
 @Column(nullable=false) private LocalDateTime createdAt;
 @Version private long version;
}
