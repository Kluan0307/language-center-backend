package com.example.academic.enrollment;

import java.math.BigDecimal;

public record EnrollmentCreated(
    Long enrollmentId, Long studentId, Long classId, BigDecimal tuitionFee) {}
