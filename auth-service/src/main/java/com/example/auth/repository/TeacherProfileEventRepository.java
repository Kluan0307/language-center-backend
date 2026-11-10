package com.example.auth.repository;

import com.example.auth.entity.TeacherProfileEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherProfileEventRepository extends JpaRepository<TeacherProfileEvent, Long> {
  List<TeacherProfileEvent> findByTeacherIdOrderByIdDesc(Long teacherId);
}
