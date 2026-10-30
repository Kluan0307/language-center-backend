package com.example.teaching.schedule;

import com.example.teaching.entity.Schedule;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleRequest(
        @NotNull(message = "schoolClassId không được để trống")
        Long schoolClassId,

        @NotNull(message = "roomId không được để trống")
        Long roomId,

        Long teacherAccountId,

        @NotNull(message = "dayOfWeek không được để trống")
        DayOfWeek dayOfWeek,

        @NotNull(message = "startTime không được để trống")
        LocalTime startTime,

        @NotNull(message = "endTime không được để trống")
        LocalTime endTime
) {
    public static ScheduleRequest fromEntity(Schedule schedule) {
        return new ScheduleRequest(
                schedule.getSchoolClass() != null ? schedule.getSchoolClass().getId() : null,
                schedule.getRoom() != null ? schedule.getRoom().getId() : null,
                schedule.getTeacherAccountId(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime()
        );
    }
}
