package com.example.finance.online;
import com.example.finance.common.BusinessTime;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service @RequiredArgsConstructor
public class ProviderInbox {
 private final ProviderEventRepository events;
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public Long store(VnpayProvider.Verified value,String source){String key=VnpayProvider.sha(String.join("|",source,value.merchantCode(),value.merchantRef(),value.transactionNo(),value.amount().toPlainString(),value.outcome(),String.valueOf(value.paidAt()),value.proofHash()));var old=events.findByEventKey(key);if(old.isPresent())return old.get().getId();var e=new ProviderEvent();e.setEventKey(key);e.setSource(source);e.setMerchantRef(value.merchantRef());e.setMerchantCode(value.merchantCode());e.setTransactionNo(value.transactionNo());e.setAmount(value.amount());e.setOutcome(value.outcome());e.setPaidAt(value.paidAt());e.setReceivedAt(LocalDateTime.now(BusinessTime.ZONE));return events.saveAndFlush(e).getId();}
}
