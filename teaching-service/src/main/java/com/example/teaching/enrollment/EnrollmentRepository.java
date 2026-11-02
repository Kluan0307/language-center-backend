package com.example.teaching.enrollment;

import com.example.teaching.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findBySchoolClass_Id(Long schoolClassId);
}
