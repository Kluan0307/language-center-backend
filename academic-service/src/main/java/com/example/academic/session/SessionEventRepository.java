package com.example.academic.session;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionEventRepository extends JpaRepository<SessionEvent, Long> {
  List<SessionEvent> findBySessionIdOrderByIdDesc(Long id);
}
