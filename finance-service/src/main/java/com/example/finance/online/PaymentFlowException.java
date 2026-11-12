package com.example.finance.online;

import com.example.finance.common.ApiException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** A rejected payment command: no order from this request was committed. */
@Getter
public class PaymentFlowException extends ApiException {
  private final String code;
  private final String orderId;

  public PaymentFlowException(HttpStatus status, String code, String message, String orderId) {
    super(status, message);
    this.code = code;
    this.orderId = orderId;
  }

  public static PaymentFlowException conflict(String code, String message) {
    return new PaymentFlowException(HttpStatus.CONFLICT, code, message, null);
  }
}
