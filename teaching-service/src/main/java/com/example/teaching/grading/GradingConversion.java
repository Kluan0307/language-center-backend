package com.example.teaching.grading;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
@Entity @Table(name="grading_conversions",uniqueConstraints=@UniqueConstraint(name="uk_grading_conversion_revision",columnNames={"code","revision"})) @Getter @Setter
public class GradingConversion {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=40,updatable=false) private String code;
 @Column(nullable=false,updatable=false) private long revision;
 @Column(nullable=false,length=160,updatable=false) private String name;
 @Column(nullable=false,length=1000,updatable=false) private String reference;
 @Column(nullable=false,columnDefinition="LONGTEXT",updatable=false) private String mapping;
 @Column(nullable=false,updatable=false) private Long createdBy;
 @Column(nullable=false,updatable=false) private LocalDateTime createdAt;
}
