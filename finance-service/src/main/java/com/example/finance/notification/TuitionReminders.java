package com.example.finance.notification;
import com.example.finance.common.*;
import com.example.finance.ledger.FinanceLedger;
import com.example.finance.tuition.Tuition;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.*;
import org.springframework.transaction.annotation.Transactional;
@Configuration @EnableScheduling @RequiredArgsConstructor @ConditionalOnProperty(name="notifications.jobs.enabled",havingValue="true")
public class TuitionReminders {
 private final EntityManager em; private final FinanceLedger ledger; private final JdbcTemplate jdbc;
 @Scheduled(fixedDelay=3600000,initialDelay=60000) @Transactional public void run(){var today=LocalDate.now(BusinessTime.ZONE);var days=List.of(today.plusDays(3),today,today.minusDays(1),today.minusDays(7));
  for(var t:em.createQuery("select t from Tuition t where t.dueDate in :days and t.status<>'CANCELLED' and t.studentId is not null and t.enrollmentId is not null and t.currency='VND' and t.paidAmount<t.amount",Tuition.class).setParameter("days",days).getResultList()){
   Long proof=jdbc.queryForObject("SELECT COUNT(*) FROM enrollments e JOIN school_classes c ON c.id=e.class_id WHERE e.id=? AND e.student_id=? AND c.id=? AND c.branch_id=? AND (e.billing_enrollment_id IS NULL OR e.billing_enrollment_id=e.id)",Long.class,t.getEnrollmentId(),t.getStudentId(),t.getClassId(),t.getBranchId());if(proof==null||proof!=1)continue;try{ledger.requireSynced(t);}catch(ApiException ex){continue;}
   String period=t.getDueDate().equals(today.plusDays(3))?"DUE_SOON":t.getDueDate().equals(today)?"DUE_TODAY":t.getDueDate().equals(today.minusDays(1))?"OVERDUE_1":"OVERDUE_7";
   StudentEvents.add(em,"finance:reminder:"+t.getId()+":"+t.getDueDate()+":"+period,"TUITION",period,"Nhắc kiểm tra học phí","Khoản học phí có mốc hạn cần lưu ý. Mở chi tiết để xem số dư và thời hạn hiện hành.",t.getStudentId(),null,null,null,t.getBranchId(),"TUITION",t.getId().toString(),null,null);
  }
 }
}
