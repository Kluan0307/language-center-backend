package com.example.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.auth.entity.*;
import com.example.auth.security.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class SessionVersionFilterTest {
  JwtUtil jwt;
  CustomUserDetailsService users;
  Account account;

  @BeforeEach
  void setup() {
    jwt = new JwtUtil();
    ReflectionTestUtils.setField(jwt, "secret", "test-only-secret-at-least-thirty-two-bytes");
    ReflectionTestUtils.setField(jwt, "expiration", 60000L);
    users = mock(CustomUserDetailsService.class);
    account =
        Account.builder()
            .id(1L)
            .username("staff")
            .password("hash")
            .role(Role.STAFF)
            .status(true)
            .branchId(11L)
            .build();
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  MockHttpServletResponse request(String method, String path, long version) throws Exception {
    when(users.loadUserByUsername("staff")).thenReturn(new AccountPrincipal(account));
    var req = new MockHttpServletRequest(method, path);
    req.addHeader(
        "Authorization", "Bearer " + jwt.generateToken("staff", "STAFF", 1L, 2L, 11L, version));
    var res = new MockHttpServletResponse();
    new JwtAuthenticationFilter(jwt, users).doFilter(req, res, (r, s) -> {});
    return res;
  }

  @Test
  void matchingVersionAuthenticates() throws Exception {
    request("GET", "/api/v1/auth/me", 0);
    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void olderVersionDoesNotAuthenticate() throws Exception {
    account.setSessionVersion(1);
    request("GET", "/api/v1/auth/me", 0);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void forcedChangeBlocksAdministrativeEndpoints() throws Exception {
    account.setMustChangePassword(true);
    var res = request("GET", "/api/v1/auth/accounts", 0);
    assertEquals(403, res.getStatus());
    assertTrue(res.getContentAsString().contains("PASSWORD_CHANGE_REQUIRED"));
  }

  @Test
  void forcedChangeAllowsMeAndPasswordChange() throws Exception {
    account.setMustChangePassword(true);
    request("GET", "/api/v1/auth/me", 0);
    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    SecurityContextHolder.clearContext();
    request("POST", "/api/v1/auth/me/password", 0);
    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void tokenCannotBeReusedForDifferentAccountWithSameUsername() throws Exception {
    account.setId(2L);
    request("GET", "/api/v1/auth/me", 0);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void publicLoginIgnoresAnAttachedRestrictedToken() throws Exception {
    account.setMustChangePassword(true);
    var response = request("POST", "/api/v1/auth/login", 0);
    assertEquals(200, response.getStatus());
    verifyNoInteractions(users);
  }
}
