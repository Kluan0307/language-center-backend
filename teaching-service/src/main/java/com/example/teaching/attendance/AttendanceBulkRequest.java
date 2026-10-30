package com.example.teaching.attendance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

public record AttendanceBulkRequest(
        @NotNull(message = "classId không được để trống")
        @Positive(message = "classId phải lớn hơn 0")
        Long classId,

        @NotNull(message = "sessionDate không được để trống")
        @PastOrPresent(message = "sessionDate không được ở tương lai")
        LocalDate sessionDate,

        @NotNull(message = "records không được để trống")
        List<@NotNull @Valid AttendanceItemRequest> records
) {
}

record AttendanceItemRequest(
        @NotNull(message = "enrollmentId không được để trống")
        @Positive(message = "enrollmentId phải lớn hơn 0")
        Long enrollmentId,

        @NotBlank(message = "status không được để trống")
        String status,

        Boolean hasPermission
) {
}
