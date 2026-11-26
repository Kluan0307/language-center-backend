package com.example.academic.website;

import com.example.academic.common.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** The existing deployment shares the auth/academic database, as LeadAssigneeDirectory does. */
@Component @RequiredArgsConstructor
public class WebsiteLeadRouting {
  private final JdbcTemplate jdbc;
  public record Recipient(Long id,String name) {}
  public List<Recipient> eligible(Long branch){
    var today=Date.valueOf(LocalDate.now(BusinessTime.ZONE));
    return jdbc.query("""
      select a.id, coalesce(s.full_name,d.full_name,a.username) as full_name
      from accounts a join account_branches ab on ab.account_id=a.id
      left join staffs s on s.account_id=a.id and a.role='STAFF'
      left join admins d on d.account_id=a.id and a.role='ADMIN'
      where a.status=true and ab.status=true and ab.branch_id=?
        and a.role in ('STAFF','ADMIN')
        and ((a.role='STAFF' and s.work_status='ACTIVE'
          and (s.hire_date is null or s.hire_date<=?) and (s.end_date is null or s.end_date>=?))
          or (a.role='ADMIN' and d.work_status='ACTIVE'
          and (d.hire_date is null or d.hire_date<=?) and (d.end_date is null or d.end_date>=?)))
      order by case when a.role='STAFF' then 0 else 1 end,
        (select count(*) from leads l where l.assigned_to=a.id and l.branch_id=ab.branch_id
          and l.status in ('NEW','CONTACTED','CONSULTING')), a.id
      """,(rs,n)->new Recipient(rs.getLong("id"),rs.getString("full_name")),branch,today,today,today,today);
  }
  public Recipient choose(Long branch,Long previous){
    var candidates=eligible(branch);
    if(candidates.isEmpty())throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Chi nhánh tạm thời chưa tiếp nhận tư vấn trực tuyến. Vui lòng thử lại sau hoặc liên hệ trực tiếp.");
    // Lock account before the final eligibility check: offboarding uses the same account lock.
    var selected=candidates.stream().filter(c->c.id().equals(previous)).findFirst().orElse(candidates.getFirst());
    jdbc.queryForObject("select id from accounts where id=? for update",Long.class,selected.id());
    return eligible(branch).stream().filter(c->c.id().equals(selected.id())).findFirst()
      .orElseThrow(()->new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Người tiếp nhận vừa thay đổi. Vui lòng gửi lại yêu cầu."));
  }
}
