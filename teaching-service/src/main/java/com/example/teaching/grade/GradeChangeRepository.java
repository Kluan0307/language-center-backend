package com.example.teaching.grade;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeChangeRepository extends JpaRepository<GradeChange, Long> {
  List<GradeChange> findByGradeIdOrderByModifiedAtAsc(Long gradeId);
}
