package com.example.assistant.chat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.assistant.model.ModelGateway;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.config.import=",
      "assistant.mcp.enabled=true",
      "assistant.chat.enabled=true"
    })
class C5HttpTest {
  static final ChatFixture fixture = new ChatFixture();

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    for (String name : List.of("auth", "academic", "teaching", "finance"))
      r.add("services." + name + ".url", fixture::origin);
  }

  @MockitoBean ModelGateway model;
  @LocalServerPort int port;

  @BeforeEach
  void setup() {
    fixture.reset();
    when(model.configured()).thenReturn(true);
  }

  @AfterAll
  static void cleanup() {
    fixture.close();
  }

  @Test
  void realChunkedBodyIsBoundedWithoutContentLengthAndNeverCallsModel() throws Exception {
    try (var client = HttpClient.newHttpClient()) {
      var body = " ".repeat(16385) + "{\"message\":\"Khóa học\"}";
      var request =
          HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/assistant/chat"))
              .header("Authorization", ChatFixture.TOKEN)
              .header("Content-Type", "application/json")
              .POST(
                  HttpRequest.BodyPublishers.ofInputStream(
                      () ->
                          new ByteArrayInputStream(
                              body.getBytes(java.nio.charset.StandardCharsets.UTF_8))))
              .build();
      var response = client.send(request, HttpResponse.BodyHandlers.ofString());
      assertEquals(413, response.statusCode());
      assertTrue(response.body().contains("REQUEST_TOO_LARGE"));
      assertEquals("no-store", response.headers().firstValue("Cache-Control").orElseThrow());
      verify(model, never()).generate(anyString(), anyList(), anyList(), any());
      assertTrue(fixture.calls.isEmpty());
    }
  }

  @Test
  void fractionalSelectionIdAndEncodedBodyAreRejected() throws Exception {
    try (var client = HttpClient.newHttpClient()) {
      var builder =
          HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/assistant/chat"))
              .header("Authorization", ChatFixture.TOKEN)
              .header("Content-Type", "application/json");
      var response =
          client.send(
              builder
                  .POST(
                      HttpRequest.BodyPublishers.ofString(
                          "{\"message\":\"Chọn học"
                              + " viên\",\"selection\":{\"type\":\"STUDENT\",\"id\":1.9}}"))
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      assertEquals(400, response.statusCode());
      var compressed =
          client.send(
              builder
                  .header("Content-Encoding", "gzip")
                  .POST(HttpRequest.BodyPublishers.ofByteArray(new byte[] {1, 2}))
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      assertEquals(415, compressed.statusCode());
      verify(model, never()).generate(anyString(), anyList(), anyList(), any());
    }
  }
}
