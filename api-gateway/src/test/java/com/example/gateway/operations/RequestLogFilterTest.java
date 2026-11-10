package com.example.gateway.operations;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class RequestLogFilterTest {
  @Test
  void downstreamCorrelationDoesNotDuplicateTheGatewayHeader() {
    var exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/auth/teachers").header("X-Request-Id", "test-123"));
    new RequestLogFilter()
        .filter(
            exchange,
            forwarded -> {
              assertEquals(
                  "test-123", forwarded.getRequest().getHeaders().getFirst("X-Request-Id"));
              forwarded.getResponse().getHeaders().add("X-Request-Id", "test-123");
              return forwarded.getResponse().setComplete();
            })
        .block();
    assertEquals(
        java.util.List.of("test-123"), exchange.getResponse().getHeaders().get("X-Request-Id"));
  }
}
