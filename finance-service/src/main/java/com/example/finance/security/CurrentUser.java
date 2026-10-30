package com.example.finance.security;

public record CurrentUser(
    Long accountId, Long profileId, Long branchId, Role role, String username) {}
