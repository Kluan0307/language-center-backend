package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import com.example.finance.online.PaymentProviderConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

class PaymentProviderConfigTest {
  private PaymentProviderConfig config(String database, String... profiles) {
    var env = new MockEnvironment().withProperty("spring.datasource.url", database);
    env.setActiveProfiles(profiles);
    var c = new PaymentProviderConfig(env);
    ReflectionTestUtils.setField(c, "enabled", true);
    ReflectionTestUtils.setField(c, "merchantCode", "TESTCODE");
    ReflectionTestUtils.setField(c, "secret", "test-only-secret-for-configuration-checks-64-characters");
    ReflectionTestUtils.setField(c, "ttlMinutes", 15);
    ReflectionTestUtils.setField(c, "serverIp", "127.0.0.1");
    ReflectionTestUtils.setField(c, "checkoutUrl", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
    ReflectionTestUtils.setField(c, "queryUrl", "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction");
    ReflectionTestUtils.setField(c, "returnUrl", "https://center.example/api/v1/finance/payment-providers/vnpay/return");
    return c;
  }

  @Test void normalProfileRequiresHttpsEvenForLocalhost() {
    var c = config("jdbc:mysql://localhost/language_center_db", "prod");
    ReflectionTestUtils.setField(c, "returnUrl", "http://localhost/return");
    assertThrows(IllegalStateException.class, c::validate);
  }
  @Test void paddedSecretIsRejectedBeforeGeneratingInvalidSignatures() {
    for (String padding : new String[]{" ", "\t", "\r\n"}) {
      for (boolean leading : new boolean[]{true, false}) {
        var c = config("jdbc:mysql://localhost/language_center_db");
        String key = "test-only-secret-for-configuration-checks-64-characters";
        ReflectionTestUtils.setField(c, "secret", leading ? padding + key : key + padding);
        var error = assertThrows(IllegalStateException.class, c::validate);
        assertEquals("VNPAY_HASH_SECRET must not contain leading or trailing whitespace", error.getMessage());
        assertFalse(error.getMessage().contains(key));
      }
    }
  }
  @Test void protocolProfileCannotUseMainDatabase() {
    var c = config("jdbc:mysql://localhost/language_center_db", "payment-test");
    assertThrows(IllegalStateException.class, c::validate);
  }
  @Test void protocolProfileCannotCombineWithProd() {
    var c = config("jdbc:mysql://localhost/lc_student_g8_123", "payment-test", "prod");
    assertThrows(IllegalStateException.class, c::validate);
  }
  @Test void sandboxEndpointCannotBeReplacedByAnotherHttpsHost() {
    var c = config("jdbc:mysql://localhost/language_center_db", "prod");
    ReflectionTestUtils.setField(c, "checkoutUrl", "https://other.example/checkout");
    assertThrows(IllegalStateException.class, c::validate);
  }
  @Test void isolatedProtocolAndOfficialSandboxConfigurationsAreAccepted() {
    assertDoesNotThrow(config("jdbc:mysql://localhost/lc_student_g8_123", "payment-test")::validate);
    assertDoesNotThrow(config("jdbc:mysql://localhost/language_center_db", "prod")::validate);
  }
}
