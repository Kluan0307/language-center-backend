package com.example.teaching.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final io.jsonwebtoken.JwtParser parser;

    public JwtAuthenticationFilter(@Value("${jwt.secret}") String secret) {
        this.parser = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null) {
            try {
                if (!header.startsWith("Bearer ")) {
                    throw new IllegalArgumentException("Token không đúng định dạng");
                }

                Claims claims = parser.parseClaimsJws(header.substring(7)).getBody();
                String subject = claims.getSubject();
                String roleName = claims.get("role", String.class);
                Long accountId = getLong(claims, "accountId");
                Long profileId = getLong(claims, "profileId");
                Long branchId = getLong(claims, "branchId");

                if (subject == null || subject.isBlank() || roleName == null || accountId == null || accountId <= 0) {
                    throw new IllegalArgumentException("Token thiếu thông tin bắt buộc");
                }

                Role role = Role.valueOf(roleName);
                CurrentUser currentUser = new CurrentUser(accountId, profileId, branchId, role, subject);

                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                currentUser,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
                        )
                );
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\":\"Token không hợp lệ hoặc đã hết hạn\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Long getLong(Claims claims, String key) {
        Object value = claims.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalArgumentException("Claim " + key + " không hợp lệ");
    }
}
