package com.example.academic.media;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Table(name="media_assets",indexes=@Index(name="idx_media_owner",columnList="uploaded_by,id")) @Getter @Setter
public class MediaAsset {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=200) private String publicId;
 @Column(nullable=false,length=200) private String assetId;
 @Column(nullable=false,unique=true,length=512) private String url;
 @Column(nullable=false,length=20) private String category;
 @Column(nullable=false) private Long uploadedBy;
 private Long branchId;
 @Column(nullable=false) private long bytes;
 @Column(nullable=false) private int width;
 @Column(nullable=false) private int height;
 @Column(nullable=false,length=20) private String format;
 @Column(nullable=false) private LocalDateTime createdAt;
 @Column(nullable=false) private boolean deleted;
}
