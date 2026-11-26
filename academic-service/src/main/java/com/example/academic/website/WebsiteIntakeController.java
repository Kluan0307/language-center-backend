package com.example.academic.website;

import com.example.academic.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/academic/public/consultations")
public class WebsiteIntakeController {
  private final WebsiteIntakeService service;
  private final WebsiteIntakeLimiter limiter;
  private final String secret;
  public WebsiteIntakeController(WebsiteIntakeService service,WebsiteIntakeLimiter limiter,@Value("${website.form.secret:}") String secret){this.service=service;this.limiter=limiter;this.secret=secret;}
  @PostMapping
  public ResponseEntity<WebsiteIntakeDto.Accepted> submit(@Valid @RequestBody WebsiteIntakeDto.Request body,
      @RequestHeader(value="X-Website-Key",defaultValue="") String key,
      @RequestHeader(value="X-Website-Client",defaultValue="") String client,HttpServletRequest request){
    if(secret.length()<32)throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Đăng ký trực tuyến tạm thời chưa sẵn sàng. Vui lòng liên hệ trung tâm.");
    if(!MessageDigest.isEqual(secret.getBytes(StandardCharsets.UTF_8),key.getBytes(StandardCharsets.UTF_8)))throw new ApiException(HttpStatus.UNAUTHORIZED,"Yêu cầu không hợp lệ.");
    if(request.getContentLengthLong()>8192)throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,"Thông tin gửi quá dài.");
    limiter.take("global",120,Duration.ofMinutes(1));
    if(client.matches("[0-9a-f]{64}"))limiter.take("client:"+client,10,Duration.ofMinutes(15));
    return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(service.submit(body));
  }
}
