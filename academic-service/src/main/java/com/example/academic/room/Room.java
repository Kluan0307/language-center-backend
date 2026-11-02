package com.example.academic.room;

import com.example.academic.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "rooms",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_room_code_branch",
            columnNames = {"code", "branch_id"}))
@Getter
@Setter
public class Room extends BaseEntity {
  @Column(nullable = false)
  private String code;

  @Column(nullable = false)
  private String name;

  private String location;

  @Column(nullable = false)
  private Integer capacity;

  @Column(columnDefinition = "text")
  private String equipment;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.AVAILABLE;

  @Column(nullable = false)
  private Long branchId;

  public enum Status {
    AVAILABLE,
    MAINTENANCE
  }
}
