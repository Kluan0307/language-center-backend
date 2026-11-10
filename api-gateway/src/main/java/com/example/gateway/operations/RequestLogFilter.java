package com.example.gateway.operations;

import java.util.UUID;
import org.slf4j.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.*;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLogFilter implements WebFilter {
  private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String supplied = exchange.getRequest().getHeaders().getFirst("X-Request-Id");
    String id =
        supplied != null && supplied.matches("[A-Za-z0-9-]{1,64}")
            ? supplied
            : UUID.randomUUID().toString();
    long start = System.nanoTime();
    exchange.getResponse().getHeaders().set("X-Request-Id", id);
    exchange
        .getResponse()
        .beforeCommit(
            () -> {
              // Downstream also returns this header; keep one value on the browser response.
              exchange.getResponse().getHeaders().set("X-Request-Id", id);
              return Mono.empty();
            });
    var forwarded =
        exchange
            .mutate()
            .request(exchange.getRequest().mutate().headers(h -> h.set("X-Request-Id", id)).build())
            .build();
    return chain
        .filter(forwarded)
        .doFinally(
            signal ->
                log.info(
                    "requestId={} method={} status={} durationMs={}",
                    id,
                    exchange.getRequest().getMethod(),
                    exchange.getResponse().getStatusCode(),
                    (System.nanoTime() - start) / 1_000_000));
  }
}
