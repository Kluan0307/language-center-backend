package com.example.academic;

import static org.junit.jupiter.api.Assertions.*;

import com.example.academic.common.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.Test;

class UpstreamBusinessErrorTest {
  @Test
  void unavailableTeacherIsBusinessRejectionRatherThanDependencyOutage() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/teacher",
        exchange -> {
          byte[] data =
              "{\"message\":\"Teacher is inactive\"}"
                  .getBytes(java.nio.charset.StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(400, data.length);
          exchange.getResponseBody().write(data);
          exchange.close();
        });
    server.start();
    try {
      var api = new RemoteApi();
      var error =
          assertThrows(
              ApiException.class,
              () ->
                  api.get(
                      "http://127.0.0.1:" + server.getAddress().getPort() + "/teacher",
                      String.class,
                      "Bearer test"));
      assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, error.getStatus());
    } finally {
      server.stop(0);
    }
  }
}
