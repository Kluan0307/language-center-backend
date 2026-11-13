package com.example.assistant.model;

import com.example.assistant.chat.Deadline;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.*;

public interface ModelGateway {
  record FunctionCall(String name, Map<String, Object> arguments, String id) {}

  record Turn(Map<String, Object> content, List<FunctionCall> calls) {}

  boolean configured();

  Turn generate(
      String instruction,
      List<Map<String, Object>> contents,
      List<McpSchema.Tool> tools,
      Deadline deadline);
}
