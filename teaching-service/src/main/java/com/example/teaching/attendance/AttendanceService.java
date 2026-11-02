package com.example.teaching.attendance;

import com.example.teaching.entity.Account;
import com.example.teaching.entity.Attendance;
import com.example.teaching.entity.Enrollment;
import com.example.teaching.entity.SchoolClass;
import com.example.teaching.enrollment.EnrollmentRepository;
import com.example.teaching.schoolclass.SchoolClassRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SchoolClassRepository schoolClassRepository;

    @Transactional(readOnly = true)
    public AttendanceSheetResponse sheet(Long classId, LocalDate date) {
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lớp học với id=" + classId));

        List<Enrollment> enrollments = enrollmentRepository.findBySchoolClass_Id(classId);
        Map<Long, Attendance> saved = attendanceRepository.findByEnrollment_SchoolClass_IdAndSessionDate(classId, date).stream()
                .collect(Collectors.toMap(a -> a.getEnrollment().getId(), Function.identity()));

        List<AttendanceRowResponse> rows = enrollments.stream()
                .map(enrollment -> {
                    Attendance attendance = saved.get(enrollment.getId());
                    String normalizedStatus = attendance == null ? "PRESENT" : normalizeStatus(attendance.getStatus());
                    boolean hasPermission = attendance != null && Boolean.TRUE.equals(attendance.getHasPermission());
                    return new AttendanceRowResponse(
                            enrollment.getId(),
                            enrollment.getStudentId(),
                            "Student-" + enrollment.getStudentId(),
                            normalizedStatus,
                            hasPermission,
                            attendance != null,
                            attendance != null && attendance.getRecordedBy() != null ? attendance.getRecordedBy().getId() : null
                    );
                })
                .toList();

        return new AttendanceSheetResponse(classId, schoolClass.getName(), date, rows);
    }

    @Transactional
    public AttendanceSheetResponse bulk(AttendanceBulkRequest request) {
        SchoolClass schoolClass = schoolClassRepository.findById(request.classId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lớp học với id=" + request.classId()));

        List<Enrollment> enrollments = enrollmentRepository.findBySchoolClass_Id(request.classId());
        Map<Long, Enrollment> activeMap = enrollments.stream()
                .collect(Collectors.toMap(Enrollment::getId, Function.identity()));

        Set<Long> requestedIds = request.records().stream()
                .map(AttendanceItemRequest::enrollmentId)
                .collect(Collectors.toSet());

        if (!requestedIds.equals(activeMap.keySet())) {
            throw new IllegalArgumentException("Phải gửi đầy đủ và chỉ các đăng ký của lớp");
        }

        Map<Long, Attendance> savedMap = attendanceRepository.findByEnrollment_SchoolClass_IdAndSessionDate(request.classId(), request.sessionDate()).stream()
                .collect(Collectors.toMap(a -> a.getEnrollment().getId(), Function.identity()));

        for (AttendanceItemRequest item : request.records()) {
            Enrollment enrollment = activeMap.get(item.enrollmentId());
            if (enrollment == null) {
                throw new EntityNotFoundException("Không tìm thấy đăng ký học với id=" + item.enrollmentId());
            }

            Attendance attendance = savedMap.getOrDefault(item.enrollmentId(), new Attendance());
            attendance.setEnrollment(enrollment);
            attendance.setSessionDate(request.sessionDate());
            attendance.setStatus(normalizeStatus(item.status()));
            attendance.setHasPermission(Boolean.TRUE.equals(item.hasPermission()) || "ABSENT".equalsIgnoreCase(normalizeStatus(item.status())));

            Account recordedBy = new Account();
            recordedBy.setId(1L);
            attendance.setRecordedBy(recordedBy);

            attendanceRepository.save(attendance);
        }

        attendanceRepository.flush();
        return sheet(request.classId(), request.sessionDate());
    }

    @Transactional(readOnly = true)
    public List<AttendanceHistoryResponse> history(Long classId, LocalDate from, LocalDate to) {
        schoolClassRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lớp học với id=" + classId));

        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from không được sau to");
        }

        Map<LocalDate, long[]> counts = new TreeMap<>();
        for (Attendance attendance : attendanceRepository.findByEnrollment_SchoolClass_IdOrderBySessionDateAsc(classId)) {
            LocalDate sessionDate = attendance.getSessionDate();
            if (from != null && sessionDate.isBefore(from)) continue;
            if (to != null && sessionDate.isAfter(to)) continue;
            counts.computeIfAbsent(sessionDate, date -> new long[3])[statusIndex(attendance.getStatus())]++;
        }

        return counts.entrySet().stream()
                .map(entry -> new AttendanceHistoryResponse(entry.getKey(), entry.getValue()[0], entry.getValue()[1], entry.getValue()[2]))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceSummaryResponse> summary(Long classId) {
        schoolClassRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lớp học với id=" + classId));

        Map<Long, long[]> counts = new HashMap<>();
        for (Attendance attendance : attendanceRepository.findByEnrollment_SchoolClass_IdOrderBySessionDateAsc(classId)) {
            counts.computeIfAbsent(attendance.getEnrollment().getId(), id -> new long[3])[statusIndex(attendance.getStatus())]++;
        }

        return enrollmentRepository.findBySchoolClass_Id(classId).stream()
                .map(enrollment -> {
                    long[] value = counts.getOrDefault(enrollment.getId(), new long[3]);
                    long totalSessions = value[0] + value[1] + value[2];
                    BigDecimal participationRate = totalSessions == 0
                            ? BigDecimal.ZERO
                            : BigDecimal.valueOf(value[0] + value[2])
                                .multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(totalSessions), 2, RoundingMode.HALF_UP);

                    return new AttendanceSummaryResponse(
                            enrollment.getId(),
                            enrollment.getStudentId(),
                            "Student-" + enrollment.getStudentId(),
                            value[0],
                            value[1],
                            value[2],
                            totalSessions,
                            participationRate
                    );
                })
                .toList();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "PRESENT";
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("PRESENT".equals(normalized) || "ABSENT".equals(normalized) || "LATE".equals(normalized)) {
            return normalized;
        }
        return "PRESENT";
    }

    private int statusIndex(String status) {
        switch (normalizeStatus(status)) {
            case "PRESENT" -> { return 0; }
            case "ABSENT" -> { return 1; }
            case "LATE" -> { return 2; }
            default -> { return 0; }
        }
    }
}
