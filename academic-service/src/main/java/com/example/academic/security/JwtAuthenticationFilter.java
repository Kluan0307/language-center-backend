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
  private final SessionDirectory sessions;

  public JwtAuthenticationFilter(@Value("${jwt.secret}") String secret, SessionDirectory sessions) {
    this.sessions = sessions;
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
        var live = sessions.get(header);
        if (live == null
            || !live.status()
            || !account.equals(live.accountId())
            || !c.getSubject().equals(live.username())) throw new IllegalArgumentException();
        if (live.mustChangePassword()) {
          res.setStatus(403);
          res.setContentType("application/json;charset=UTF-8");
          res.getWriter()
              .write(
                  "{\"code\":\"PASSWORD_CHANGE_REQUIRED\",\"message\":\"Bạn phải đổi mật khẩu trước"
                      + " khi tiếp tục\"}");
          return;
        }
        role = Role.valueOf(live.role());
        profile = live.profileId();
        branch = live.branchId();
        var ids = live.branchIds() == null ? java.util.List.<Long>of() : live.branchIds();
        String selected = req.getHeader("X-Branch-Id");
        if (selected != null && role != Role.STUDENT) {
          try {
            branch = Long.valueOf(selected);
            if (branch <= 0) throw new NumberFormatException();
          } catch (NumberFormatException e) {
            error(res, 400, "Invalid X-Branch-Id");
            return;
          }
        }
        if (role != Role.SUPER_ADMIN && role != Role.STUDENT
            && (ids.isEmpty() || branch == null || !ids.contains(branch))) {
          error(res, 403, "Chi nhánh không được phân công");
          return;
        }
        var user = new CurrentUser(account, profile, branch, role, live.username(), ids);
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    user, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
      } catch (org.springframework.web.client.RestClientResponseException e) {
        SecurityContextHolder.clearContext();
        error(
            res,
            e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403 ? 401 : 503,
            "Không xác minh được phiên đăng nhập");
        return;
      } catch (org.springframework.web.client.RestClientException e) {
        SecurityContextHolder.clearContext();
        error(res, 503, "Dịch vụ xác thực chưa sẵn sàng");
        return;
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
