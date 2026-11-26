package com.example.academic.website;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface WebsitePublicationRepository extends JpaRepository<WebsitePublication,Long> {
  Optional<WebsitePublication> findByKindAndSourceId(WebsitePublication.Kind kind,Long sourceId);
  Optional<WebsitePublication> findByKindAndSlug(WebsitePublication.Kind kind,String slug);
  boolean existsByKindAndSlugAndSourceIdNot(WebsitePublication.Kind kind,String slug,Long sourceId);
}
