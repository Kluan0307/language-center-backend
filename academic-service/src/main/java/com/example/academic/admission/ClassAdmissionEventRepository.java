package com.example.academic.admission;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassAdmissionEventRepository extends JpaRepository<ClassAdmissionEvent, Long> {
  List<ClassAdmissionEvent> findByClassIdOrderByIdDesc(Long classId);
}
