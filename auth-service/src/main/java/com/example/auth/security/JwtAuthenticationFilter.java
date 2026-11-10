package com.example.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;
  private final CustomUserDetailsService userDetailsService;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return "OPTIONS".equals(request.getMethod())
        || ("POST".equals(request.getMethod())
            && request.getRequestURI().equals("/api/v1/auth/login"));
  }

  private boolean passwordRoute(HttpServletRequest request) {
    String path = request.getServletPath();
    if (path == null || path.isEmpty()) path = request.getRequestURI();
    return ("GET".equals(request.getMethod()) && path.equals("/api/v1/auth/me"))
        || ("POST".equals(request.getMethod())
            && (path.equals("/api/v1/auth/me/password")
                || path.equals("/api/v1/auth/me/logout-all")));
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(7); // bỏ chữ "Bearer " (7 ký tự)

    try {
      String username = jwtUtil.extractUsername(token);

      if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (userDetails.isEnabled()
            && userDetails.isAccountNonLocked()
            && jwtUtil.validateToken(token, username)
            && userDetails instanceof AccountPrincipal principal
            && principal.accountId().equals(jwtUtil.extractAccountId(token))
            && principal.sessionVersion() == jwtUtil.extractSessionVersion(token)) {
          if (principal.mustChangePassword() && !passwordRoute(request)) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response
                .getWriter()
                .write(
                    "{\"code\":\"PASSWORD_CHANGE_REQUIRED\",\"message\":\"Bạn phải đổi mật khẩu"
                        + " trước khi tiếp tục\"}");
            return;
          }
          UsernamePasswordAuthenticationToken authToken =
              new UsernamePasswordAuthenticationToken(
                  userDetails, null, userDetails.getAuthorities());
          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authToken);
        }
      }
    } catch (Exception e) {
      // Token sai/hết hạn -> bỏ qua, để Spring Security tự chặn ở bước sau
    }

    filterChain.doFilter(request, response);
  }
}
