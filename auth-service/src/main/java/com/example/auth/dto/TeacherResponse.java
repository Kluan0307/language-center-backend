package com.example.auth.dto;

public record TeacherResponse(
    Long profileId,
    Long accountId,
    String fullName,
    Long branchId,
    java.util.List<Long> branchIds) {}
