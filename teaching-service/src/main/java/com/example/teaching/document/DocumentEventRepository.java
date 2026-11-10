package com.example.teaching.document;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentEventRepository extends JpaRepository<DocumentEvent, Long> {
  List<DocumentEvent> findByDocumentIdOrderByIdDesc(Long id);

  boolean existsByDocumentId(Long id);

  Optional<DocumentEvent> findFirstByDocumentIdAndRevisionOrderByIdDesc(Long id, int revision);
}
