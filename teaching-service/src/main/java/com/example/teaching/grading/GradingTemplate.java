package com.example.teaching.grading;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
@Entity @Table(name="grading_templates",uniqueConstraints=@UniqueConstraint(name="uk_grading_template_revision",columnNames={"code","revision"}))
@Getter @Setter
public class GradingTemplate {
 @Id @Column(length=60) private String id;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30,updatable=false) private GradingModels.Kind code;
 @Column(nullable=false,updatable=false) private long revision;
 @Column(nullable=false,length=160,updatable=false) private String name;
 @Column(nullable=false,columnDefinition="LONGTEXT",updatable=false) private String specification;
 @Column(nullable=false,updatable=false) private Long createdBy;
 @Column(nullable=false,updatable=false) private LocalDateTime createdAt;
 @Column(nullable=false,length=500,updatable=false) private String reason;
}
