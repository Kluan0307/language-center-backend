package com.example.auth.dto;

import java.time.LocalDateTime;

public record BranchAssignmentResponse(
    Long branchId,
    boolean status,
    boolean isPrimary,
    LocalDateTime assignedAt,
    Long assignedBy,
    LocalDateTime revokedAt) {}
