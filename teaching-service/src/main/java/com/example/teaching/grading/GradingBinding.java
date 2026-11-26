package com.example.teaching.grading;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
@Entity @Table(name="grading_class_bindings") @Getter @Setter
public class GradingBinding {
 @Id private Long classId;
 @Column(nullable=false) private Long branchId;
 @Column(nullable=false) private Long courseId;
 @Column(nullable=false) private long revision;
 @Column(nullable=false,length=60) private String templateId;
 @Column(nullable=false,length=160) private String name;
 @Column(nullable=false,columnDefinition="LONGTEXT") private String specification;
 @Column(nullable=false) private boolean locked;
 @Column(nullable=false) private Long assignedBy;
 @Column(nullable=false) private LocalDateTime assignedAt;
 @Version private long version;
}
