package com.example.finance.common;

import jakarta.validation.ConstraintViolationException;
import java.util.*;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(com.example.finance.online.PaymentFlowException.class)
  ResponseEntity<Map<String, Object>> payment(com.example.finance.online.PaymentFlowException e) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("code", e.getCode());
    body.put("message", e.getMessage());
    body.put("requestRejected", true);
    if (e.getOrderId() != null) body.put("orderId", e.getOrderId());
    return ResponseEntity.status(e.getStatus()).body(body);
  }
  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<Map<String, String>> missing() {
    return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy API"));
  }

  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<Map<String, String>> unauthenticated() {
    return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
  }

  @ExceptionHandler(ApiException.class)
  ResponseEntity<Map<String, String>> business(ApiException e) {
    return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException e) {
    Map<String, String> errors = new LinkedHashMap<>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(f -> errors.putIfAbsent(f.getField(), f.getDefaultMessage()));
    return ResponseEntity.badRequest().body(errors);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class,
    ConstraintViolationException.class,
    HandlerMethodValidationException.class
  })
  ResponseEntity<Map<String, String>> invalid(Exception e) {
    return ResponseEntity.badRequest().body(Map.of("message", "Dữ liệu hoặc tham số không hợp lệ"));
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<Map<String, String>> denied() {
    return ResponseEntity.status(403)
        .body(Map.of("message", "Bạn không có quyền thực hiện thao tác này"));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<Map<String, String>> integrity() {
    return ResponseEntity.status(409)
        .body(Map.of("message", "Dữ liệu trùng hoặc đang được tham chiếu"));
  }

  @ExceptionHandler({
    PessimisticLockingFailureException.class,
    OptimisticLockingFailureException.class
  })
  ResponseEntity<Map<String, String>> concurrent() {
    return ResponseEntity.status(409)
        .body(Map.of("message", "Dữ liệu đang được cập nhật, vui lòng thử lại"));
  }
}
