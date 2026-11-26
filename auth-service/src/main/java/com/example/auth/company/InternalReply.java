package com.example.auth.company;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "internal_support_replies", indexes = @Index(columnList = "ticket_id,id"))
@Getter
@Setter
public class InternalReply {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long ticketId;

  @Column(nullable = false)
  private Long actorId;

  @Column(nullable = false, length = 2000)
  private String message;

  @Column(nullable = false, length = 20)
  private String statusAfter;

  @Column(nullable = false)
  private LocalDateTime createdAt;
}
