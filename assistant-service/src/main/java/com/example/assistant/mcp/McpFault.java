package com.example.assistant.mcp;

/** Safe machine codes only: never retain an HTTP body, credential or provider exception. */
public final class McpFault extends RuntimeException {
  private final String code;

  public McpFault(String code) {
    super(code);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
