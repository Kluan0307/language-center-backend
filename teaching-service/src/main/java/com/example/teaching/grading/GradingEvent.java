package com.example.teaching.grading;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
@Entity @Table(name="grading_events",indexes=@Index(name="idx_grading_event_class",columnList="class_id,id")) @Getter @Setter
public class GradingEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Long classId;
 @Column(nullable=false,length=40) private String kind;
 @Column(nullable=false) private Long actorId;
 @Column(nullable=false) private LocalDateTime occurredAt;
 @Column(nullable=false,length=500) private String reason;
 @Column(columnDefinition="LONGTEXT") private String beforeValue;
 @Column(nullable=false,columnDefinition="LONGTEXT") private String afterValue;
}
