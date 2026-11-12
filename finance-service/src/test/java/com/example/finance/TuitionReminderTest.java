package com.example.finance;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.example.finance.notification.*;
import com.example.finance.ledger.FinanceLedger;
import com.example.finance.tuition.Tuition;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:tuition-reminders;MODE=MySQL;DB_CLOSE_DELAY=-1","notifications.jobs.enabled=false"}) @Transactional
class TuitionReminderTest {
 @Autowired EntityManager em; @Autowired FinanceLedger ledger; @MockitoBean JdbcTemplate jdbc;
 Tuition t;
 @BeforeEach void setup(){t=new Tuition();t.setEnrollmentId(101L);t.setClassId(9L);t.setBranchId(3L);t.setStudentId(11L);t.setAmount(new BigDecimal("1000"));t.setOriginalAmount(new BigDecimal("1000"));t.setDueDate(LocalDate.now(com.example.finance.common.BusinessTime.ZONE));em.persist(t);em.flush();when(jdbc.queryForObject(anyString(),eq(Long.class),eq(101L),eq(11L),eq(9L),eq(3L))).thenReturn(1L);}
 long events(){em.flush();return em.createQuery("select count(e) from StudentEvent e",Long.class).getSingleResult();}
 void run(){new TuitionReminders(em,ledger,jdbc).run();}
 @Test void reminderIsBoundedByTuitionDueDateAndPeriod(){run();run();assertEquals(1,events());assertEquals("DUE_TODAY",em.createQuery("select e.type from StudentEvent e",String.class).getSingleResult());}
 @Test void cancelledAndFullyPaidTuitionAreNotReminded(){t.setStatus(Tuition.Status.CANCELLED);em.flush();run();assertEquals(0,events());t.setStatus(Tuition.Status.PAID);t.setPaidAmount(t.getAmount());em.flush();run();assertEquals(0,events());}
 @Test void childOrWrongOwnershipProofDoesNotCreateReminder(){when(jdbc.queryForObject(anyString(),eq(Long.class),eq(101L),eq(11L),eq(9L),eq(3L))).thenReturn(0L);run();assertEquals(0,events());}
 @Test void unverifiedLedgerDoesNotAssertDebt(){t.setPaidAmount(BigDecimal.ONE);em.flush();run();assertEquals(0,events());}
}
