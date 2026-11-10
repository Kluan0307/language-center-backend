package com.example.finance.security;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

@Component
public class SessionDirectory {
  private final RestClient client;

  public SessionDirectory(@Value("${services.auth.url:http://localhost:8081}") String url) {
    var factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
    factory.setReadTimeout(Duration.ofSeconds(5));
    client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
  }

  public record Session(
      Long accountId,
      Long profileId,
      Long branchId,
      List<Long> branchIds,
      String role,
      String username,
      boolean status,
      boolean mustChangePassword) {
    public Session(
        Long accountId,
        Long profileId,
        Long branchId,
        List<Long> branchIds,
        String role,
        String username,
        boolean status) {
      this(accountId, profileId, branchId, branchIds, role, username, status, false);
    }
  }

  public Session get(String bearer) {
    return client
        .get()
        .uri("/api/v1/auth/me")
        .header("Authorization", bearer)
        .retrieve()
        .body(Session.class);
  }
}
