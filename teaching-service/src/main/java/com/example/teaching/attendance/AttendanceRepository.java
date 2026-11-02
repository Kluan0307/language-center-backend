package com.example.teaching.attendance;

import com.example.teaching.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByEnrollment_SchoolClass_IdAndSessionDate(Long schoolClassId, LocalDate sessionDate);

    List<Attendance> findByEnrollment_SchoolClass_IdOrderBySessionDateAsc(Long schoolClassId);
}
