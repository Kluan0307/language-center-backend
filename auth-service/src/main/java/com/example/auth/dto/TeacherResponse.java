package com.example.auth.dto;

public record TeacherResponse(
    Long profileId,
    Long accountId,
    String fullName,
    Long branchId,
    java.util.List<Long> branchIds,
    String personnelCode,
    java.time.LocalDate hireDate,
    java.time.LocalDate endDate) {}
