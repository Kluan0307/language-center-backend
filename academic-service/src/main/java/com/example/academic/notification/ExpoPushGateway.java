package com.example.academic.notification;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ExpoPushGateway {
 @Value("${notifications.expo.access-token:}") private String accessToken;
 public record Result(String status,String id,String code) {}
 private Map<String,Object> post(String path,Object body){var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());factory.setReadTimeout(Duration.ofSeconds(5));var client=RestClient.builder().requestFactory(factory).baseUrl("https://exp.host/--/api/v2/push").build();var request=client.post().uri(path).header("Content-Type","application/json");if(!accessToken.isBlank())request.header("Authorization","Bearer "+accessToken);return request.body(body).retrieve().body(new ParameterizedTypeReference<Map<String,Object>>(){});}
 private Result result(Object value){if(!(value instanceof Map<?,?> r))return new Result("UNKNOWN",null,"INVALID_RESPONSE");if("ok".equals(r.get("status")))return new Result("OK",r.get("id") instanceof String id?id:null,null);String code=r.get("details") instanceof Map<?,?> details&&details.get("error") instanceof String error?error:"PROVIDER_ERROR";return new Result("ERROR",null,code);}
 public Result send(String token,Long account,Long notification){var response=post("/send",Map.of("to",token,"title","Language Center","body","Bạn có thông báo mới. Mở ứng dụng để xem.","sound","default","channelId","student-updates","data",Map.of("accountId",account,"notificationId",notification)));return result(response==null?null:response.get("data"));}
 public Result receipt(String ticket){var response=post("/getReceipts",Map.of("ids",List.of(ticket)));return result(response!=null&&response.get("data") instanceof Map<?,?> data?data.get(ticket):null);}
}
