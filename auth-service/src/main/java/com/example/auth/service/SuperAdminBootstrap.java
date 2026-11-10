package com.example.auth.service;

import com.example.auth.entity.*;
import com.example.auth.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "auth.bootstrap.enabled", havingValue = "true")
public class SuperAdminBootstrap implements CommandLineRunner {
  private final AccountRepository accounts;
  private final SuperAdminRepository profiles;
  private final PasswordEncoder passwords;

  @Value("${auth.bootstrap.username:}")
  private String username;

  @Value("${auth.bootstrap.password:}")
  private String password;

  @Override
  @Transactional
  public void run(String... args) {
    if (accounts.count() != 0) return;
    if (!username.matches("[A-Za-z0-9_.@-]{3,50}")
        || password.length() < 12
        || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new IllegalStateException(
          "Provide a valid bootstrap username and a password of 12-72 UTF-8 bytes");
    Account account =
        accounts.save(
            Account.builder()
                .username(username)
                .password(passwords.encode(password))
                .role(Role.SUPER_ADMIN)
                .status(true)
                .build());
    profiles.save(SuperAdmin.builder().account(account).fullName("Super Admin").build());
  }
}
