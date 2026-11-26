package com.example.academic.website;

import com.example.academic.common.ApiException;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class WebsiteIntakeLimiter {
  private record Bucket(long started,int count) {}
  private final Map<String,Bucket> buckets=new HashMap<>();
  public synchronized void take(String key,int limit,Duration window){
    long now=System.currentTimeMillis(),ttl=window.toMillis();
    buckets.entrySet().removeIf(e->now-e.getValue().started()>Duration.ofHours(1).toMillis());
    Bucket b=buckets.get(key);
    if(b==null||now-b.started()>=ttl)b=new Bucket(now,0);
    if(b.count()>=limit||buckets.size()>=10000&&!buckets.containsKey(key))
      throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,"Bạn đã gửi nhiều yêu cầu. Vui lòng thử lại sau.");
    buckets.put(key,new Bucket(b.started(),b.count()+1));
  }
}
