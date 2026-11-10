package com.example.auth.common;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.context.request.*;

@Component
public class RemoteApi {
  private final JdkClientHttpRequestFactory factory;

  public RemoteApi() {
    factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
    factory.setReadTimeout(Duration.ofSeconds(5));
  }

  public String token() {
    var attributes = RequestContextHolder.getRequestAttributes();
    if (!(attributes instanceof ServletRequestAttributes request))
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing user request");
    String token = request.getRequest().getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer "))
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
    return token;
  }

  public <T> T get(String url, Class<T> type) {
    return get(url, type, token());
  }

  public <T> T get(String url, Class<T> type, String token) {
    try {
      T result =
          RestClient.builder()
              .requestFactory(factory)
              .build()
              .get()
              .uri(url)
              .header("Authorization", token)
              .retrieve()
              .body(type);
      if (result == null)
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Empty upstream response");
      return result;
    } catch (RestClientException e) {
      throw failure(e);
    }
  }

  public <T> T post(String url, Object body, Class<T> type, String token) {
    try {
      T result =
          RestClient.builder()
              .requestFactory(factory)
              .build()
              .post()
              .uri(url)
              .header("Authorization", token)
              .body(body)
              .retrieve()
              .body(type);
      if (result == null)
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Empty upstream response");
      return result;
    } catch (RestClientException e) {
      throw failure(e);
    }
  }

  private ApiException failure(RestClientException e) {
    if (e instanceof RestClientResponseException response) {
      int status = response.getStatusCode().value();
      if (status == 404) return ApiException.missing();
      if (status == 403) return ApiException.forbidden();
      if (status == 409) return ApiException.conflict("Upstream business conflict");
      if (status == 401)
        return new ApiException(HttpStatus.UNAUTHORIZED, "User token rejected by upstream");
    }
    return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Dependent service unavailable");
  }
}
