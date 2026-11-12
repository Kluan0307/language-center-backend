package com.example.finance.online;
import jakarta.validation.constraints.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@Service @RequiredArgsConstructor
public class ReconcileService {
 private final ReconcileJournal journal; private final OnlineOrderService orders; private final VnpayProvider provider; private final ProviderInbox inbox; private final OnlineSettlement settlement;
 public record Request(@NotNull UUID requestId,@Min(0) long expectedVersion,@NotBlank @Size(min=3,max=500) String reason) {}
 public OnlineOrderService.View reconcile(String id,Request request){var op=journal.start(id,request.requestId(),request.expectedVersion(),request.reason());if(op.getStatus().equals("DONE"))return orders.operatorDetail(id);var order=orders.operatorOrder(id);var evidence=provider.query(order);Long event=inbox.store(evidence,"QUERY");settlement.process(event,true);journal.done(op.getId(),event);return orders.operatorDetail(id);}
}
