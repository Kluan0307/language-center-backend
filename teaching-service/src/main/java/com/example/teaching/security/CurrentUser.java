package com.example.teaching.security;

public record CurrentUser(
        Long accountId,
        Long profileId,
        Long branchId,
        Role role,
        String username) {
}
