package com.example.teaching.grading;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
@Entity @Table(name="grading_releases",uniqueConstraints={@UniqueConstraint(name="uk_grading_release_revision",columnNames={"class_id","revision"}),@UniqueConstraint(name="uk_grading_release_request",columnNames="request_id")}) @Getter @Setter
public class GradingRelease {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,updatable=false) private Long classId;
 @Column(nullable=false,updatable=false) private long revision;
 @Column(nullable=false,length=20,updatable=false) private String state;
 @Column(nullable=false,length=20,updatable=false) private String action;
 @Column(nullable=false,length=36,updatable=false) private String requestId;
 @Column(nullable=false,length=64,updatable=false) private String intent;
 @Column(nullable=false,length=64,updatable=false) private String fingerprint;
 @Column(nullable=false,columnDefinition="LONGTEXT",updatable=false) private String snapshot;
 @Column(nullable=false,updatable=false) private Long actorId;
 @Column(nullable=false,updatable=false) private Long submittedBy;
 @Column(nullable=false,length=500,updatable=false) private String reason;
 @Column(nullable=false,updatable=false) private LocalDateTime occurredAt;
}
