package com.example.teaching;

import static org.junit.jupiter.api.Assertions.*;

import com.example.teaching.common.RemoteApi;
import com.example.teaching.integration.AcademicDirectory;
import com.example.teaching.security.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.*;

class AcademicDirectoryUnitTest {
  @Test
  void readsAcademicDtoAndForwardsUserToken() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var authorization = new java.util.concurrent.atomic.AtomicReference<String>();
    server.createContext(
        "/api/v1/academic/classes/9",
        exchange -> {
          authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
          byte[] body =
              "{\"schoolClass\":{\"id\":9,\"branchId\":3,\"teacherId\":2,\"courseId\":5,\"name\":\"Class\",\"full\":false},\"schedules\":[]}"
                  .getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          try (var output = exchange.getResponseBody()) {
            output.write(body);
          }
        });
    server.start();
    var request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer user-token");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, 2L, 3L, Role.TEACHER, "teacher"), null, List.of()));
    try {
      var directory = new AcademicDirectory(new RemoteApi(), new BranchScope());
      ReflectionTestUtils.setField(
          directory, "base", "http://127.0.0.1:" + server.getAddress().getPort());
      assertEquals(9L, directory.schoolClass(9L).id());
      assertEquals("Bearer user-token", authorization.get());
    } finally {
      server.stop(0);
      RequestContextHolder.resetRequestAttributes();
      SecurityContextHolder.clearContext();
    }
  }
}
