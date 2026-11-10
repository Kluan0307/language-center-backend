package com.example.auth.security;

import com.example.auth.entity.Account;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class AccountPrincipal extends User {
  private final Long accountId;
  private final long sessionVersion;
  private final boolean mustChangePassword;

  public AccountPrincipal(Account account) {
    super(
        account.getUsername(),
        account.getPassword(),
        Boolean.TRUE.equals(account.getStatus()),
        true,
        true,
        true,
        List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name())));
    accountId = account.getId();
    sessionVersion = account.getSessionVersion();
    mustChangePassword = account.isMustChangePassword();
  }

  public Long accountId() {
    return accountId;
  }

  public long sessionVersion() {
    return sessionVersion;
  }

  public boolean mustChangePassword() {
    return mustChangePassword;
  }
}
