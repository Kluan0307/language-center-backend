package com.example.academic.lead;

import com.example.academic.common.*;
import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(
    name = "lead_activities",
    indexes = @Index(name = "idx_lead_activity", columnList = "lead_id,id"))
@Getter
@Setter
public class LeadActivity extends BaseEntity {
  @Column(nullable = false, updatable = false)
  private Long leadId;

  @Column(nullable = false, updatable = false)
  private String action;

  @Column(length = 2000, nullable = false, updatable = false)
  private String note;

  @Column(updatable = false)
  private String channel;

  @Column(updatable = false)
  private String oldStatus;

  @Column(nullable = false, updatable = false)
  private String newStatus;

  @Column(updatable = false)
  private Long oldAssigneeId;

  @Column(updatable = false)
  private String oldAssigneeName;

  @Column(nullable = false, updatable = false)
  private Long assigneeId;

  @Column(nullable = false, updatable = false)
  private String assigneeName;

  @Column(updatable = false)
  private LocalDateTime oldNextContactAt;

  @Column(updatable = false)
  private LocalDateTime nextContactAt;

  @Column(nullable = false, updatable = false)
  private Long actorId;

  @Column(nullable = false, updatable = false)
  private String actorName;

  @Column(nullable = false, updatable = false)
  private LocalDateTime occurredAt = LocalDateTime.now(BusinessTime.ZONE);
}
