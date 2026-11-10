package com.example.academic.results;

public interface ClassClosureEventRepository
    extends org.springframework.data.jpa.repository.JpaRepository<ClassClosureEvent, String> {
  java.util.List<ClassClosureEvent> findByClassIdOrderByRevisionDesc(Long id);
}
