package com.example.assistant.mcp;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class BackendGateway {
  enum Target {
    AUTH,
    ACADEMIC,
    FINANCE,
    TEACHING
  }

  private final Map<Target, URI> origins;
  private final HttpClient http;
  private final JsonMapper json;

  public BackendGateway(
      JsonMapper json,
      @Value("${services.auth.url}") String auth,
      @Value("${services.academic.url}") String academic,
      @Value("${services.finance.url}") String finance,
      @Value("${services.teaching.url}") String teaching) {
    this.json = json;
    origins =
        Map.of(
            Target.AUTH,
            origin(auth),
            Target.ACADEMIC,
            origin(academic),
            Target.FINANCE,
            origin(finance),
            Target.TEACHING,
            origin(teaching));
    http =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
  }

  private URI origin(String value) {
    URI uri = URI.create(value);
    if (!Set.of("http", "https").contains(uri.getScheme())
        || uri.getHost() == null
        || uri.getUserInfo() != null
        || uri.getQuery() != null
        || uri.getFragment() != null
        || !(uri.getPath().isEmpty() || uri.getPath().equals("/")))
      throw new IllegalArgumentException("Backend URL must be a configured HTTP(S) origin");
    return URI.create(value.replaceAll("/+$", ""));
  }

  @SuppressWarnings("unchecked")
  Map<String, Object> get(
      Target target,
      String fixedPath,
      Map<String, Object> params,
      String bearer,
      Long selectedBranch) {
    String prefix = "/api/v1/" + target.name().toLowerCase(Locale.ROOT) + "/assistant-query/";
    if (target == Target.AUTH
        ? !fixedPath.equals("/api/v1/auth/me")
        : !fixedPath.startsWith(prefix) || !fixedPath.substring(prefix.length()).matches("[a-z-]+"))
      throw new McpFault("FORBIDDEN");
    StringBuilder url = new StringBuilder(origins.get(target).toString()).append(fixedPath);
    for (var e : params.entrySet())
      url.append(url.indexOf("?") < 0 ? '?' : '&')
          .append(encode(e.getKey()))
          .append('=')
          .append(encode(String.valueOf(e.getValue())));
    var request =
        HttpRequest.newBuilder(URI.create(url.toString()))
            .timeout(Duration.ofSeconds(6))
            .header("Authorization", bearer)
            .header("Accept", "application/json");
    if (selectedBranch != null) request.header("X-Branch-Id", selectedBranch.toString());
    try {
      var pending = http.sendAsync(request.GET().build(), info -> new LimitedBody());
      HttpResponse<byte[]> response;
      try {
        response = pending.get(6, java.util.concurrent.TimeUnit.SECONDS);
      } finally {
        if (!pending.isDone()) pending.cancel(true);
      }
      int code = response.statusCode();
      if (code != 200)
        throw new McpFault(
            switch (code) {
              case 400 -> "INVALID_ARGUMENTS";
              case 401 -> "UNAUTHENTICATED";
              case 403 -> "FORBIDDEN";
              case 404 -> "NOT_FOUND";
              case 429 -> "RATE_LIMITED";
              default -> "DEPENDENCY_UNAVAILABLE";
            });
      byte[] bytes = response.body();
      Object result = json.readValue(bytes, Object.class);
      if (!(result instanceof Map<?, ?> map)) throw new McpFault("INVALID_BACKEND_RESPONSE");
      return (Map<String, Object>) map;
    } catch (McpFault e) {
      throw e;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new McpFault("DEPENDENCY_UNAVAILABLE");
    } catch (Exception e) {
      for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause())
        if (cause instanceof McpFault fault) throw fault;
      throw new McpFault("DEPENDENCY_UNAVAILABLE");
    }
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  /** Complete a bounded body before the request future finishes, including slow-body deadlines. */
  private static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
    private final java.util.concurrent.CompletableFuture<byte[]> result =
        new java.util.concurrent.CompletableFuture<>();
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private java.util.concurrent.Flow.Subscription subscription;

    @Override
    public java.util.concurrent.CompletionStage<byte[]> getBody() {
      return result;
    }

    @Override
    public void onSubscribe(java.util.concurrent.Flow.Subscription subscription) {
      this.subscription = subscription;
      subscription.request(1);
    }

    @Override
    public void onNext(List<java.nio.ByteBuffer> buffers) {
      long incoming = buffers.stream().mapToLong(java.nio.ByteBuffer::remaining).sum();
      if (bytes.size() + incoming > 262144) {
        subscription.cancel();
        result.completeExceptionally(new McpFault("RESULT_TOO_LARGE"));
        return;
      }
      for (var buffer : buffers) {
        byte[] chunk = new byte[buffer.remaining()];
        buffer.get(chunk);
        bytes.writeBytes(chunk);
      }
      subscription.request(1);
    }

    @Override
    public void onError(Throwable error) {
      result.completeExceptionally(error);
    }

    @Override
    public void onComplete() {
      result.complete(bytes.toByteArray());
    }
  }
}
