package com.example.teaching.grading;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
@Entity @Table(name="grading_legacy_imports",uniqueConstraints={@UniqueConstraint(name="uk_grading_import_class",columnNames="class_id"),@UniqueConstraint(name="uk_grading_import_request",columnNames="request_id")}) @Getter @Setter
public class GradingLegacyImport {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,updatable=false) private Long classId;
 @Column(nullable=false,length=36,updatable=false) private String requestId;
 @Column(nullable=false,length=64,updatable=false) private String fingerprint;
 @Column(nullable=false,length=64,updatable=false) private String intent;
 @Column(nullable=false,length=60,updatable=false) private String templateId;
 @Column(nullable=false,columnDefinition="LONGTEXT",updatable=false) private String snapshot;
 @Column(nullable=false,length=500,updatable=false) private String reason;
 @Column(nullable=false,updatable=false) private Long actorId;
 @Column(nullable=false,updatable=false) private LocalDateTime createdAt;
}
