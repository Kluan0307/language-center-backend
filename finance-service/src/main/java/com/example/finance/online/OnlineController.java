package com.example.finance.online;
import com.example.finance.common.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor
public class OnlineController {
 private final OnlineOrderService service; private final ReconcileService reconciliation; private final ReconcileJournal journal;
 @GetMapping("/api/v1/finance/student/me/payment-contexts") @PreAuthorize("hasRole('STUDENT')") public java.util.List<OnlineOrderService.PaymentContext> contexts(@RequestParam java.util.List<Long> tuitionIds){return service.contexts(tuitionIds);}
 @GetMapping("/api/v1/finance/student/me/tuitions/{id}/payment-context") @PreAuthorize("hasRole('STUDENT')") public OnlineOrderService.PaymentContext context(@PathVariable Long id){return service.context(id);}
 @GetMapping("/api/v1/finance/student/me/payment-config") @PreAuthorize("hasRole('STUDENT')") public OnlineOrderService.Settings settings(){return service.settings();}
 @PostMapping("/api/v1/finance/student/me/payment-orders") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('STUDENT')") public OnlineOrderService.View create(@Valid @RequestBody OnlineOrderService.Create c,jakarta.servlet.http.HttpServletRequest request){return service.create(c,request.getRemoteAddr());}
 @GetMapping("/api/v1/finance/student/me/payment-orders") @PreAuthorize("hasRole('STUDENT')") public PageResponse<OnlineOrderService.View> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(required=false) String state,@RequestParam(required=false) Long tuitionId,@RequestParam(defaultValue="false") boolean summary){return service.list(true,page,size,state,tuitionId,null,summary);}
 @GetMapping("/api/v1/finance/student/me/payment-orders/{id}") @PreAuthorize("hasRole('STUDENT')") public OnlineOrderService.View detail(@PathVariable String id){return service.own(id);}
 @GetMapping("/api/v1/finance/student/me/payment-orders/requests/{requestId}") @PreAuthorize("hasRole('STUDENT')") public OnlineOrderService.View recovery(@PathVariable UUID requestId){return service.recover(requestId);}
 @GetMapping("/api/v1/finance/payment-orders") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')") public PageResponse<OnlineOrderService.View> management(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(required=false) String state,@RequestParam(required=false) Long tuitionId,@RequestParam(required=false) Long branchId,@RequestParam(required=false) String keyword,@RequestParam(required=false) Long studentId,@RequestParam(required=false) Long classId,@RequestParam(required=false) Long receiptId,@RequestParam(defaultValue="false") boolean allAssigned){return service.search(false,page,size,state,tuitionId,branchId,false,keyword,studentId,classId,receiptId,allAssigned);}
 @GetMapping("/api/v1/finance/payment-orders/{id}") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')") public OnlineOrderService.View managementDetail(@PathVariable String id){return service.operatorDetail(id);}
 @PostMapping("/api/v1/finance/payment-orders/{id}/reconcile") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')") public OnlineOrderService.View reconcile(@PathVariable String id,@Valid @RequestBody ReconcileService.Request request){return reconciliation.reconcile(id,request);}
 @GetMapping("/api/v1/finance/payment-orders/reconciliations/{requestId}") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')") public ReconcileJournal.Recovery operation(@PathVariable UUID requestId){return journal.recover(requestId);}
 @GetMapping("/api/v1/finance/provider-events") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')") public PageResponse<OnlineOrderService.EventView> events(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.events(page,size);}
}
