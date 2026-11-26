package com.example.academic.website;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
public interface WebsiteInquiryRepository extends JpaRepository<WebsiteInquiry,String> {
  long countByPhoneHashAndCreatedAtAfter(String phoneHash,LocalDateTime since);
}
