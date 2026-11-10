package com.example.finance.security;

import java.util.List;

public record CurrentUser(
    Long accountId,
    Long profileId,
    Long branchId,
    Role role,
    String username,
    List<Long> branchIds) {
  public CurrentUser(Long accountId, Long profileId, Long branchId, Role role, String username) {
    this(
        accountId,
        profileId,
        branchId,
        role,
        username,
        branchId == null ? List.of() : List.of(branchId));
  }
}
