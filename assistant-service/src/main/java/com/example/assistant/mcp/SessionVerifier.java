package com.example.assistant.mcp;

import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class SessionVerifier {
  public enum Role {
    SUPER_ADMIN,
    ADMIN,
    STAFF,
    TEACHER
  }

  public enum Mode {
    ACTIVE_BRANCH,
    ALL_ASSIGNED,
    WHOLE_SYSTEM
  }

  public record Identity(
      long accountId,
      Long profileId,
      String username,
      Role role,
      Long primaryBranch,
      List<Long> branches) {}

  public static final class Caller {
    private final String bearer;
    private final Identity identity;
    private final Mode mode;
    private final Long selectedBranch;

    private Caller(String bearer, Identity identity, Mode mode, Long branch) {
      this.bearer = bearer;
      this.identity = identity;
      this.mode = mode;
      selectedBranch = branch;
    }

    Role role() {
      return identity.role();
    }

    Identity identity() {
      return identity;
    }

    Mode mode() {
      return mode;
    }

    Long branch() {
      return selectedBranch;
    }

    String bearer() {
      return bearer;
    }

    List<Long> scopeBranches() {
      return mode == Mode.WHOLE_SYSTEM
          ? List.of()
          : mode == Mode.ALL_ASSIGNED ? identity.branches() : List.of(selectedBranch);
    }

    @Override
    public String toString() {
      return "Authenticated MCP caller [redacted]";
    }
  }

  private final BackendGateway backend;

  public SessionVerifier(BackendGateway backend) {
    this.backend = backend;
  }

  Caller authenticate(String bearer, Mode mode, Long selected) {
    var identity = current(bearer);
    return select(bearer, identity, mode, selected);
  }

  public Identity current(String bearer) {
    if (bearer == null
        || bearer.length() > 8192
        || !bearer.startsWith("Bearer ")
        || bearer.length() < 8
        || bearer.indexOf('\r') >= 0
        || bearer.indexOf('\n') >= 0) throw new McpFault("UNAUTHENTICATED");
    return lookup(bearer);
  }

  private Caller select(String bearer, Identity identity, Mode mode, Long selected) {
    if (mode == null
        || selected != null && selected <= 0
        || mode != Mode.ACTIVE_BRANCH && selected != null) throw new McpFault("INVALID_SCOPE");
    if (mode == Mode.WHOLE_SYSTEM && identity.role() != Role.SUPER_ADMIN
        || mode == Mode.ALL_ASSIGNED && identity.role() != Role.ADMIN)
      throw new McpFault("FORBIDDEN");
    Long branch =
        mode == Mode.ACTIVE_BRANCH
            ? (selected == null ? identity.primaryBranch() : selected)
            : null;
    if (mode == Mode.ACTIVE_BRANCH
        && (branch == null
            || identity.role() != Role.SUPER_ADMIN && !identity.branches().contains(branch)))
      throw new McpFault("FORBIDDEN");
    return new Caller(bearer, identity, mode, branch);
  }

  void revalidate(Caller caller) {
    var fresh = lookup(caller.bearer);
    if (!fresh.equals(caller.identity)) throw new McpFault("SESSION_CHANGED");
  }

  private Identity lookup(String bearer) {
    var row = backend.get(BackendGateway.Target.AUTH, "/api/v1/auth/me", Map.of(), bearer, null);
    try {
      if (!Boolean.TRUE.equals(row.get("status"))) throw new McpFault("UNAUTHENTICATED");
      if (!Boolean.FALSE.equals(row.get("mustChangePassword")))
        throw new McpFault("PASSWORD_CHANGE_REQUIRED");
      long account = positive(row.get("accountId"));
      Long profile = row.get("profileId") == null ? null : positive(row.get("profileId"));
      Long branch = row.get("branchId") == null ? null : positive(row.get("branchId"));
      Role role = Role.valueOf((String) row.get("role"));
      String name = (String) row.get("username");
      if (name == null || name.isBlank()) throw new McpFault("UNAUTHENTICATED");
      if (!(row.get("branchIds") instanceof List<?> ids) || ids.size() > 1000)
        throw new McpFault("UNAUTHENTICATED");
      var branches = ids.stream().map(this::positive).distinct().sorted().toList();
      if (role != Role.SUPER_ADMIN
          && (branches.isEmpty() || branch == null || !branches.contains(branch)))
        throw new McpFault("FORBIDDEN");
      if (role == Role.TEACHER && profile == null) throw new McpFault("FORBIDDEN");
      return new Identity(account, profile, name, role, branch, branches);
    } catch (McpFault e) {
      throw e;
    } catch (Exception e) {
      throw new McpFault("UNAUTHENTICATED");
    }
  }

  private long positive(Object value) {
    try {
      long n = new java.math.BigDecimal(value.toString()).longValueExact();
      if (n <= 0) throw new ArithmeticException();
      return n;
    } catch (Exception e) {
      throw new McpFault("UNAUTHENTICATED");
    }
  }
}
