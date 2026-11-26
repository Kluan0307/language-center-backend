package com.example.auth.company;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "internal_support_tickets", indexes = @Index(columnList = "owner_id,branch_id"))
@Getter
@Setter
public class InternalTicket {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @Column(nullable = false)
  private Long ownerId;

  private Long branchId;

  @Column(nullable = false, length = 30)
  private String category;

  @Column(nullable = false, length = 120)
  private String title;

  @Column(nullable = false, length = 3000)
  private String description;

  @Column(nullable = false, length = 20)
  private String status = "OPEN";

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;
}
