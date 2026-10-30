package com.example.teaching.schedule;

import com.example.teaching.entity.Schedule;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleResponse(
        Long id,
        Long schoolClassId,
        Long roomId,
        Long teacherAccountId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
    public static ScheduleResponse fromEntity(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getSchoolClass() != null ? schedule.getSchoolClass().getId() : null,
                schedule.getRoom() != null ? schedule.getRoom().getId() : null,
                schedule.getTeacherAccountId(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime()
        );
    }
}
