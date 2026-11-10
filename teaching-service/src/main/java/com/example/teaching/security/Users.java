package com.example.teaching.security;

import com.example.teaching.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

public final class Users {
  private Users() {}

  public static CurrentUser current() {
    var a = SecurityContextHolder.getContext().getAuthentication();
    if (a == null || !(a.getPrincipal() instanceof CurrentUser u))
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập");
    return u;
  }
}
