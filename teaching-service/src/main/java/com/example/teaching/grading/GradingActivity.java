package com.example.teaching.grading;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.*;
@Entity @Table(name="grading_activities",indexes=@Index(name="idx_grading_activity_class",columnList="class_id"))
@Getter @Setter
public class GradingActivity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Version private long version;
 @Column(nullable=false) private Long classId;
 @Column(nullable=false,length=160) private String name;
 @Column(nullable=false) private LocalDate assessmentDate;
 @Column(nullable=false,columnDefinition="TEXT") private String components;
 private LocalDateTime rosterFrozenAt;
 private Long retakeOf;
 private Long conversionId;
 @Column(nullable=false) private Long createdBy;
 @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="grading_candidates",joinColumns=@JoinColumn(name="activity_id"),uniqueConstraints=@UniqueConstraint(name="uk_grading_candidate",columnNames={"activity_id","student_id"}))
 @OrderBy("studentId ASC") private List<Candidate> candidates=new ArrayList<>();
 @Embeddable @Getter @Setter @NoArgsConstructor @AllArgsConstructor
 public static class Candidate {
  @Column(nullable=false) private Long studentId;
  @Column(nullable=false) private String studentName;
  @Column(nullable=false) private Long enrollmentId;
 }
}
