package com.example.assistant.chat;

import com.example.assistant.mcp.SessionVerifier;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/assistant")
@org.springframework.security.access.prepost.PreAuthorize(
    "hasAnyRole('SUPER_ADMIN','ADMIN','STAFF','TEACHER')")
public class ChatController {
  private final ChatService service;

  public ChatController(ChatService service) {
    this.service = service;
  }

  private SessionVerifier.Identity actor(Authentication auth) {
    if (auth == null || !(auth.getPrincipal() instanceof SessionVerifier.Identity identity))
      throw new ChatFault(401, "UNAUTHENTICATED");
    return identity;
  }

  @GetMapping("/config")
  public Map<String, Object> config(Authentication auth) {
    return service.configuration(actor(auth));
  }

  @PostMapping("/chat")
  public ChatModels.Response chat(
      Authentication auth,
      @RequestHeader("Authorization") String bearer,
      @RequestHeader(value = "X-Branch-Id", required = false) Long branch,
      @Valid @RequestBody ChatModels.Request body,
      HttpServletRequest request) {
    return service.chat(
        actor(auth), bearer, branch, body, (Long) request.getAttribute("assistant.startedNanos"));
  }

  @DeleteMapping("/conversations/{id}")
  public ResponseEntity<Void> delete(
      Authentication auth, @RequestHeader("Authorization") String bearer, @PathVariable UUID id) {
    service.delete(actor(auth), bearer, id);
    return ResponseEntity.noContent().build();
  }
}
