package com.example.teaching.grading;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="grading_course_defaults",uniqueConstraints=@UniqueConstraint(name="uk_grading_course_branch",columnNames={"course_id","branch_id"}))
@Getter @Setter
public class GradingCourseDefault {
 @Id @Column(length=80) private String id;
 @Column(nullable=false) private Long courseId;
 @Column(nullable=false) private Long branchId;
 @Column(nullable=false,length=60) private String templateId;
 @Column(nullable=false) private long revision;
 @Version private long version;
}
