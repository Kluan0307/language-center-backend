package com.example.academic.attendance;

import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
  interface ProfileCount {Long getEnrollmentId();String getStatus();long getTotal();}
 @org.springframework.data.jpa.repository.Query("select a.enrollment.id as enrollmentId,cast(a.status as string) as status,count(a) as total from Attendance a where a.enrollment.student.id=:studentId group by a.enrollment.id,a.status")
 List<ProfileCount> profileCounts(@org.springframework.data.repository.query.Param("studentId")Long studentId);
  boolean existsByEnrollmentSchoolClassIdAndSessionDate(Long classId,LocalDate date);

  List<Attendance> findByEnrollmentSchoolClassIdAndSessionDate(Long classId, LocalDate date);

  List<Attendance> findByEnrollmentSchoolClassIdOrderBySessionDateAsc(Long classId);

 interface EvaluationMark {Long getEnrollmentId();Long getSessionId();java.time.LocalDate getSessionDate();String getStatus();boolean getHasPermission();}
 @org.springframework.data.jpa.repository.Query("select a.enrollment.id as enrollmentId,s.id as sessionId,a.sessionDate as sessionDate,cast(a.status as string) as status,a.hasPermission as hasPermission from Attendance a left join a.classSession s where a.enrollment.schoolClass.id=:classId")
 java.util.List<EvaluationMark> evaluationMarks(@org.springframework.data.repository.query.Param("classId") Long classId);
}
