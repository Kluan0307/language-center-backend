package com.example.academic.schedule;

import com.example.academic.common.BaseEntity;
import com.example.academic.schoolclass.SchoolClass;
import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(
    name = "schedules",
    indexes = @Index(name = "idx_schedule_class_day", columnList = "class_id,day_of_week"))
@Getter
@Setter
public class Schedule extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "class_id", nullable = false)
  private SchoolClass schoolClass;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DayOfWeek dayOfWeek;

  @Column(nullable = false)
  private LocalTime startTime;

  @Column(nullable = false)
  private LocalTime endTime;
}
