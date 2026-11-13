package com.example.assistant.chat;

import static org.junit.jupiter.api.Assertions.*;

import com.example.assistant.mcp.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import tools.jackson.databind.json.JsonMapper;

class C5FilterTest {
  @Test
  void rotatingBearerDoesNotResetAccountRateAndOtherAccountIsSeparate() throws Exception {
    try (var fixture = new ChatFixture()) {
      var json = JsonMapper.builder().build();
      var backend =
          new BackendGateway(
              json, fixture.origin(), fixture.origin(), fixture.origin(), fixture.origin());
      var filter =
          new AssistantAuthFilter(
              new SessionVerifier(backend), json, new UsageLimiter(1, 2, 10, 1));
      var first = invoke(filter, ChatFixture.TOKEN);
      assertEquals(200, first.getStatus());
      var second = invoke(filter, ChatFixture.ROTATED);
      assertEquals(429, second.getStatus());
      assertEquals("60", second.getHeader("Retry-After"));
      assertEquals("no-store", second.getHeader("Cache-Control"));
      assertFalse(second.getContentAsString().contains(ChatFixture.ROTATED));
      assertEquals(200, invoke(filter, ChatFixture.SECOND).getStatus());
      assertEquals(401, invoke(filter, "Bearer invalid").getStatus());
    }
  }

  private MockHttpServletResponse invoke(AssistantAuthFilter filter, String bearer)
      throws Exception {
    var request = new MockHttpServletRequest("GET", "/api/v1/assistant/config");
    request.addHeader("Authorization", bearer);
    var response = new MockHttpServletResponse();
    filter.doFilter(request, response, (req, res) -> {});
    return response;
  }
}
