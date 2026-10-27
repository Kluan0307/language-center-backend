package com.example.academic.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {
  private final HttpStatus status;

  public ApiException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  public static ApiException bad(String m) {
    return new ApiException(HttpStatus.BAD_REQUEST, m);
  }

  public static ApiException missing() {
    return new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu");
  }

  public static ApiException conflict(String m) {
    return new ApiException(HttpStatus.CONFLICT, m);
  }

  public static ApiException forbidden() {
    return new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này");
  }
}
