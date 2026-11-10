package com.example.auth.operations;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "management.endpoints.web.exposure.include=health",
      "management.endpoint.health.probes.enabled=true",
      "management.endpoint.health.show-details=never",
      "management.endpoint.health.show-components=never",
      "management.endpoint.health.group.readiness.include=readinessState,db"
    })
class OperationsHttpTest {
  @Autowired Environment env;

  HttpResponse<String> request(String path, String id) throws Exception {
    return HttpClient.newHttpClient()
        .send(
            HttpRequest.newBuilder(
                    URI.create("http://127.0.0.1:" + env.getProperty("local.server.port") + path))
                .header("X-Request-Id", id)
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void probesReturnOnlyStatusAndPreserveSafeRequestId() throws Exception {
    for (String path :
        java.util.List.of("/actuator/health/liveness", "/actuator/health/readiness")) {
      var res = request(path, "probe-123");
      assertEquals(200, res.statusCode());
      assertEquals("{\"status\":\"UP\"}", res.body());
      assertEquals("probe-123", res.headers().firstValue("X-Request-Id").orElseThrow());
    }
  }

  @Test
  void sensitiveManagementPathsAreDeniedAndUnsafeRequestIdsReplaced() throws Exception {
    var res = request("/actuator/env", "bad id");
    assertTrue(java.util.Set.of(401, 403, 404).contains(res.statusCode()));
    assertTrue(
        res.headers().firstValue("X-Request-Id").orElseThrow().matches("[A-Za-z0-9-]{1,64}"));
    assertFalse(res.body().contains("jwt.secret"));
  }
}
