package com.example.auth;

import static org.junit.jupiter.api.Assertions.*;

import com.example.auth.dto.*;
import org.junit.jupiter.api.Test;

class CredentialRedactionTest {
  @Test
  void requestAndResponseDiagnosticsNeverIncludeCredentials() {
    String secret = "unique-test-credential";
    var login = new LoginRequest();
    login.setUsername("staff");
    login.setPassword(secret);
    assertFalse(login.toString().contains(secret));
    assertFalse(CreateUserRequest.builder().password(secret).build().toString().contains(secret));
    assertFalse(new ChangePasswordRequest(secret, secret, secret).toString().contains(secret));
    assertFalse(new ResetPasswordRequest(secret, secret, "Recovery").toString().contains(secret));
    assertFalse(LoginResponse.builder().token(secret).build().toString().contains(secret));
  }
}
