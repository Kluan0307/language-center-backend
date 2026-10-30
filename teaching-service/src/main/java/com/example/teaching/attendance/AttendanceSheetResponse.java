package com.example.teaching.attendance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AttendanceSheetResponse(
        Long classId,
        String className,
        LocalDate sessionDate,
        List<AttendanceRowResponse> records
) {
}

record AttendanceRowResponse(
        Long enrollmentId,
        Long studentId,
        String studentName,
        String status,
        boolean hasPermission,
        boolean saved,
        Long recordedByAccountId
) {
}

record AttendanceHistoryResponse(
        LocalDate sessionDate,
        long present,
        long absent,
        long late
) {
}

record AttendanceSummaryResponse(
        Long enrollmentId,
        Long studentId,
        String studentName,
        long present,
        long absent,
        long late,
        long totalSessions,
        BigDecimal participationRate
) {
}
