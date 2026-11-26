package com.example.teaching.grading;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;
@Entity @Table(name="grading_marks",uniqueConstraints=@UniqueConstraint(name="uk_grading_mark",columnNames={"activity_id","student_id","component"}))
@Getter @Setter
public class GradingMark {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long activityId;
 @Column(nullable=false) private Long studentId;
 @Column(nullable=false,length=40) private String component;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private GradingModels.Status status;
 @Column(precision=7,scale=2) private BigDecimal score;
 private Integer rawScore;
 private Long conversionId;
 @Enumerated(EnumType.STRING) @Column(length=30) private GradingModels.Level level;
 @Column(length=1000) private String comment;
 @Column(nullable=false) private Long enteredBy;
 @Column(nullable=false) private LocalDateTime updatedAt;
 @Version private long version;
}
