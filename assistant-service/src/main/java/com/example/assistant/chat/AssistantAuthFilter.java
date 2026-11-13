package com.example.assistant.chat;

import com.example.assistant.mcp.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

@Component
public class AssistantAuthFilter extends OncePerRequestFilter {
  private final SessionVerifier verifier;
  private final JsonMapper json;
  private final UsageLimiter limiter;

  public AssistantAuthFilter(SessionVerifier verifier, JsonMapper json, UsageLimiter limiter) {
    this.verifier = verifier;
    this.json = json;
    this.limiter = limiter;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/v1/assistant/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    req.setAttribute("assistant.startedNanos", System.nanoTime());
    HttpServletRequest bounded = req;
    UUID trace = UUID.randomUUID();
    res.setHeader("X-Request-Id", trace.toString());
    res.setHeader("Cache-Control", "no-store");
    res.setHeader("Pragma", "no-cache");
    try {
      if (req.getContentLengthLong() > 16384) throw new ChatFault(413, "REQUEST_TOO_LARGE");
      var who = verifier.current(req.getHeader("Authorization"));
      limiter.request(who.accountId());
      if (req.getMethod().equals("POST") && req.getRequestURI().equals("/api/v1/assistant/chat"))
        bounded = new BoundedRequest(req);
      var auth =
          new UsernamePasswordAuthenticationToken(
              who, null, List.of(new SimpleGrantedAuthority("ROLE_" + who.role().name())));
      SecurityContextHolder.getContext().setAuthentication(auth);
    } catch (McpFault e) {
      int status =
          switch (e.code()) {
            case "FORBIDDEN", "PASSWORD_CHANGE_REQUIRED" -> 403;
            case "UNAUTHENTICATED" -> 401;
            default -> 503;
          };
      reject(res, status, e.code());
      return;
    } catch (ChatFault e) {
      reject(res, e.status(), e.code());
      return;
    } catch (IOException e) {
      reject(res, 400, "INVALID_REQUEST");
      return;
    }
    try {
      chain.doFilter(bounded, res);
    } finally {
      SecurityContextHolder.clearContext();
      org.slf4j.LoggerFactory.getLogger(AssistantAuthFilter.class)
          .info(
              "assistant_http requestId={} status={} durationMs={}",
              trace,
              res.getStatus(),
              (System.nanoTime() - (Long) req.getAttribute("assistant.startedNanos")) / 1_000_000);
    }
  }

  private void reject(HttpServletResponse res, int status, String code) throws IOException {
    SecurityContextHolder.clearContext();
    res.setStatus(status);
    org.slf4j.LoggerFactory.getLogger(AssistantAuthFilter.class)
        .info(
            "assistant_reject requestId={} status={} code={}",
            res.getHeader("X-Request-Id"),
            status,
            code);
    if (status == 429) res.setHeader("Retry-After", "60");
    res.setContentType("application/json;charset=UTF-8");
    res.getWriter()
        .write(json.writeValueAsString(Map.of("code", code, "message", ChatMessages.error(code))));
  }
}
