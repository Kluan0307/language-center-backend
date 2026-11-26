package com.example.auth.service;

import com.example.auth.common.*;
import com.example.auth.entity.*;
import com.example.auth.repository.*;
import com.example.auth.integration.BranchDirectory;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional
public class EmploymentService {
  private final AccountRepository accounts;
  private final EntityManager em;
  private final BranchDirectory branches;
  public record Update(@NotNull Long version, @NotBlank String workStatus,
      LocalDate hireDate, LocalDate endDate, @NotBlank @Size(min=3,max=255) String reason) {}
  public record View(Long accountId, String personnelCode, long version, String workStatus, LocalDate hireDate, LocalDate endDate) {}

  private EmploymentProfile profile(Long id, String username, boolean write) {
    var caller=accounts.findByUsername(username).orElseThrow(ApiException::missing);
    if (!Boolean.TRUE.equals(caller.getStatus()) || caller.getRole()!=Role.SUPER_ADMIN && caller.getRole()!=Role.ADMIN)
      throw ApiException.forbidden();
    var account=write?accounts.lock(id).orElseThrow(ApiException::missing):accounts.findById(id).orElseThrow(ApiException::missing);
    if (account.getRole()!=Role.STAFF && account.getRole()!=Role.ADMIN) throw ApiException.bad("Dùng trang hồ sơ giáo viên cho giáo viên");
    if (caller.getRole()!=Role.SUPER_ADMIN && (account.getRole()==Role.ADMIN || account.activeBranchIds().isEmpty() || !caller.activeBranchIds().containsAll(account.activeBranchIds())))
      throw ApiException.forbidden();
    String type=account.getRole()==Role.ADMIN?"Admin":"Staff";
    var p=(EmploymentProfile)em.createQuery("select p from "+type+" p where p.account.id=:id", Object.class).setParameter("id",id).getResultStream().findFirst().orElseThrow(ApiException::missing);
    if(write)em.refresh(p,LockModeType.PESSIMISTIC_WRITE);
    return p;
  }
  private View view(EmploymentProfile p){return new View(p.getAccount().getId(),p.getAccount().getPersonnelCode(),p.getVersion(),p.getWorkStatus(),p.getHireDate(),p.getEndDate());}
  @Transactional(readOnly=true) public View get(Long id,String username){return view(profile(id,username,false));}
  @Transactional(readOnly=true) public List<EmploymentEvent> history(Long id,String username){
    profile(id,username,false);
    return em.createQuery("select e from EmploymentEvent e where e.accountId=:id order by e.id desc",EmploymentEvent.class).setParameter("id",id).getResultList();
  }
  public View update(Long id,Update r,String username){
    var p=profile(id,username,true);
    if(r.version()==null||r.version()!=p.getVersion())throw ApiException.conflict("Hồ sơ đã thay đổi; hãy tải lại");
    EmploymentRules.validate(r.workStatus(),r.hireDate(),r.endDate());
    if(r.reason()==null||r.reason().trim().length()<3||r.reason().trim().length()>255)throw ApiException.bad("Nhập lý do từ 3 đến 255 ký tự");
    var a=p.getAccount();
    if("INACTIVE".equals(r.workStatus())&& !"INACTIVE".equals(p.getWorkStatus()))branches.requireNoActiveLeads(a.getId(),a.activeBranchIds());
    if(Objects.equals(p.getWorkStatus(),r.workStatus())&&Objects.equals(p.getHireDate(),r.hireDate())&&Objects.equals(p.getEndDate(),r.endDate()))return view(p);
    String before=view(p).toString();
    p.setWorkStatus(r.workStatus());p.setHireDate(r.hireDate());p.setEndDate(r.endDate());
    a.setSessionVersion(Math.addExact(a.getSessionVersion(),1));
    a.recordSecurityEvent("EMPLOYMENT_UPDATED",accounts.findByUsername(username).orElseThrow().getId(),r.reason().trim());
    var event=new EmploymentEvent();event.setAccountId(id);event.setActorId(accounts.findByUsername(username).orElseThrow().getId());event.setBeforeState(before);event.setAfterState(view(p).toString());event.setReason(r.reason().trim());event.setOccurredAt(java.time.LocalDateTime.now(BusinessTime.ZONE));em.persist(event);
    em.flush();return view(p);
  }
}
