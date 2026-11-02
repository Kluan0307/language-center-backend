package com.example.teaching.schedule;

import com.example.teaching.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByRoom_IdAndDayOfWeek(Long roomId, DayOfWeek dayOfWeek);

    List<Schedule> findBySchoolClass_IdAndDayOfWeek(Long schoolClassId, DayOfWeek dayOfWeek);

    List<Schedule> findByTeacherAccountId(Long teacherAccountId);
}
