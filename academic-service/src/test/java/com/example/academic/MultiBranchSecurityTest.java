package com.example.academic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.academic.common.ApiException;
import com.example.academic.security.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.ResourceAccessException;

class MultiBranchSecurityTest {
  final String secret = "test-only-secret-at-least-thirty-two-bytes";
  SessionDirectory directory;
  JwtAuthenticationFilter filter;
  String bearer;

  @BeforeEach
  void setup() {
    directory = mock(SessionDirectory.class);
    filter = new JwtAuthenticationFilter(secret, directory);
    bearer =
        "Bearer "
            + Jwts.builder()
                .setSubject("admin")
                .claim("role", "ADMIN")
                .claim("accountId", 1L)
                .claim("profileId", 2L)
                .claim("branchId", 11L)
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(
                    Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  SessionDirectory.Session session(List<Long> ids) {
    return new SessionDirectory.Session(1L, 2L, 11L, ids, "ADMIN", "admin", true);
  }

  MockHttpServletResponse request(String branch, jakarta.servlet.FilterChain chain)
      throws Exception {
    var req = new MockHttpServletRequest("GET", "/api/v1/test");
    req.addHeader("Authorization", bearer);
    if (branch != null) req.addHeader("X-Branch-Id", branch);
    var res = new MockHttpServletResponse();
    filter.doFilter(req, res, chain);
    return res;
  }

  @Test
  void existingTokenCanUseNewAssignmentButNeverAnUnassignedBranch() throws Exception {
    when(directory.get(bearer)).thenReturn(session(List.of(11L, 12L)));
    var res =
        request(
            "12",
            (req, r) -> {
              assertEquals(12L, Users.current().branchId());
              assertEquals(12L, new BranchScope().filter(null));
              new BranchScope().check(11L);
              assertThrows(ApiException.class, () -> new BranchScope().filter(13L));
              assertThrows(ApiException.class, () -> new BranchScope().check(13L));
            });
    assertEquals(200, res.getStatus());
    var chain = mock(jakarta.servlet.FilterChain.class);
    assertEquals(403, request("13", chain).getStatus());
    verifyNoInteractions(chain);
  }

  @Test
  void revocationImmediatelyRejectsOldTokenSelection() throws Exception {
    when(directory.get(bearer)).thenReturn(session(List.of(11L)));
    var chain = mock(jakarta.servlet.FilterChain.class);
    assertEquals(403, request("12", chain).getStatus());
    verifyNoInteractions(chain);
  }

  @Test
  void invalidBranchHeaderIsRejected() throws Exception {
    when(directory.get(bearer)).thenReturn(session(List.of(11L)));
    assertEquals(400, request("0", mock(jakarta.servlet.FilterChain.class)).getStatus());
  }

  @Test
  void disabledAccountCannotReachBusinessCode() throws Exception {
    when(directory.get(bearer))
        .thenReturn(
            new SessionDirectory.Session(1L, 2L, 11L, List.of(11L), "ADMIN", "admin", false));
    var chain = mock(jakarta.servlet.FilterChain.class);
    assertEquals(401, request(null, chain).getStatus());
    verifyNoInteractions(chain);
  }

  @Test
  void authOutageDoesNotFallBackToStaleClaims() throws Exception {
    when(directory.get(bearer)).thenThrow(new ResourceAccessException("offline"));
    var chain = mock(jakarta.servlet.FilterChain.class);
    assertEquals(503, request(null, chain).getStatus());
    verifyNoInteractions(chain);
  }

  @Test
  void currentRoleOverridesRoleInOldToken() throws Exception {
    when(directory.get(bearer))
        .thenReturn(
            new SessionDirectory.Session(1L, 2L, 11L, List.of(11L), "STAFF", "admin", true));
    request(
        null,
        (req, res) -> {
          assertEquals(Role.STAFF, Users.current().role());
          assertTrue(
              SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                  .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF")));
        });
  }

  @Test
  void temporaryPasswordSessionCannotReachBusinessCode() throws Exception {
    when(directory.get(bearer))
        .thenReturn(
            new SessionDirectory.Session(1L, 2L, 11L, List.of(11L), "ADMIN", "admin", true, true));
    var chain = mock(jakarta.servlet.FilterChain.class);
    var response = request(null, chain);
    assertEquals(403, response.getStatus());
    assertTrue(response.getContentAsString().contains("PASSWORD_CHANGE_REQUIRED"));
    verifyNoInteractions(chain);
  }
}
