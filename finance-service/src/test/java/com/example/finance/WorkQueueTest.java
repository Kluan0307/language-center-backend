package com.example.finance;
import static org.junit.jupiter.api.Assertions.*;
import com.example.finance.common.*;
import com.example.finance.security.*;
import com.example.finance.online.*;
import com.example.finance.tuition.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:finance-work-queue;MODE=MySQL;DB_CLOSE_DELAY=-1","payment.jobs.enabled=false","notification.jobs.enabled=false"}) @Transactional
class WorkQueueTest {
 @Autowired EntityManager em; @Autowired com.example.finance.stats.WorkQueueController queue; @Autowired TuitionService tuition; @Autowired OnlineOrderService online;
 void actor(Role role,List<Long> ids){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new CurrentUser(100L,11L,ids.isEmpty()?null:ids.getFirst(),role,"operator",ids),null,List.of(new SimpleGrantedAuthority("ROLE_"+role))));}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 Tuition bill(long branch,boolean cancelled){var t=new Tuition();t.setStudentId(11L);t.setClassId(9L);t.setBranchId(branch);t.setAmount(new BigDecimal("1000"));t.setDueDate(LocalDate.now(BusinessTime.ZONE).minusDays(1));if(cancelled)t.setStatus(Tuition.Status.CANCELLED);em.persist(t);return t;}
 void order(Tuition t,String state,String review){var o=new OnlineOrder();o.setId(UUID.randomUUID().toString());o.setAccountId(100L);o.setStudentId(11L);o.setTuitionId(t.getId());o.setBillingRootId(1L);o.setBranchId(t.getBranchId());o.setRequestId(UUID.randomUUID().toString());o.setFingerprint("a".repeat(64));o.setMerchantRef(UUID.randomUUID().toString().replace("-",""));o.setMerchantCode("TEST");o.setAmount(new BigDecimal("100"));o.setClientIp("127.0.0.1");o.setState(state);o.setReviewCode(review);o.setCreatedAt(LocalDateTime.now());o.setUpdatedAt(LocalDateTime.now());o.setExpiresAt(LocalDateTime.now().plusHours(1));em.persist(o);}
 @BeforeEach void seed(){var a=bill(3,false);var b=bill(4,false);var foreign=bill(5,false);bill(3,true);order(a,"REVIEW_REQUIRED",null);order(b,"SUCCEEDED","NEEDS_CHECK");order(foreign,"REVIEW_REQUIRED",null);em.flush();actor(Role.ADMIN,List.of(3L,4L));}
 @Test void countsMatchCompleteScopedDestinationQueriesBeforePagination(){var all=queue.get(null,true);assertEquals(2,all.tuitionOverdue());assertEquals(2,all.ordersReview());assertNotNull(all.observedAt());assertEquals(all.tuitionOverdue(),tuition.list(0,1,null,null,null,null,null,true,true).totalElements());assertEquals(all.ordersReview(),online.search(false,0,1,"REVIEW_REQUIRED",null,null,false,null,null,null,null,true).totalElements());assertEquals(1,tuition.list(1,1,null,null,null,null,null,true,true).content().size());assertEquals(1,queue.get(null,false).tuitionOverdue());}
 @Test void flagsNeverGrantUnassignedBranchesOrStaffAggregateAccess(){assertThrows(ApiException.class,()->queue.get(5L,false));assertThrows(ApiException.class,()->queue.get(3L,true));actor(Role.STAFF,List.of(3L));assertThrows(ApiException.class,()->queue.get(null,true));assertThrows(ApiException.class,()->tuition.list(0,1,null,null,null,null,null,true,true));assertThrows(ApiException.class,()->online.search(false,0,1,null,null,null,false,null,null,null,null,true));assertEquals(1,queue.get(null,false).ordersReview());}
 @Test void zeroAndForbiddenAreDistinctAndSuperAdminCanSelectAnyBranch(){actor(Role.SUPER_ADMIN,List.of());assertEquals(3,queue.get(null,false).ordersReview());assertEquals(0,queue.get(99L,false).tuitionOverdue());for(var role:List.of(Role.TEACHER,Role.STUDENT)){actor(role,List.of(3L));assertThrows(org.springframework.security.access.AccessDeniedException.class,()->queue.get(null,false));}}
}
