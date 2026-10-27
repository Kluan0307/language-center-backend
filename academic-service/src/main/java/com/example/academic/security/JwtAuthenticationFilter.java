package com.example.academic.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtParser parser;

  public JwtAuthenticationFilter(@Value("${jwt.secret}") String secret) {
    parser =
        Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
            .build();
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String header = req.getHeader("Authorization");
    if (header != null) {
      try {
        if (!header.startsWith("Bearer ")) throw new IllegalArgumentException();
        Claims c = parser.parseClaimsJws(header.substring(7)).getBody();
        if (c.getExpiration() == null || c.getSubject() == null || c.getSubject().isBlank())
          throw new IllegalArgumentException();
        String roleClaim = c.get("role", String.class);
        if (roleClaim == null) throw new IllegalArgumentException();
        Role role = Role.valueOf(roleClaim);
        Long account = id(c, "accountId"), profile = id(c, "profileId"), branch = id(c, "branchId");
        if (account == null
            || account <= 0
            || profile != null && profile <= 0
            || branch != null && branch <= 0) throw new IllegalArgumentException();
        if (role != Role.SUPER_ADMIN && branch == null) {
          error(res, 403, "Tài khoản chưa được gán chi nhánh");
          return;
        }
        var user = new CurrentUser(account, profile, branch, role, c.getSubject());
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    user, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
      } catch (JwtException | IllegalArgumentException e) {
        SecurityContextHolder.clearContext();
        error(res, 401, "Token không hợp lệ hoặc đã hết hạn");
        return;
      }
    }
    chain.doFilter(req, res);
  }

  private Long id(Claims c, String key) {
    Object value = c.get(key);
    if (value == null) return null;
    if (!(value instanceof Number)) throw new IllegalArgumentException();
    return Long.valueOf(value.toString());
  }

  private void error(HttpServletResponse res, int status, String message) throws IOException {
    res.setStatus(status);
    res.setContentType("application/json;charset=UTF-8");
    res.getWriter().write("{\"message\":\"" + message + "\"}");
  }
}
