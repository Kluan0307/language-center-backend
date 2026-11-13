package com.example.assistant.chat;

import java.util.Map;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ChatExceptionHandler {
  @ExceptionHandler(ChatFault.class)
  ResponseEntity<?> fault(ChatFault e) {
    return ResponseEntity.status(e.status())
        .headers(
            headers -> {
              if (e.status() == 429) headers.set("Retry-After", "60");
              if (e.code().equals("MODEL_BUSY") || e.code().equals("MODEL_COOLDOWN"))
                headers.set("Retry-After", "30");
            })
        .body(Map.of("code", e.code(), "message", ChatMessages.error(e.code())));
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<?> invalid(Exception ignored) {
    return ResponseEntity.badRequest()
        .body(Map.of("code", "INVALID_REQUEST", "message", ChatMessages.error("INVALID_REQUEST")));
  }
}
