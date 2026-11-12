package com.example.academic.notification;

import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.*;

@Configuration @EnableScheduling @RequiredArgsConstructor
@ConditionalOnProperty(name="notifications.jobs.enabled",havingValue="true")
public class NotificationJobs {
 private final JdbcTemplate jdbc; private final NotificationDelivery delivery; private final PushDispatch dispatch; private final ExpoPushGateway gateway;
 @Value("${notifications.push.enabled:false}") private boolean pushEnabled;
 private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(NotificationJobs.class);
 @Scheduled(fixedDelay=5000,initialDelay=5000) public void run(){for(String id:jdbc.queryForList("SELECT id FROM student_notification_outbox WHERE status='PENDING' ORDER BY occurred_at,id LIMIT 50",String.class))try{delivery.deliver(id);}catch(RuntimeException ex){LOG.warn("Notification delivery deferred for event {}",id);}if(!pushEnabled)return;
  for(Long id:jdbc.queryForList("SELECT id FROM student_push_deliveries WHERE status='PENDING' ORDER BY id LIMIT 50",Long.class))try{var a=dispatch.claim(id);if(a!=null){ExpoPushGateway.Result r;try{r=gateway.send(a.token(),a.account(),a.notification());}catch(RuntimeException ex){r=new ExpoPushGateway.Result("UNKNOWN",null,"TRANSPORT_UNCERTAIN");}dispatch.complete(id,r);}}catch(RuntimeException ex){LOG.warn("Push attempt deferred for delivery {}",id);}
  var now=LocalDateTime.now(com.example.academic.common.BusinessTime.ZONE);jdbc.update("UPDATE student_push_deliveries SET status='UNKNOWN',result_code='INTERRUPTED_ATTEMPT' WHERE status='SENDING' AND attempted_at<?",now.minusMinutes(10));
  for(var row:jdbc.queryForList("SELECT id,ticket_id FROM student_push_deliveries WHERE status='ACCEPTED' AND attempted_at<? AND attempted_at>? AND (receipt_checked_at IS NULL OR receipt_checked_at<?) LIMIT 50",now.minusMinutes(15),now.minusHours(24),now.minusMinutes(15)))try{dispatch.receipt(((Number)row.get("id")).longValue(),gateway.receipt((String)row.get("ticket_id")));}catch(RuntimeException ex){LOG.warn("Push receipt verification deferred");}
 }
}
