package com.example.assistant;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.assistant.mcp.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(
    properties = {"assistant.mcp.enabled=false", "spring.config.import=", "server.port=0"})
class ServiceSecurityTest {
  @Autowired WebApplicationContext context;
  @Autowired AssistantMcpRuntime runtime;
  MockMvc mvc;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void healthOnlyIsPublic() throws Exception {
    mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
  }

  @Test
  void noHttpMcpOrToolExecutionEndpointExists() throws Exception {
    for (String route :
        java.util.List.of("/mcp", "/sse", "/api/v1/assistant/tools", "/api/v1/assistant/chat")) {
      mvc.perform(get(route)).andExpect(status().isUnauthorized());
      mvc.perform(post(route).content("{}")).andExpect(status().isUnauthorized());
    }
  }

  @Test
  void featureDisabledDoesNotContactAnyBackend() {
    assertEquals(
        "FEATURE_DISABLED",
        assertThrows(
                McpFault.class,
                () -> runtime.open("Bearer unused", SessionVerifier.Mode.ACTIVE_BRANCH, 1L))
            .code());
  }

  @Test
  @org.springframework.security.test.context.support.WithMockUser(roles = "SUPER_ADMIN")
  void evenAuthenticatedHttpUsersCannotOpenMcp() throws Exception {
    mvc.perform(post("/mcp").content("{}")).andExpect(status().isForbidden());
  }
}
