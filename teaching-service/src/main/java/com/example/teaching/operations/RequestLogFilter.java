package com.example.teaching.operations;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLogFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String supplied = req.getHeader("X-Request-Id");
    String id =
        supplied != null && supplied.matches("[A-Za-z0-9-]{1,64}")
            ? supplied
            : UUID.randomUUID().toString();
    long start = System.nanoTime();
    res.setHeader("X-Request-Id", id);
    try {
      chain.doFilter(req, res);
    } finally {
      log.info(
          "requestId={} method={} status={} durationMs={}",
          id,
          req.getMethod(),
          res.getStatus(),
          (System.nanoTime() - start) / 1_000_000);
    }
  }
}
