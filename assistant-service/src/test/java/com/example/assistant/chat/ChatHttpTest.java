package com.example.assistant.chat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.assistant.model.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
    properties = {
      "assistant.mcp.enabled=true",
      "assistant.chat.enabled=true",
      "spring.config.import=",
      "logging.level.io.modelcontextprotocol=OFF"
    })
class ChatHttpTest {
  static final ChatFixture fixture = new ChatFixture();

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    for (String service : List.of("auth", "academic", "finance", "teaching"))
      r.add("services." + service + ".url", fixture::origin);
  }

  @MockitoBean ModelGateway model;
  @Autowired WebApplicationContext context;
  @Autowired JsonMapper json;
  MockMvc mvc;

  @BeforeEach
  void setup() {
    fixture.reset();
    when(model.configured()).thenReturn(true);
    when(model.generate(anyString(), anyList(), anyList(), any()))
        .thenReturn(
            new ModelGateway.Turn(
                Map.of(
                    "role",
                    "model",
                    "parts",
                    List.of(
                        Map.of(
                            "functionCall", Map.of("name", "search_courses", "args", Map.of())))),
                List.of(new ModelGateway.FunctionCall("search_courses", Map.of(), null))));
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @AfterAll
  static void cleanup() {
    fixture.close();
  }

  String body(String message) throws Exception {
    return json.writeValueAsString(Map.of("message", message, "requestId", UUID.randomUUID()));
  }

  @Test
  void authenticatedChatAndConfigWorkWithSafeContract() throws Exception {
    mvc.perform(get("/api/v1/assistant/config").header("Authorization", ChatFixture.TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(true))
        .andExpect(jsonPath("$.readOnly").value(true));
    mvc.perform(
            post("/api/v1/assistant/chat")
                .header("Authorization", ChatFixture.TOKEN)
                .header("X-Branch-Id", "1")
                .contentType("application/json")
                .content(body("Khóa học giao tiếp?")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ANSWER"))
        .andExpect(jsonPath("$.scope.mode").value("ACTIVE_BRANCH"))
        .andExpect(jsonPath("$.cards[0].data.content[0].phone").doesNotExist());
  }

  @Test
  void noSessionCookieOrClientRoleCanReplaceBearer() throws Exception {
    mvc.perform(
            post("/api/v1/assistant/chat")
                .contentType("application/json")
                .content(body("Khóa học?")))
        .andExpect(status().isUnauthorized());
    fixture.role = "STUDENT";
    mvc.perform(get("/api/v1/assistant/config").header("Authorization", ChatFixture.TOKEN))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void unknownFieldsAndOverlongQuestionsAreRejected() throws Exception {
    mvc.perform(
            post("/api/v1/assistant/chat")
                .header("Authorization", ChatFixture.TOKEN)
                .contentType("application/json")
                .content("{\"message\":\"Khóa học?\",\"role\":\"SUPER_ADMIN\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/v1/assistant/chat")
                .header("Authorization", ChatFixture.TOKEN)
                .contentType("application/json")
                .content(body("a".repeat(2001))))
        .andExpect(status().isBadRequest());
    verify(model, never()).generate(anyString(), anyList(), anyList(), any());
  }

  @Test
  void bulkBodyAndForeignScopeAreRejected() throws Exception {
    mvc.perform(
            post("/api/v1/assistant/chat")
                .header("Authorization", ChatFixture.TOKEN)
                .contentType("application/json")
                .content(body("a".repeat(20000))))
        .andExpect(status().isPayloadTooLarge());
    mvc.perform(
            post("/api/v1/assistant/chat")
                .header("Authorization", ChatFixture.TOKEN)
                .header("X-Branch-Id", "3")
                .contentType("application/json")
                .content(body("Khóa học?")))
        .andExpect(status().isForbidden());
  }

  @Test
  void outsideQuestionsDoNotInvokeModel() throws Exception {
    mvc.perform(
            post("/api/v1/assistant/chat")
                .header("Authorization", ChatFixture.TOKEN)
                .contentType("application/json")
                .content(body("Viết code React giúp tôi")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("OUT_OF_SCOPE"));
    verify(model, never()).generate(anyString(), anyList(), anyList(), any());
  }

  @Test
  void deletionIsOwnedAndReturns204() throws Exception {
    var response =
        mvc.perform(
                post("/api/v1/assistant/chat")
                    .header("Authorization", ChatFixture.TOKEN)
                    .contentType("application/json")
                    .content(body("Khóa học giao tiếp?")))
            .andReturn();
    String id =
        String.valueOf(
            json.readValue(response.getResponse().getContentAsString(), Map.class)
                .get("conversationId"));
    mvc.perform(
            delete("/api/v1/assistant/conversations/" + id)
                .header("Authorization", ChatFixture.SECOND))
        .andExpect(status().isNotFound());
    mvc.perform(
            delete("/api/v1/assistant/conversations/" + id)
                .header("Authorization", ChatFixture.TOKEN))
        .andExpect(status().isNoContent());
  }

  @Test
  void teacherFinanceIsDeniedBeforeModel() throws Exception {
    fixture.role = "TEACHER";
    mvc.perform(
            post("/api/v1/assistant/chat")
                .header("Authorization", ChatFixture.TOKEN)
                .contentType("application/json")
                .content(body("Học phí học viên An?")))
        .andExpect(status().isForbidden());
    verify(model, never()).generate(anyString(), anyList(), anyList(), any());
  }

  @Test
  void revokedAuthFailsEvenWithConversationInRequest() throws Exception {
    fixture.active = false;
    mvc.perform(
            post("/api/v1/assistant/chat")
                .header("Authorization", ChatFixture.TOKEN)
                .contentType("application/json")
                .content(body("Khóa học?")))
        .andExpect(status().isUnauthorized());
    assertTrue(fixture.calls.isEmpty());
  }
}
