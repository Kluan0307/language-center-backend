package com.example.auth.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record BranchAssignmentsRequest(
    @NotNull @Size(max = 100) List<@NotNull @Positive Long> branchIds,
    @Positive Long primaryBranchId) {}
