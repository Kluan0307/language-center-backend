package com.example.auth.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
@Entity @Table(name="employment_events",indexes=@Index(name="idx_employment_account",columnList="account_id,id")) @Getter @Setter
public class EmploymentEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long accountId;
 @Column(nullable=false) private Long actorId;
 @Column(nullable=false,length=1000) private String beforeState;
 @Column(nullable=false,length=1000) private String afterState;
 @Column(nullable=false,length=255) private String reason;
 @Column(nullable=false) private LocalDateTime occurredAt;
}
