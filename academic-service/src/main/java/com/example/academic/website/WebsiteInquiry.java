package com.example.academic.website;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Consent and idempotency receipt. Public responses never reveal this row or the lead. */
@Entity
@Table(name="website_inquiries",indexes={@Index(name="idx_website_inquiry_phone",columnList="phone_hash,created_at")})
@Getter @Setter
public class WebsiteInquiry {
  @Id @Column(length=36) private String requestId;
  @Column(nullable=false,length=64) private String fingerprint;
  @Column(nullable=false,length=64) private String phoneHash;
  @Column(nullable=false) private Long leadId;
  @Column(nullable=false) private Long branchId;
  @Column(length=120) private String programSlug;
  @Column(length=120) private String courseSlug;
  @Column(length=120) private String classSlug;
  @Column(nullable=false,length=50) private String consentVersion;
  @Column(nullable=false) private LocalDateTime createdAt;
}
