package com.example.academic;
import com.example.academic.website.WebsiteIntakeLimiter;
import com.example.academic.common.ApiException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WebsiteIntakeLimiterTest {
  @Test void repeatedRequestsAreLimitedIndependently(){var limit=new WebsiteIntakeLimiter();for(int i=0;i<3;i++)limit.take("one",3,Duration.ofHours(1));assertEquals(429,assertThrows(ApiException.class,()->limit.take("one",3,Duration.ofHours(1))).getStatus().value());assertDoesNotThrow(()->limit.take("other",3,Duration.ofHours(1)));}
}
