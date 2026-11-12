package com.example.academic.support;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
@Entity @Table(name="student_support_operations") @Getter @Setter
public class SupportOperation {
 @Id @Column(length=36) private String id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="request_id",nullable=false) private SupportRequest request;
 @Column(nullable=false) private Long actorId;
 @Column(nullable=false,length=64) private String fingerprint;
 @Column(nullable=false,length=20) private String action;
 @Column(nullable=false) private LocalDateTime createdAt;
}
