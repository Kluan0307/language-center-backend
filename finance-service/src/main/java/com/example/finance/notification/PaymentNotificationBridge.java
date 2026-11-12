package com.example.finance.notification;
import com.example.finance.online.*;
import jakarta.persistence.*;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
import org.springframework.transaction.annotation.Transactional;
@Configuration @EnableScheduling @RequiredArgsConstructor @ConditionalOnProperty(name="notifications.jobs.enabled",havingValue="true")
public class PaymentNotificationBridge {
 private final EntityManager em;
 @Scheduled(fixedDelay=5000,initialDelay=5000) @Transactional public void run(){for(var p:em.createQuery("select p from PaymentOutbox p where p.deliveredAt is null order by p.id",PaymentOutbox.class).setMaxResults(50).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList()){var o=em.find(OnlineOrder.class,p.getOrderId());if(o==null)continue;StudentEvents.add(em,"finance:online:"+p.getEventKey(),"PAYMENT",p.getType(),p.getType().equals("PAYMENT_SUCCEEDED")?"Thanh toán đã ghi nhận":"Cập nhật đơn thanh toán","Mở đơn để kiểm tra trạng thái từ trung tâm.",p.getStudentId(),null,null,null,o.getBranchId(),p.getReceiptId()==null?"PAYMENT_ORDER":"RECEIPT",p.getReceiptId()==null?p.getOrderId():p.getReceiptId().toString(),null,null);p.setDeliveredAt(LocalDateTime.now(com.example.finance.common.BusinessTime.ZONE));}}
}
