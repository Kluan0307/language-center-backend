package com.example.academic.attendance;

import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
  List<Attendance> findByEnrollmentSchoolClassIdAndSessionDate(Long classId, LocalDate date);

  List<Attendance> findByEnrollmentSchoolClassIdOrderBySessionDateAsc(Long classId);
}
