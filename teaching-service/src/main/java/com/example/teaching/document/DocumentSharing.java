package com.example.teaching.document;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
@Entity @Table(name="document_sharing_events") @Getter @Setter
public class DocumentSharing {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Long documentId; private int revision; private boolean sharedToStudents; private Long actorId;
 @Column(nullable=false,length=500) private String reason;
 private LocalDateTime occurredAt;
}
