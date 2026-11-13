package com.example.academic;
import static org.junit.jupiter.api.Assertions.*;
import com.example.academic.common.ApiException;
import com.example.academic.security.*;
import com.example.academic.student.Student;
import com.example.academic.support.*;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:student-support;MODE=MySQL;DB_CLOSE_DELAY=-1") @Transactional
class StudentSupportTest {
 @org.springframework.test.context.bean.override.mockito.MockitoBean com.example.academic.common.RemoteApi remote;
 @Test void financialResourceUsesOwnedFinanceProofAndCannotUseUnverifiedBalance(){org.mockito.Mockito.when(remote.get(org.mockito.ArgumentMatchers.contains("/tuitions/1"),org.mockito.ArgumentMatchers.eq(SupportService.FinanceDetail.class))).thenReturn(new SupportService.FinanceDetail(new SupportService.FinanceItem(5L,"VERIFIED")));var c=new SupportService.Create(UUID.randomUUID(),SupportRequest.Category.PAYMENT,"Tuition question","Please check this tuition amount","TUITION",1L);assertEquals(5L,service.create(c).request().branchId());org.mockito.Mockito.when(remote.get(org.mockito.ArgumentMatchers.contains("/receipts/2"),org.mockito.ArgumentMatchers.eq(SupportService.FinanceDetail.class))).thenThrow(ApiException.missing());assertThrows(ApiException.class,()->service.create(new SupportService.Create(UUID.randomUUID(),SupportRequest.Category.PAYMENT,"Receipt question","Please check this receipt amount","RECEIPT",2L)));}
 @Autowired SupportService service; @Autowired EntityManager em; Student own,other;
 void actor(Role role,Long profile,Long branch){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new CurrentUser(role==Role.STUDENT?profile.equals(own.getId())?101L:102L:200L,profile,branch,role,"actor"),null,List.of()));}
 @BeforeEach void setup(){own=student(101L);other=student(102L);actor(Role.STUDENT,own.getId(),null);}
 Student student(Long account){var s=new Student();s.setFullName("Student"+account);s.setPhone("0900000000");s.setBranchId(3L);s.setAccountId(account);em.persist(s);em.flush();return s;}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 SupportService.Create command(UUID uuid){return new SupportService.Create(uuid,SupportRequest.Category.PROFILE,"Update profile","Please check my profile information",null,null);}
 SupportService.StudentDetail create(){return service.create(command(UUID.randomUUID()));}
 @Test void createRetryUsesOneTicketAndRecoveryIsOwned(){var c=command(UUID.randomUUID());var r=service.create(c);assertEquals(r.request().id(),service.create(c).request().id());assertEquals(1,service.list(true,0,20,null,null,null).totalElements());assertEquals(r.request().id(),((SupportService.StudentDetail)service.recover(c.requestId(),true)).request().id());actor(Role.STUDENT,other.getId(),null);assertThrows(ApiException.class,()->service.recover(c.requestId(),true));}
 @Test void studentCannotReadOtherTicketOrLinkForeignResource(){var r=create();actor(Role.STUDENT,other.getId(),null);assertThrows(ApiException.class,()->service.studentDetail(r.request().id()));assertEquals(0,service.list(true,0,20,null,null,null).totalElements());assertThrows(ApiException.class,()->service.create(new SupportService.Create(UUID.randomUUID(),SupportRequest.Category.PROFILE,"Subject","Long enough description","PROFILE",own.getId())));assertThrows(ApiException.class,()->service.create(new SupportService.Create(UUID.randomUUID(),SupportRequest.Category.GRADE,"Subject","Long enough description","ENROLLMENT",999L)));}
 @Test void internalMessagesNeverLeakAndRepliesAdvanceVersion(){var r=create();actor(Role.STAFF,22L,3L);var staff=(SupportService.StaffDetail)service.reply(r.request().id(),UUID.randomUUID(),r.request().version(),"PRIVATE NOTE",true,false);assertEquals(SupportRequest.Status.OPEN,staff.request().status());assertEquals(2,staff.messages().size());actor(Role.STUDENT,own.getId(),null);var mine=service.studentDetail(r.request().id());assertEquals(1,mine.messages().size());assertFalse(mine.toString().contains("PRIVATE"));assertTrue(mine.request().version()>r.request().version());}
 @Test void publicReplyResolvesAndStudentReopens(){var r=create();Long id=r.request().id();actor(Role.STAFF,22L,3L);var d=(SupportService.StaffDetail)service.reply(id,UUID.randomUUID(),r.request().version(),"We are checking",false,false);assertEquals(SupportRequest.Status.IN_PROGRESS,d.request().status());d=(SupportService.StaffDetail)service.change(id,UUID.randomUUID(),d.request().version(),SupportRequest.Status.RESOLVED,"INTERNAL REASON",false);actor(Role.STUDENT,own.getId(),null);var mine=service.studentDetail(id);assertEquals(SupportRequest.Status.RESOLVED,mine.request().status());assertFalse(mine.toString().contains("INTERNAL"));mine=(SupportService.StudentDetail)service.reply(id,UUID.randomUUID(),mine.request().version(),"I still need help",false,true);assertEquals(SupportRequest.Status.OPEN,mine.request().status());}
 @Test void closeIsFinalAndRetryDoesNotCreateAnotherMessage(){var r=create();Long id=r.request().id();UUID uuid=UUID.randomUUID();var d=(SupportService.StudentDetail)service.change(id,uuid,r.request().version(),SupportRequest.Status.CLOSED,"Closed",true);assertEquals(SupportRequest.Status.CLOSED,d.request().status());var replay=(SupportService.StudentDetail)service.change(id,uuid,r.request().version(),SupportRequest.Status.CLOSED,"Closed",true);assertEquals(d.messages().size(),replay.messages().size());assertThrows(ApiException.class,()->service.reply(id,UUID.randomUUID(),d.request().version(),"Late message",false,true));}
 @Test void staleVersionAndChangedUuidIntentFail(){var r=create();UUID uuid=UUID.randomUUID();service.reply(r.request().id(),uuid,r.request().version(),"My reply",false,true);assertThrows(ApiException.class,()->service.reply(r.request().id(),UUID.randomUUID(),r.request().version(),"Stale reply",false,true));assertThrows(ApiException.class,()->service.reply(r.request().id(),uuid,r.request().version(),"Changed text",false,true));}
 @Test void teachersStudentsAndWrongBranchCannotHandleTickets(){var r=create();assertThrows(ApiException.class,()->service.staffDetail(r.request().id()));actor(Role.TEACHER,22L,3L);assertThrows(ApiException.class,()->service.staffDetail(r.request().id()));actor(Role.STAFF,22L,4L);assertThrows(ApiException.class,()->service.staffDetail(r.request().id()));assertEquals(0,service.list(false,0,20,null,null,null).totalElements());actor(Role.SUPER_ADMIN,22L,null);assertEquals(r.request().id(),service.staffDetail(r.request().id()).request().id());}
 @Test void invalidTransitionsDoNotChangeBusinessData(){var r=create();actor(Role.STAFF,22L,3L);assertThrows(ApiException.class,()->service.change(r.request().id(),UUID.randomUUID(),r.request().version(),SupportRequest.Status.CLOSED,"No skipping",false));assertThrows(ApiException.class,()->service.list(false,0,101,null,null,null));}

 @Test void uuidOrderLinkUsesFinanceOwnershipAndOrderBranchAndKeepsLegacyReplay(){
   String key=UUID.randomUUID().toString();
   org.mockito.Mockito.when(remote.get(org.mockito.ArgumentMatchers.endsWith("/payment-orders/"+key),org.mockito.ArgumentMatchers.eq(SupportService.OrderProof.class))).thenReturn(new SupportService.OrderProof(key,5L,own.getId()));
   var c=new SupportService.Create(UUID.randomUUID(),SupportRequest.Category.PAYMENT,"Payment question","Please review my online payment","PAYMENT_ORDER",null,key);
   var d=service.create(c);assertEquals(key,d.request().resourceKey());assertNull(d.request().resourceId());assertEquals(5L,d.request().branchId());assertEquals(d.request().id(),service.create(c).request().id());
   actor(Role.STAFF,22L,3L);assertThrows(ApiException.class,()->service.staffDetail(d.request().id()));
   actor(Role.STAFF,22L,5L);assertEquals(key,service.staffDetail(d.request().id()).request().resourceKey());
   assertEquals(1,service.search(false,0,20,null,null,null,null,"GD-"+key.substring(0,8),null).totalElements());
   actor(Role.STUDENT,own.getId(),null);assertThrows(ApiException.class,()->service.create(new SupportService.Create(c.requestId(),c.category(),c.subject(),c.description(),"PAYMENT_ORDER",null,UUID.randomUUID().toString())));
 }
 @Test void foreignMalformedOrMixedOrderReferencesAreRejected(){
   String key=UUID.randomUUID().toString();
   org.mockito.Mockito.when(remote.get(org.mockito.ArgumentMatchers.endsWith("/payment-orders/"+key),org.mockito.ArgumentMatchers.eq(SupportService.OrderProof.class))).thenReturn(new SupportService.OrderProof(key,3L,other.getId()));
   assertThrows(ApiException.class,()->service.create(new SupportService.Create(UUID.randomUUID(),SupportRequest.Category.PAYMENT,"Payment issue","Please check this payment order","PAYMENT_ORDER",null,key)));
   assertThrows(ApiException.class,()->service.create(new SupportService.Create(UUID.randomUUID(),SupportRequest.Category.PAYMENT,"Payment issue","Please check this payment order","PAYMENT_ORDER",2L,key)));
   assertThrows(ApiException.class,()->service.create(new SupportService.Create(UUID.randomUUID(),SupportRequest.Category.PAYMENT,"Payment issue","Please check this payment order","PAYMENT_ORDER",null,"../another-order")));
   assertThrows(ApiException.class,()->service.create(new SupportService.Create(UUID.randomUUID(),SupportRequest.Category.PAYMENT,"Payment issue","Please check this payment order","TUITION",null,key)));
   assertEquals(0,service.list(true,0,20,null,null,null).totalElements());
 }
 @Test void queueFiltersBeforePagingAndDoesNotCrossStudentOrBranch(){
   own.setFullName("An% Nguyen");em.flush();var first=create();var second=create();
   actor(Role.STAFF,22L,3L);
   var filtered=service.search(false,0,1,null,SupportRequest.Category.PROFILE,null,own.getId(),"An%","ACTIVE");assertEquals(2,filtered.totalElements());assertEquals(2,filtered.totalPages());assertEquals(1,filtered.content().size());
   assertEquals(0,service.search(false,0,20,null,null,null,other.getId(),null,null).totalElements());
   assertEquals(0,service.search(false,0,20,null,null,null,null,"An_",null).totalElements());
   actor(Role.STAFF,22L,4L);assertEquals(0,service.search(false,0,20,null,null,null,own.getId(),"An",null).totalElements());
 }
 @Test void statusAuditIsStaffOnlyAndRetryDoesNotDuplicateHistory(){
   var r=create();actor(Role.STAFF,22L,3L);var d=service.staffDetail(r.request().id());assertEquals(1,d.statusHistory().size());
   UUID id=UUID.randomUUID();d=(SupportService.StaffDetail)service.change(r.request().id(),id,r.request().version(),SupportRequest.Status.IN_PROGRESS,"PRIVATE STATUS REASON",false);
   assertEquals(2,d.statusHistory().size());var replay=(SupportService.StaffDetail)service.change(r.request().id(),id,r.request().version(),SupportRequest.Status.IN_PROGRESS,"PRIVATE STATUS REASON",false);assertEquals(2,replay.statusHistory().size());
   actor(Role.STUDENT,own.getId(),null);assertFalse(service.studentDetail(r.request().id()).toString().contains("PRIVATE STATUS REASON"));
 }

}
