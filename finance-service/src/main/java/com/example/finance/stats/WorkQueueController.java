package com.example.finance.stats;

import com.example.finance.common.BusinessTime;
import com.example.finance.security.BranchScope;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/v1/finance/stats/work-queue")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
public class WorkQueueController {
  private final BranchScope scope;
  private final EntityManager em;
  public record Counts(long tuitionOverdue,long ordersReview,LocalDate businessDate,OffsetDateTime observedAt) {}
  @GetMapping @Transactional(readOnly=true)
  public Counts get(@RequestParam(required=false) Long branchId,@RequestParam(defaultValue="false") boolean allAssigned){
    var branches=scope.selection(branchId,allAssigned);var today=LocalDate.now(BusinessTime.ZONE);
    return new Counts(count("Tuition","r.status<>'CANCELLED' and r.dueDate<:today and r.amount>r.paidAmount",branches,today),count("OnlineOrder","(r.state='REVIEW_REQUIRED' or r.reviewCode is not null)",branches,null),today,OffsetDateTime.now(BusinessTime.ZONE));
  }
  private long count(String entity,String condition,Set<Long> branches,LocalDate today){
    if(branches!=null&&branches.isEmpty())return 0;
    var q=em.createQuery("select count(r) from "+entity+" r where "+condition+(branches==null?"":" and r.branchId in :branches"),Long.class);
    if(branches!=null)q.setParameter("branches",branches);if(today!=null)q.setParameter("today",today);return q.getSingleResult();
  }
}
