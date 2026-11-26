package com.example.teaching.grading;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
@Entity @Table(name="grading_completion_batches") @Getter @Setter
public class GradingCompletionBatch {
 @Id @Column(length=36) private String requestId;
 @Column(nullable=false) private Long classId;
 @Column(nullable=false) private Long releaseId;
 @Column(nullable=false,length=64) private String intent;
 @Column(nullable=false) private Long actorId;
 @Column(nullable=false) private LocalDateTime createdAt;
 @Column(nullable=false,length=500) private String reason;
 @Column(nullable=false,columnDefinition="LONGTEXT") private String snapshot;
}
