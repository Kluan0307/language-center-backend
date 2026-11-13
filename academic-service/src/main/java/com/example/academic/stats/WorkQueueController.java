package com.example.academic.stats;

import com.example.academic.common.BusinessTime;
import com.example.academic.security.BranchScope;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/v1/academic/stats/work-queue")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
public class WorkQueueController {
  private final BranchScope scope;
  private final EntityManager em;
  public record Counts(long supportActive,long leadsNew,LocalDate businessDate,OffsetDateTime observedAt) {}
  @GetMapping @Transactional(readOnly=true)
  public Counts get(@RequestParam(required=false) Long branchId,@RequestParam(defaultValue="false") boolean allAssigned){
    var branches=scope.selection(branchId,allAssigned);
    return new Counts(count("SupportRequest","r.status in ('OPEN','IN_PROGRESS')",branches),count("Lead","r.status='NEW'",branches),LocalDate.now(BusinessTime.ZONE),OffsetDateTime.now(BusinessTime.ZONE));
  }
  private long count(String entity,String condition,Set<Long> branches){
    if(branches!=null&&branches.isEmpty())return 0;
    var q=em.createQuery("select count(r) from "+entity+" r where "+condition+(branches==null?"":" and r.branchId in :branches"),Long.class);
    if(branches!=null)q.setParameter("branches",branches);return q.getSingleResult();
  }
}
