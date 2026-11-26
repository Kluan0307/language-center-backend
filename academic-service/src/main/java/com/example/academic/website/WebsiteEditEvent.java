package com.example.academic.website;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
@Entity @Table(name="website_edit_events",indexes=@Index(columnList="kind,source_id,id")) @Getter @Setter
public class WebsiteEditEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=20) private String kind;
 @Column(nullable=false) private Long sourceId;
 @Column(nullable=false) private Long actorId;
 @Column(nullable=false) private String actorName;
 @Column(nullable=false) private LocalDateTime occurredAt;
 @Column(nullable=false,length=20) private String state;
 @Column(nullable=false,length=16000) private String snapshot;
}
