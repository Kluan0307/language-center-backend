package com.example.academic.support;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
@Entity @Table(name="student_support_messages",indexes=@Index(name="idx_support_message",columnList="request_id,id")) @Getter @Setter
public class SupportMessage {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="request_id",nullable=false) private SupportRequest request;
 @Column(nullable=false,length=2000) private String message;
 @Column(nullable=false) private boolean internalNote;
 @Column(nullable=false,length=20) private String sender;
 private Long actorId;
 private String actorName;
 @Column(nullable=false) private LocalDateTime createdAt;
}
