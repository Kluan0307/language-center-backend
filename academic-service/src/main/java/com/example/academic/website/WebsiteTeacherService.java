package com.example.academic.website;

import com.example.academic.common.*;
import com.example.academic.security.Users;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.sql.Date;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class WebsiteTeacherService {
  private final WebsiteTeacherRepository repository;
  private final JdbcTemplate jdbc;
  private final jakarta.persistence.EntityManager em;
  public List<WebsiteCatalogService.SourceOption> sources(int page){if(page<0||page>10000)throw ApiException.bad("Invalid page");return jdbc.query("select id,full_name from teachers order by id limit 100 offset ?",(r,n)->new WebsiteCatalogService.SourceOption(r.getLong(1),r.getString(2)),page*100);}
  public List<WebsiteEditEvent> history(Long id){return em.createQuery("select e from WebsiteEditEvent e where e.kind='TEACHER' and e.sourceId=:id order by e.id desc",WebsiteEditEvent.class).setParameter("id",id).setMaxResults(100).getResultList();}
  public record Edit(@NotNull @Positive Long teacherId,@NotNull @PositiveOrZero Long version,
      @NotBlank @Size(max=120) @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
      @NotNull WebsitePublication.State state,@Size(max=2000) String biography,
      @Size(max=500) String teachingFocus,@Size(max=1000) String imageUrl,boolean showQualification) {}
  public record Profile(String slug,String name,String specialization,String biography,
      String teachingFocus,String qualification,String imageUrl) {}
  public record Page(List<Profile> items,int page,int size,boolean hasNext) {}
  private List<Profile> query(String slug,String branch,int offset,int size){
    var today=Date.valueOf(LocalDate.now(BusinessTime.ZONE));
    var params=new ArrayList<Object>(List.of(today,today));
    String sql="""
      select p.slug,t.full_name,t.specialization,p.biography,p.teaching_focus,
        case when p.show_qualification=true then t.qualification else null end as qualification,p.image_url
      from website_teacher_profiles p join teachers t on t.id=p.teacher_id
      join accounts a on a.id=t.account_id
      where p.state='PUBLISHED' and a.status=true and a.role='TEACHER' and t.work_status='ACTIVE'
        and (t.hire_date is null or t.hire_date<=?) and (t.end_date is null or t.end_date>=?)
        and exists (select ab.id from account_branches ab
          join branches b on b.id=ab.branch_id
          join website_publications bp on bp.source_id=b.id and bp.kind='BRANCH' and bp.state='PUBLISHED'
          where ab.account_id=a.id and ab.status=true and b.status='ACTIVE'
      """;
    if(branch!=null){sql+=" and bp.slug=?";params.add(branch);}
    sql+=")";if(slug!=null){sql+=" and p.slug=?";params.add(slug);}
    sql+=" order by p.id limit ? offset ?";params.add(size);params.add(offset);
    return jdbc.query(sql,(r,n)->new Profile(r.getString("slug"),r.getString("full_name"),r.getString("specialization"),r.getString("biography"),r.getString("teaching_focus"),r.getString("qualification"),r.getString("image_url")),params.toArray());
  }
  public Page list(int page,int size,String branch){
    if(page<0||page>10000||size<1||size>100)throw ApiException.bad("Phân trang không hợp lệ");
    var rows=query(null,branch,page*size,size+1);return new Page(rows.stream().limit(size).toList(),page,size,rows.size()>size);
  }
  public Profile detail(String slug){return query(slug,null,0,1).stream().findFirst().orElseThrow(ApiException::missing);}
  public List<WebsiteTeacherProfile> edits(int page){if(page<0||page>10000)throw ApiException.bad("Phân trang không hợp lệ");return repository.findAll(PageRequest.of(page,50)).getContent();}
  @Transactional
  public WebsiteTeacherProfile edit(Edit r){
    if(jdbc.queryForList("select t.id from teachers t join accounts a on a.id=t.account_id where t.id=? and a.role='TEACHER'",Long.class,r.teacherId()).isEmpty())throw ApiException.missing();
    var p=repository.findByTeacherId(r.teacherId()).orElseGet(WebsiteTeacherProfile::new);
    if(p.getVersion()!=r.version())throw ApiException.conflict("Hồ sơ đã thay đổi. Hãy tải lại.");
    if(repository.existsBySlugAndTeacherIdNot(r.slug(),r.teacherId()))throw ApiException.conflict("Slug đã tồn tại");
    if(p.getState()==WebsitePublication.State.PUBLISHED&&p.getSlug()!=null&&!p.getSlug().equals(r.slug()))throw ApiException.conflict("Ẩn hồ sơ trước khi đổi slug");
    if(r.imageUrl()!=null&&!r.imageUrl().isBlank())try{var u=URI.create(r.imageUrl());if(!"https".equals(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null)throw new IllegalArgumentException();}catch(IllegalArgumentException e){throw ApiException.bad("Ảnh phải là URL HTTPS hợp lệ");}
    if(r.state()==WebsitePublication.State.PUBLISHED&&(r.biography()==null||r.biography().isBlank()))throw ApiException.bad("Cần nội dung giới thiệu đã duyệt");
    p.setTeacherId(r.teacherId());p.setSlug(r.slug());p.setState(r.state());p.setBiography(r.biography());p.setTeachingFocus(r.teachingFocus());p.setImageUrl(r.imageUrl());p.setShowQualification(r.showQualification());p.setUpdatedAt(LocalDateTime.now(BusinessTime.ZONE));p.setUpdatedBy(Users.current().accountId());repository.saveAndFlush(p);
    if(r.state()==WebsitePublication.State.PUBLISHED&&query(r.slug(),null,0,1).isEmpty())throw ApiException.bad("Giáo viên cần đang làm việc tại chi nhánh đã công khai");
    var event=new WebsiteEditEvent();event.setKind("TEACHER");event.setSourceId(r.teacherId());event.setActorId(Users.current().accountId());event.setActorName(Users.current().username());event.setOccurredAt(p.getUpdatedAt());event.setState(p.getState().name());event.setSnapshot(r.toString());em.persist(event);
    return p;
  }
}
