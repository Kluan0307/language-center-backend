package com.example.teaching.publication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface GradePublicationRepository extends JpaRepository<GradePublication,Long> {
 Optional<GradePublication> findFirstByExamIdOrderByRevisionDesc(Long id);
 Optional<GradePublication> findByRequestId(String id);
 boolean existsByExamId(Long id);
}
