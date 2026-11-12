package com.example.teaching.publication;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;
@Entity @Table(name="published_grades",uniqueConstraints=@UniqueConstraint(columnNames={"publication_id","student_id","enrollment_id"})) @Getter @Setter
public class PublishedGrade {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="publication_id",nullable=false) private GradePublication publication;
 @Column(nullable=false) private Long studentId;
 @Column(nullable=false) private Long enrollmentId;
 @Column(precision=7,scale=2) private BigDecimal score;
 @Column(length=1000) private String studentComment;
}
