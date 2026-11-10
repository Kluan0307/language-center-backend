package com.example.teaching.document;

import org.springframework.data.jpa.repository.*;

public interface DocumentRepository
    extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from Document d where d.id=:id")
  java.util.Optional<Document> lock(@org.springframework.data.repository.query.Param("id") Long id);
}
