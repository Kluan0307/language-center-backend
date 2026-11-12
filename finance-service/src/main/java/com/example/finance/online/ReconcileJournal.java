package com.example.finance.online;
import com.example.finance.common.*;
import com.example.finance.security.*;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service @RequiredArgsConstructor
public class ReconcileJournal {
 private final EntityManager em; private final OnlineOrderService orders; private final OnlineOrderRepository repository; private final com.example.finance.tuition.TuitionRepository tuition;
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public ReconcileOperation start(String id,UUID requestId,long expected,String reason){if(reason==null||reason.trim().length()<3||reason.trim().length()>500)throw ApiException.bad("Reconciliation reason must contain 3..500 characters");if(!Set.of(Role.SUPER_ADMIN,Role.ADMIN).contains(Users.current().role()))throw ApiException.forbidden();var initial=orders.operatorOrder(id);String fingerprint=VnpayProvider.sha(id+"|"+expected+"|"+reason.trim());var prior=em.find(ReconcileOperation.class,requestId.toString());if(prior!=null){if(!prior.getActorId().equals(Users.current().accountId()))throw ApiException.missing();if(!prior.getFingerprint().equals(fingerprint))throw ApiException.conflict("Reconciliation intent changed");return prior;}em.detach(initial);tuition.lock(initial.getTuitionId()).orElseThrow(ApiException::missing);var o=repository.lock(id).orElseThrow(ApiException::missing);em.refresh(o);if(o.getVersion()!=expected)throw ApiException.conflict("Order changed; reload before reconciliation");var op=new ReconcileOperation();op.setId(requestId.toString());op.setOrderId(id);op.setActorId(Users.current().accountId());op.setBranchId(o.getBranchId());op.setFingerprint(fingerprint);op.setReason(reason.trim());op.setCreatedAt(LocalDateTime.now(BusinessTime.ZONE));em.persist(op);em.flush();return op;}
 @Transactional public void done(String id,Long event){var op=em.find(ReconcileOperation.class,id);op.setStatus("DONE");op.setEventId(event);op.setCompletedAt(LocalDateTime.now(BusinessTime.ZONE));}
 public record Recovery(String operationStatus,OnlineOrderService.View order) {}
 @Transactional(readOnly=true) public Recovery recover(UUID id){var op=em.find(ReconcileOperation.class,id.toString());if(op==null||!op.getActorId().equals(Users.current().accountId()))throw ApiException.missing();return new Recovery(op.getStatus(),orders.operatorDetail(op.getOrderId()));}
}
