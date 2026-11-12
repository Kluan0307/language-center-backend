package com.example.finance.online;
import com.example.finance.common.BusinessTime;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;
@Configuration @EnableScheduling class PaymentScheduling {}
@Component @RequiredArgsConstructor @ConditionalOnProperty(name="payment.jobs.enabled",havingValue="true")
public class OnlineJobs {
 private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(OnlineJobs.class);
 private final OnlineOrderRepository orders; private final ProviderEventRepository events; private final OnlineSettlement settlement;
 @Scheduled(fixedDelayString="${payment.jobs.delay-ms:60000}") public void run(){for(var id:orders.expiredIds(LocalDateTime.now(BusinessTime.ZONE),PageRequest.of(0,100)))try{settlement.expire(id);}catch(RuntimeException ex){log.warn("Payment expiry retry required for order {}",id);}for(var id:events.receivedIds(PageRequest.of(0,100)))try{settlement.process(id,false);}catch(RuntimeException ex){log.warn("Payment inbox retry required for event {}",id);}}
}
