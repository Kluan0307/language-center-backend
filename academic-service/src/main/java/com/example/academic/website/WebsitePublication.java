package com.example.academic.website;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Editorial metadata only. Names, fees and dates remain in their operational source. */
@Entity
@Table(name="website_publications", indexes=@Index(name="idx_website_public",columnList="kind,state,source_id"), uniqueConstraints={
    @UniqueConstraint(name="uk_website_source",columnNames={"kind","source_id"}),
    @UniqueConstraint(name="uk_website_slug",columnNames={"kind","slug"})})
@Getter @Setter
public class WebsitePublication {
  public enum Kind { PROGRAM, COURSE, BRANCH, CLASS }
  public enum State { DRAFT, PUBLISHED }
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Version private long version;
  @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Kind kind;
  @Column(nullable=false) private Long sourceId;
  @Column(nullable=false,length=120) private String slug;
  @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private State state=State.DRAFT;
  @Column(length=2000) private String summary;
  @Column(length=2000) private String audience;
  @Column(length=4000) private String outcomes;
  @Column(length=4000) private String topics;
  @Column(length=1000) private String imageUrl;
  @Column(length=500) private String openingHours;
  @Column(length=1000) private String mapUrl;
  private Long programId;
  @Column(nullable=false) private boolean showTuition;
  @Column(nullable=false) private LocalDateTime updatedAt;
  @Column(nullable=false) private Long updatedBy;
}
