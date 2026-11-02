package com.example.teaching.attendance;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/teaching/attendances")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/sheet")
    public AttendanceSheetResponse sheet(
            @RequestParam Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return attendanceService.sheet(classId, date);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public AttendanceSheetResponse bulk(@Valid @RequestBody AttendanceBulkRequest request) {
        return attendanceService.bulk(request);
    }

    @GetMapping("/history")
    public List<AttendanceHistoryResponse> history(
            @RequestParam Long classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return attendanceService.history(classId, from, to);
    }

    @GetMapping("/summary")
    public List<AttendanceSummaryResponse> summary(@RequestParam Long classId) {
        return attendanceService.summary(classId);
    }
}
