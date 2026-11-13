package com.example.assistant.chat;

public final class ChatFault extends RuntimeException {
  private final int status;
  private final String code;

  public ChatFault(int status, String code) {
    super(code);
    this.status = status;
    this.code = code;
  }

  public int status() {
    return status;
  }

  public String code() {
    return code;
  }
}
