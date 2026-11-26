package com.example.academic.website;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
@Entity @Table(name="website_teacher_profiles",uniqueConstraints={
  @UniqueConstraint(name="uk_website_teacher_source",columnNames="teacher_id"),
  @UniqueConstraint(name="uk_website_teacher_slug",columnNames="slug")})
@Getter @Setter
public class WebsiteTeacherProfile {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Version private long version;
  @Column(nullable=false) private Long teacherId;
  @Column(nullable=false,length=120) private String slug;
  @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private WebsitePublication.State state=WebsitePublication.State.DRAFT;
  @Column(length=2000) private String biography;
  @Column(length=500) private String teachingFocus;
  @Column(length=1000) private String imageUrl;
  @Column(nullable=false) private boolean showQualification;
  @Column(nullable=false) private LocalDateTime updatedAt;
  @Column(nullable=false) private Long updatedBy;
}
