package com.example.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.auth.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

class DisabledTokenUnitTest {
  @Test
  void disabledUserCannotAuthenticateWithAnExistingToken() throws Exception {
    var jwt = mock(JwtUtil.class);
    var users = mock(CustomUserDetailsService.class);
    when(jwt.extractUsername("old")).thenReturn("staff");
    when(jwt.validateToken("old", "staff")).thenReturn(true);
    when(users.loadUserByUsername("staff"))
        .thenReturn(
            User.withUsername("staff").password("hash").roles("STAFF").disabled(true).build());
    var req = new MockHttpServletRequest();
    req.addHeader("Authorization", "Bearer old");
    SecurityContextHolder.clearContext();
    new JwtAuthenticationFilter(jwt, users)
        .doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }
}
