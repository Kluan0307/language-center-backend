package com.example.assistant.chat;

import com.example.assistant.mcp.SessionVerifier;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.*;

public final class ChatModels {
  private ChatModels() {}

  public enum EntityType {
    STUDENT,
    CLASS,
    SESSION
  }

  public record Selection(@NotNull EntityType type, @NotNull @Positive Long id) {}

  public record Request(
      @NotBlank @Size(max = 2000) String message,
      UUID conversationId,
      UUID requestId,
      SessionVerifier.Mode scopeMode,
      @jakarta.validation.Valid Selection selection) {
    @Override
    public String toString() {
      return "Chat request [content redacted]";
    }
  }

  public record Choice(EntityType type, Long id, String code, String label, Long branchId) {}

  public record Card(String tool, String title, Object data) {}

  public record Source(String tool, String observedAt, Map<String, Object> scope) {}

  public record Response(
      UUID requestId,
      UUID conversationId,
      String status,
      String message,
      Map<String, Object> scope,
      OffsetDateTime observedAt,
      List<Card> cards,
      List<Choice> choices,
      List<Source> sources,
      List<String> warnings,
      boolean hasMore,
      Integer nextPage) {
    @Override
    public String toString() {
      return "Chat response [requestId=" + requestId + ", status=" + status + "]";
    }
  }

  public record Evidence(String tool, Map<String, Object> arguments, Map<String, Object> payload) {}
}
