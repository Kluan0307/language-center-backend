package com.example.auth.security;

import com.example.auth.entity.Account;
import com.example.auth.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final AccountRepository accountRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Account account =
        accountRepository
            .findByUsername(username)
            .orElseThrow(
                () -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));

    return new AccountPrincipal(account);
  }
}
