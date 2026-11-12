package com.example.teaching.publication;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

/** Append-only publication operation. The highest revision decides current visibility. */
@Entity @Table(name="grade_publications",uniqueConstraints={@UniqueConstraint(columnNames={"exam_id","revision"}),@UniqueConstraint(columnNames="request_id")}) @Getter @Setter
public class GradePublication {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="exam_id",nullable=false) private com.example.teaching.exam.Exam exam;
 private Long classId;
 private Long branchId;
 private long revision;
 @Column(nullable=false,length=36) private String requestId;
 @Column(nullable=false,length=64) private String fingerprint;
 @Column(nullable=false,length=64) private String draftFingerprint;
 @Column(nullable=false,length=20) private String action;
 @Column(nullable=false,length=500) private String reason;
 private Long publishedBy;
 private LocalDateTime publishedAt;
 private String examName;
 private String examType;
 private LocalDate examDate;
 @Column(precision=7,scale=2) private BigDecimal maxScore;
 @Column(precision=7,scale=2) private BigDecimal weight;
}
