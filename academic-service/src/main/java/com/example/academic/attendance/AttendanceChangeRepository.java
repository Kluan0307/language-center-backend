package com.example.academic.attendance;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceChangeRepository extends JpaRepository<AttendanceChange, Long> {
  List<AttendanceChange> findBySessionIdOrderByIdDesc(Long id);
}
