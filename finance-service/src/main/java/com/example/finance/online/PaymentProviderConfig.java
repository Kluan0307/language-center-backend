package com.example.finance.online;
import com.example.finance.common.ApiException;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.*;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
@Component @Getter
public class PaymentProviderConfig {
 @Value("${payment.vnpay.enabled:false}") private boolean enabled;
 @Value("${payment.vnpay.merchant-code:}") private String merchantCode;
 @Value("${payment.vnpay.secret:}") private String secret;
 @Value("${payment.vnpay.checkout-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}") private String checkoutUrl;
 @Value("${payment.vnpay.query-url:https://sandbox.vnpayment.vn/merchant_webapi/api/transaction}") private String queryUrl;
 @Value("${payment.vnpay.return-url:}") private String returnUrl;
 @Value("${payment.vnpay.server-ip:127.0.0.1}") private String serverIp;
 @Value("${payment.vnpay.ttl-minutes:15}") private int ttlMinutes;
 private final Environment environment;
 public PaymentProviderConfig(Environment environment){this.environment=environment;}
 public boolean protocolTest(){return Arrays.asList(environment.getActiveProfiles()).contains("payment-test");}
 @PostConstruct public void validate(){if(!secret.equals(secret.strip()))throw new IllegalStateException("VNPAY_HASH_SECRET must not contain leading or trailing whitespace");if(!enabled&&merchantCode.isBlank()&&secret.isBlank())return;boolean test=Arrays.asList(environment.getActiveProfiles()).contains("payment-test")&&!Arrays.asList(environment.getActiveProfiles()).contains("prod");if(!merchantCode.matches("[a-zA-Z0-9]{8}")||secret.length()<32||ttlMinutes<1||ttlMinutes>15||!serverIp.matches("[0-9a-fA-F:.]{3,45}"))throw new IllegalStateException("Invalid VNPAY configuration");if(protocolTest()&&(!test||!environment.getProperty("spring.datasource.url","").matches(".*(/lc_student_g8_[0-9]+([?].*)?|jdbc:h2:mem:.*)")))throw new IllegalStateException("Protocol test requires an isolated G8 test database and cannot use prod");for(String s:List.of(checkoutUrl,queryUrl,returnUrl)){if(s.isBlank()&&!enabled)continue;URI u=URI.create(s);if(u.getUserInfo()!=null||u.getFragment()!=null||u.getQuery()!=null||u.getHost()==null)throw new IllegalStateException("Invalid payment endpoint");if(test&&Set.of("127.0.0.1","localhost").contains(u.getHost()))continue;if(!"https".equals(u.getScheme()))throw new IllegalStateException("Payment endpoints require HTTPS");}if(!test&&(!checkoutUrl.equals("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html")||!queryUrl.equals("https://sandbox.vnpayment.vn/merchant_webapi/api/transaction")))throw new IllegalStateException("Only VNPAY sandbox is supported in G8");}
 public void requireVerification(){if(!merchantCode.matches("[a-zA-Z0-9]{8}")||secret.length()<32)throw new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Provider verification is not configured");}
 public void requireEnabled(){if(!enabled)throw new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Online payment is not configured");}
}
