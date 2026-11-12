package com.example.finance;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.finance.common.*;
import com.example.finance.online.*;
import com.example.finance.payment.*;
import com.example.finance.ledger.*;
import com.example.finance.tuition.*;
import com.example.finance.student.StudentFinanceService;
import com.example.finance.security.*;
import jakarta.persistence.EntityManager;
import java.math.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.*;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:online-payments;MODE=MySQL;DB_CLOSE_DELAY=-1","payment.vnpay.enabled=true","payment.vnpay.merchant-code=TESTCODE","payment.vnpay.secret=test-secret-for-isolated-protocol-qualification-only-64-characters","payment.vnpay.checkout-url=http://127.0.0.1:29999/checkout","payment.vnpay.query-url=http://127.0.0.1:29999/query","payment.vnpay.return-url=http://127.0.0.1:28180/api/v1/finance/payment-providers/vnpay/return","payment.jobs.enabled=false"}) @ActiveProfiles("payment-test")
class OnlinePaymentTest {
 @Autowired OnlineOrderService service; @Autowired OnlineOrderRepository orders; @Autowired OnlineSettlement settlement; @Autowired ProviderInbox inbox; @Autowired VnpayProvider provider; @Autowired VnpayController controller; @Autowired PaymentProviderConfig config; @Autowired ProviderEventRepository events; @Autowired ProviderTransactionRepository transactions; @Autowired PaymentService manual; @Autowired CorrectionService corrections; @Autowired ReconcileService reconcile; @Autowired ReconcileJournal journal; @Autowired JdbcTemplate jdbc; @Autowired EntityManager em; @Autowired PlatformTransactionManager tm; @Autowired JsonMapper json;
 @MockitoBean StudentFinanceService finance;
 Tuition t; TransactionTemplate tx; static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
 void actor(Role role,long account,long profile,long branch){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new CurrentUser(account,profile,branch<=0?null:branch,role,"test"),null,List.of()));}
 @BeforeEach void setup(){tx=new TransactionTemplate(tm);for(String table:List.of("payment_outbox","payment_reconcile_operations","payment_provider_events","payment_provider_transactions","payment_orders","payment_corrections","payments","tuition_adjustments","tuition"))jdbc.update("DELETE FROM "+table);ReflectionTestUtils.setField(config,"enabled",true);ReflectionTestUtils.setField(config,"queryUrl","http://127.0.0.1:29999/query");tx.executeWithoutResult(s->{t=new Tuition();t.setEnrollmentId(101L);t.setClassId(9L);t.setBranchId(3L);t.setStudentId(11L);t.setOriginalAmount(new BigDecimal("2000"));t.setAmount(new BigDecimal("2000"));t.setDueDate(LocalDate.now(BusinessTime.ZONE).plusDays(2));em.persist(t);});var proof=new StudentFinanceService.TuitionView(t.getId(),101L,List.of(101L),"Class","English",3L,"VND","2000.00","0.00","2000.00","0.00","2000.00","0.00",t.getDueDate(),"UNPAID",false,false,"VERIFIED",null,0);when(finance.detail(t.getId())).thenReturn(new StudentFinanceService.TuitionDetail(proof,List.of()));actor(Role.STUDENT,101,11,0);}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void paymentContextSelectsPayResumeAndSupportWithoutCreatingOrders(){
   assertEquals("PAY",service.context(t.getId()).action());assertEquals(0,orders.count());
   var order=create("1000");var context=service.context(t.getId());
   assertEquals("RESUME",context.action());assertEquals(order.id(),context.order().id());assertNull(context.order().checkoutUrl());assertEquals("Class",context.order().className());
   pay(order.id(),"3211","1200");assertEquals("SUPPORT",service.context(t.getId()).action());assertEquals("PAYMENT_REVIEW_REQUIRED",service.context(t.getId()).reason());
 }
 @Test void batchContextUsesOwnedLabelsAndRejectsAnyForeignTuition(){
   var proof=finance.detail(t.getId()).tuition();when(finance.labels(List.of(t.getId()))).thenReturn(Map.of(t.getId(),proof));
   var order=create("1000");clearInvocations(finance);
   var contexts=service.contexts(List.of(t.getId()));assertEquals(1,contexts.size());assertEquals(order.id(),contexts.getFirst().order().id());
   verify(finance,never()).detail(anyLong());
   when(finance.labels(List.of(t.getId(),9999L))).thenReturn(Map.of(t.getId(),proof));
   assertThrows(ApiException.class,()->service.contexts(List.of(t.getId(),9999L)));
 }
 @Test void expiredOrderDoesNotBlockButUnresolvedReviewStillDoes(){
   var order=create("1000");expiry(order.id());assertEquals("PAY",service.context(t.getId()).action());
   jdbc.update("UPDATE payment_orders SET review_code='CHECK_MONEY' WHERE id=?",order.id());
   assertEquals("SUPPORT",service.context(t.getId()).action());
   var error=assertThrows(PaymentFlowException.class,()->create("1000"));assertEquals("PAYMENT_REVIEW_REQUIRED",error.getCode());assertEquals(1,orders.count());
 }
 @Test void errorCodesAllowRecoveryWithoutLeakingAnotherAccountsOrder(){
   var first=create("1000");var conflict=assertThrows(PaymentFlowException.class,()->create("1000"));
   assertEquals("PAYMENT_ALREADY_PENDING",conflict.getCode());assertEquals(first.id(),conflict.getOrderId());
   // Same student re-provisioned to another account: do not expose an old account's order.
   actor(Role.STUDENT,102,11,0);var context=service.context(t.getId());assertEquals("SUPPORT",context.action());assertNull(context.order());
   conflict=assertThrows(PaymentFlowException.class,()->create("1000"));assertNull(conflict.getOrderId());
   actor(Role.STUDENT,101,11,0);var changed=assertThrows(PaymentFlowException.class,()->create("2500"));assertEquals("PAYMENT_BALANCE_CHANGED",changed.getCode());
 }
 @Test void effectiveExpiryIsFilteredAndPaginatedInDatabase(){
   var old=create("500");expiry(old.id());var current=create("600");
   var pending=service.list(true,0,1,"PENDING",null,null,true);assertEquals(1,pending.totalElements());assertEquals(current.id(),pending.content().getFirst().id());assertNull(pending.content().getFirst().checkoutUrl());
   var expired=service.list(true,0,1,"EXPIRED",null,null,true);assertEquals(1,expired.totalElements());assertEquals(old.id(),expired.content().getFirst().id());
   assertEquals(2,service.list(true,0,1,null,null,null,true).totalPages());assertEquals(1,service.list(true,1,1,null,null,null,true).content().size());
   assertNotNull(service.list(true,0,10,"PENDING",null,null).content().getFirst().checkoutUrl());
   jdbc.update("UPDATE payment_orders SET review_code='CHECK' WHERE id=?",old.id());assertEquals(1,service.list(true,0,20,"REVIEW_REQUIRED",null,null,true).totalElements());
 }
 @Test void disabledProviderContextCannotInviteNewCheckout(){
   ReflectionTestUtils.setField(config,"enabled",false);assertEquals("UNAVAILABLE",service.context(t.getId()).action());
   var error=assertThrows(PaymentFlowException.class,()->create("1000"));assertEquals("PAYMENT_DISABLED",error.getCode());assertEquals(0,orders.count());
 }
 @Test void ledgerRaceReturnsDefiniteReviewRejectionWithoutCreatingOrder(){
   jdbc.update("UPDATE tuition SET paid_amount=1 WHERE id=?",t.getId());
   var error=assertThrows(PaymentFlowException.class,()->create("1000"));assertEquals("PAYMENT_REVIEW_REQUIRED",error.getCode());assertEquals(0,orders.count());
 }
 @Test void unavailableAcademicLabelsDoNotHideOwnedFinancialOrderStatus(){
   var order=create("1000");when(finance.detail(t.getId())).thenThrow(new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Academic temporarily unavailable"));
   when(finance.labels(anyCollection())).thenThrow(new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Academic temporarily unavailable"));
   assertEquals("PENDING",service.own(order.id()).state());assertNull(service.own(order.id()).className());
   assertEquals(order.id(),service.list(true,0,20,null,null,null,true).content().getFirst().id());
   assertThrows(ApiException.class,()->service.context(t.getId()));assertThrows(ApiException.class,()->create("1000"));
 }
 @Test void returnPageHasSafeMobileNavigationButCannotConfirmMoney(){
   var order=create("1000");var page=controller.back(evidence(order.id(),"3212","1000","00","00"));
   assertTrue(page.getBody().contains("Quay lại ứng dụng"));assertTrue(page.getBody().contains("width=device-width"));
   assertTrue(page.getHeaders().getFirst("Content-Security-Policy").contains("default-src 'none'"));
   assertTrue(page.getHeaders().getCacheControl().contains("no-store"));assertEquals(0,receipts());assertEquals("PENDING",service.own(order.id()).state());
 }
 OnlineOrderService.Create command(UUID id,String amount){return new OnlineOrderService.Create(id,t.getId(),amount,"VND");}
 OnlineOrderService.View create(String amount){return service.create(command(UUID.randomUUID(),amount),"127.0.0.1");}
 MultiValueMap<String,String> evidence(String id,String number,String amount,String response,String status){var o=orders.findById(id).orElseThrow();var values=new LinkedMultiValueMap<String,String>();values.add("vnp_TmnCode","TESTCODE");values.add("vnp_TxnRef",o.getMerchantRef());values.add("vnp_Amount",new BigDecimal(amount).movePointRight(2).toBigIntegerExact().toString());values.add("vnp_ResponseCode",response);values.add("vnp_TransactionStatus",status);values.add("vnp_TransactionNo",number);values.add("vnp_PayDate",DATE.format(LocalDateTime.now(BusinessTime.ZONE)));values.add("vnp_SecureHash",VnpayProvider.hmac(config.getSecret(),VnpayProvider.canonical(values.toSingleValueMap())));return values;}
 Long receive(MultiValueMap<String,String> values){return inbox.store(provider.ipn(values),"IPN");}
 String pay(String id,String number,String amount){return settlement.process(receive(evidence(id,number,amount,"00","00")),false);}
 BigDecimal paid(){return jdbc.queryForObject("SELECT paid_amount FROM tuition WHERE id=?",BigDecimal.class,t.getId());}
 long receipts(){return jdbc.queryForObject("SELECT COUNT(*) FROM payments",Long.class);}
 void expiry(String id){jdbc.update("UPDATE payment_orders SET expires_at=? WHERE id=?",LocalDateTime.now(BusinessTime.ZONE).minusMinutes(1),id);}
 @Test void createRetryAndRecoveryReturnOneIntent(){UUID request=UUID.randomUUID();var first=service.create(command(request,"1000"),"127.0.0.1");var second=service.create(command(request,"1000.00"),"127.0.0.1");assertEquals(first.id(),second.id());assertEquals(1,orders.count());assertEquals(first.id(),service.recover(request).id());assertThrows(ApiException.class,()->service.create(command(request,"500"),"127.0.0.1"));assertThrows(ApiException.class,()->create("1000"));}
 @Test void signingUsesGatewayAmountAndNoIdentitySecret(){var order=create("1000.25");assertEquals("1000.25",order.amount());assertTrue(order.checkoutUrl().contains("vnp_Amount=100025"));assertFalse(order.checkoutUrl().contains(config.getSecret()));assertFalse(order.checkoutUrl().contains("Bearer"));assertEquals("PROTOCOL_TEST",order.environment());}
 @Test void disabledProviderDoesNotCreateFakeOrder(){ReflectionTestUtils.setField(config,"enabled",false);assertFalse(service.settings().enabled());assertThrows(ApiException.class,()->create("1000"));assertEquals(0,orders.count());}
 @Test void overpayZeroAndWrongCurrencyRejected(){assertThrows(ApiException.class,()->create("2001"));assertThrows(ApiException.class,()->create("0"));assertThrows(ApiException.class,()->service.create(new OnlineOrderService.Create(UUID.randomUUID(),t.getId(),"1000","USD"),"127.0.0.1"));assertEquals(0,orders.count());}
 @Test void actualCallbackAllocatesOnceAndEmitsAfterCommit(){var order=create("1000");var values=evidence(order.id(),"1001","1000","00","00");Long event=receive(values);assertEquals("00",settlement.process(event,false));assertEquals("00",settlement.process(receive(values),false));assertEquals(1,receipts());assertEquals(0,new BigDecimal("1000").compareTo(paid()));assertEquals("SUCCEEDED",service.own(order.id()).state());assertEquals(1,events.count());assertEquals(1,transactions.count());assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM payment_outbox WHERE type='PAYMENT_SUCCEEDED'",Integer.class));}
 @Test void concurrentProcessorsCannotAllocateTwice() throws Exception {var order=create("1000");Long event=receive(evidence(order.id(),"1002","1000","00","00"));try(var pool=Executors.newFixedThreadPool(2)){var a=pool.submit(()->settlement.process(event,false));var b=pool.submit(()->settlement.process(event,false));assertEquals("00",a.get(10,TimeUnit.SECONDS));assertEquals("00",b.get(10,TimeUnit.SECONDS));}assertEquals(1,receipts());assertEquals(0,new BigDecimal("1000").compareTo(paid()));}
 @Test void invalidSignatureMerchantOrDuplicateParameterNeverEnterInbox(){var order=create("1000");var bad=evidence(order.id(),"1003","1000","00","00");bad.set("vnp_Amount","150000");assertThrows(ApiException.class,()->receive(bad));var duplicate=evidence(order.id(),"1003","1000","00","00");duplicate.add("vnp_Amount","100000");assertThrows(ApiException.class,()->receive(duplicate));assertEquals(0,events.count());assertEquals(0,receipts());}
 @Test void signedWrongAmountIsDurablyHeldForReview(){var order=create("1000");assertEquals("04",pay(order.id(),"1004","1200"));assertEquals("REVIEW_REQUIRED",service.own(order.id()).state());assertEquals(0,receipts());assertEquals(1,transactions.count());assertEquals("REVIEW_REQUIRED",events.findAll().getFirst().getStatus());assertEquals(0,paid().signum());}
 @Test void expiredPaymentIsNotDroppedAndNeedsVerifiedReconciliation(){var order=create("1000");expiry(order.id());assertEquals("00",pay(order.id(),"1005","1000"));assertEquals("REVIEW_REQUIRED",service.own(order.id()).state());assertEquals(0,receipts());Long query=inbox.store(provider.ipn(evidence(order.id(),"1005","1000","00","00")),"QUERY");settlement.process(query,true);assertEquals("SUCCEEDED",service.own(order.id()).state());assertEquals(1,receipts());}
 @Test void changedOutstandingHoldsRealMoneyWithoutNegativeDebt(){var order=create("1000");actor(Role.SUPER_ADMIN,201,21,0);manual.create(new PaymentDto.Request(t.getId(),new BigDecimal("1500"),Payment.PaymentMethod.CASH,null,UUID.randomUUID().toString(),null));actor(Role.STUDENT,101,11,0);pay(order.id(),"1006","1000");assertEquals("REVIEW_REQUIRED",service.own(order.id()).state());assertEquals(1,receipts());assertEquals(0,new BigDecimal("1500").compareTo(paid()));}
 @Test void manualCashRacingCallbackNeverExceedsDebt() throws Exception {var order=create("2000");Long event=receive(evidence(order.id(),"1007","2000","00","00"));try(var pool=Executors.newFixedThreadPool(2)){var a=pool.submit(()->settlement.process(event,false));var b=pool.submit(()->{actor(Role.SUPER_ADMIN,201,21,0);try{return manual.create(new PaymentDto.Request(t.getId(),new BigDecimal("2000"),Payment.PaymentMethod.CASH,null,UUID.randomUUID().toString(),null));}catch(ApiException ex){return null;}finally{SecurityContextHolder.clearContext();}});a.get(10,TimeUnit.SECONDS);b.get(10,TimeUnit.SECONDS);}assertEquals(1,receipts());assertEquals(0,new BigDecimal("2000").compareTo(paid()));}
 @Test void transactionNumberCannotCreditTwoOrders(){var first=create("1000");pay(first.id(),"1008","1000");var second=create("500");pay(second.id(),"1008","500");assertEquals("REVIEW_REQUIRED",service.own(second.id()).state());assertEquals("SUCCEEDED",service.own(first.id()).state());assertEquals(1,receipts());}
 @Test void failedCallbackNeverDowngradesSucceededOrder(){var order=create("1000");pay(order.id(),"1009","1000");settlement.process(receive(evidence(order.id(),"1009","1000","24","02")),false);assertEquals("SUCCEEDED",service.own(order.id()).state());assertEquals(1,receipts());}
 @Test void responseSuccessWithFailedTransactionCannotCreateReceipt(){var order=create("1000");settlement.process(receive(evidence(order.id(),"1010","1000","00","02")),false);assertEquals("FAILED",service.own(order.id()).state());assertEquals(0,receipts());}
 @Test void returnPageCannotMarkOrderPaid(){var order=create("1000");var response=controller.back(evidence(order.id(),"1011","1000","00","00"));assertTrue(response.getBody().contains(order.id()));assertEquals("PENDING",service.own(order.id()).state());assertEquals(0,receipts());assertEquals(0,events.count());}
 @Test void durableInboxCanRecoverAfterCallerDisappears(){var order=create("1000");receive(evidence(order.id(),"1012","1000","00","00"));assertEquals(0,receipts());new OnlineJobs(orders,events,settlement).run();assertEquals(1,receipts());assertEquals("SUCCEEDED",service.own(order.id()).state());}
 @Test void expiredOlderOrderCanEnterReviewWhileNewOneIsPending(){var first=create("1000");expiry(first.id());var next=create("1000");pay(first.id(),"1013","1000");assertEquals("REVIEW_REQUIRED",service.own(first.id()).state());assertEquals("PENDING",service.own(next.id()).state());assertEquals(0,receipts());}
 @Test void ownerAndRoleChecksDoNotTrustBranchHeaderOrOrderId(){var order=create("1000");actor(Role.STUDENT,102,22,3);assertThrows(ApiException.class,()->service.own(order.id()));assertEquals(0,service.list(true,0,20,null,null,null).totalElements());actor(Role.STAFF,202,22,4);assertThrows(ApiException.class,()->service.operatorDetail(order.id()));actor(Role.STUDENT,101,11,0);assertThrows(ApiException.class,()->service.operatorDetail(order.id()));}
 @Test void receiptCorrectionDoesNotRewriteOrderSuccess(){var order=create("1000");pay(order.id(),"1014","1000");actor(Role.SUPER_ADMIN,201,21,0);Long payment=orders.findById(order.id()).orElseThrow().getReceiptId();Long version=jdbc.queryForObject("SELECT version FROM tuition WHERE id=?",Long.class,t.getId());corrections.create(payment,new CorrectionService.Request(PaymentCorrection.Type.VOID,new BigDecimal("1000"),null,null,"Void test receipt",UUID.randomUUID().toString(),version));assertEquals("SUCCEEDED",orders.findById(order.id()).orElseThrow().getState());assertEquals(0,paid().signum());}
 @Test void reservedManualKeyCannotImpersonateOnlineSettlement(){actor(Role.SUPER_ADMIN,201,21,0);assertThrows(ApiException.class,()->manual.create(new PaymentDto.Request(t.getId(),BigDecimal.ONE,Payment.PaymentMethod.CASH,null,"ONLINE-forged",null)));assertEquals(0,receipts());}
 @Test void queryDrMustVerifySignedServerResponseAndAuditIntent() throws Exception {var order=create("1000");expiry(order.id());var server=com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/query",exchange->{try{var request=json.readValue(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8),new tools.jackson.core.type.TypeReference<Map<String,String>>(){});Map<String,String> response=new LinkedHashMap<>();response.put("vnp_ResponseId",UUID.randomUUID().toString().replace("-",""));response.put("vnp_Command","querydr");response.put("vnp_ResponseCode","00");response.put("vnp_Message","Success");response.put("vnp_TmnCode","TESTCODE");response.put("vnp_TxnRef",request.get("vnp_TxnRef"));response.put("vnp_Amount","100000");response.put("vnp_BankCode","NCB");response.put("vnp_PayDate",DATE.format(LocalDateTime.now(BusinessTime.ZONE)));response.put("vnp_TransactionNo","1015");response.put("vnp_TransactionType","01");response.put("vnp_TransactionStatus","00");response.put("vnp_OrderInfo",request.get("vnp_OrderInfo"));response.put("vnp_PromotionCode","");response.put("vnp_PromotionAmount","");response.put("vnp_SecureHash",VnpayProvider.hmac(config.getSecret(),String.join("|",response.values())));byte[] bytes=json.writeValueAsString(response).getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);}finally{exchange.close();}});server.start();try{ReflectionTestUtils.setField(config,"queryUrl","http://127.0.0.1:"+server.getAddress().getPort()+"/query");actor(Role.SUPER_ADMIN,201,21,0);var request=new ReconcileService.Request(UUID.randomUUID(),orders.findById(order.id()).orElseThrow().getVersion(),"Verified late settlement");assertEquals("SUCCEEDED",reconcile.reconcile(order.id(),request).state());assertEquals("SUCCEEDED",reconcile.reconcile(order.id(),request).state());assertEquals("DONE",journal.recover(request.requestId()).operationStatus());assertEquals(1,receipts());}finally{server.stop(0);}}

 @Test void disablingCheckoutStillAcceptsAuthenticInFlightPayment(){var order=create("1000");ReflectionTestUtils.setField(config,"enabled",false);assertEquals("00",pay(order.id(),"1016","1000"));assertEquals("SUCCEEDED",service.own(order.id()).state());assertNull(service.own(order.id()).checkoutUrl());assertEquals(1,receipts());}
 @Test void unknownVerifiedReferenceIsRetainedWithoutInventingReceipt(){var order=create("1000");var v=evidence(order.id(),"1017","1000","00","00");v.set("vnp_TxnRef","unknownref");v.remove("vnp_SecureHash");v.set("vnp_SecureHash",VnpayProvider.hmac(config.getSecret(),VnpayProvider.canonical(v.toSingleValueMap())));assertEquals("01",settlement.process(receive(v),false));assertEquals(1,transactions.count());assertEquals("UNKNOWN_ORDER",transactions.findAll().getFirst().getReviewCode());assertEquals(0,receipts());}
 @Test void invalidQuerySignatureIsDependencyFailureAndLeavesAuditPending() throws Exception {var order=create("1000");var server=com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/query",e->{byte[] bytes="{\"vnp_SecureHash\":\"invalid\"}".getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type","application/json");e.sendResponseHeaders(200,bytes.length);e.getResponseBody().write(bytes);e.close();});server.start();try{ReflectionTestUtils.setField(config,"queryUrl","http://127.0.0.1:"+server.getAddress().getPort()+"/query");actor(Role.SUPER_ADMIN,201,21,0);var request=new ReconcileService.Request(UUID.randomUUID(),orders.findById(order.id()).orElseThrow().getVersion(),"Check unverifiable provider response");ApiException error=assertThrows(ApiException.class,()->reconcile.reconcile(order.id(),request));assertEquals(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,error.getStatus());assertEquals("PENDING",journal.recover(request.requestId()).operationStatus());assertEquals(0,events.count());assertEquals(0,receipts());}finally{server.stop(0);}}

 @Test void queryingAllocatedReceiptDoesNotHideAnotherUnallocatedBankTransaction(){var order=create("1000");pay(order.id(),"1020","1000");pay(order.id(),"1021","1000");assertTrue(service.own(order.id()).reviewRequired());Long query=inbox.store(provider.ipn(evidence(order.id(),"1020","1000","00","00")),"QUERY");settlement.process(query,true);assertTrue(service.own(order.id()).reviewRequired());assertEquals(1,receipts());assertEquals(2,transactions.count());assertThrows(ApiException.class,()->create("500"));}
}

