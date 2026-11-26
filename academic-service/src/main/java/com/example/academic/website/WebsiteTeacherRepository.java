package com.example.academic.website;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface WebsiteTeacherRepository extends JpaRepository<WebsiteTeacherProfile,Long> {
  Optional<WebsiteTeacherProfile> findByTeacherId(Long id);
  boolean existsBySlugAndTeacherIdNot(String slug,Long id);
}
