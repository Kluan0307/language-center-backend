package com.example.academic.notification;

import com.example.academic.common.*;
import com.example.academic.security.Users;
import com.example.academic.student.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional
public class NotificationService {
 private final StudentAccess access; private final EntityManager em;
 @Value("${notifications.push.enabled:false}") private boolean pushAvailable;
 public record View(Long id,String category,String type,String title,String body,String resourceType,String resourceId,OffsetDateTime createdAt,OffsetDateTime readAt) {}
 public record Preferences(boolean pushEnabled,boolean schedule,boolean grades,boolean tuition,boolean payment,boolean documents,boolean support,long version,boolean pushAvailable) {}
 public record Change(boolean pushEnabled,boolean schedule,boolean grades,boolean tuition,boolean payment,boolean documents,boolean support,@Min(0) long version) {}
 private View view(StudentNotification n){return new View(n.getId(),n.getCategory(),n.getType(),n.getTitle(),n.getBody(),n.getResourceType(),n.getResourceId(),n.getCreatedAt().atZone(BusinessTime.ZONE).toOffsetDateTime(),n.getReadAt()==null?null:n.getReadAt().atZone(BusinessTime.ZONE).toOffsetDateTime());}
 private StudentNotification owned(Long id){var s=access.owner();return em.createQuery("select n from StudentNotification n where n.id=:id and n.accountId=:account and n.studentId=:student",StudentNotification.class).setParameter("id",id).setParameter("account",Users.current().accountId()).setParameter("student",s.getId()).getResultStream().findFirst().orElseThrow(ApiException::missing);}
 @Transactional(readOnly=true) public View detail(Long id){return view(owned(id));}
 @Transactional(readOnly=true) public PageResponse<View> list(int page,int size,Boolean unread,String category){var s=access.owner();if(page<0||size<1||size>100||(long)page*size>Integer.MAX_VALUE)throw ApiException.bad("Invalid page");if(category!=null&&!Set.of("SCHEDULE","GRADES","TUITION","PAYMENT","DOCUMENTS","SUPPORT").contains(category))throw ApiException.bad("Invalid category");String where="n.accountId=:account and n.studentId=:student"+(unread==null?"":unread?" and n.readAt is null":" and n.readAt is not null");if(category!=null)where+=" and n.category=:category";var q=em.createQuery("select n from StudentNotification n where "+where+" order by n.createdAt desc,n.id desc",StudentNotification.class);var c=em.createQuery("select count(n) from StudentNotification n where "+where,Long.class);for(var query:List.of(q,c)){query.setParameter("account",Users.current().accountId());query.setParameter("student",s.getId());if(category!=null)query.setParameter("category",category);}long total=c.getSingleResult();return new PageResponse<>(q.setFirstResult(page*size).setMaxResults(size).getResultList().stream().map(this::view).toList(),page,size,total,(int)Math.ceil((double)total/size));}
 @Transactional(readOnly=true) public long unread(){var s=access.owner();return em.createQuery("select count(n) from StudentNotification n where n.accountId=:account and n.studentId=:student and n.readAt is null",Long.class).setParameter("account",Users.current().accountId()).setParameter("student",s.getId()).getSingleResult();}
 public void read(Long id){var n=owned(id);em.createQuery("update StudentNotification n set n.readAt=:at where n.id=:id and n.accountId=:account and n.readAt is null").setParameter("at",LocalDateTime.now(BusinessTime.ZONE)).setParameter("id",id).setParameter("account",n.getAccountId()).executeUpdate();}
 private Preferences view(NotificationPreference p){return new Preferences(p.isPushEnabled(),p.isSchedule(),p.isGrades(),p.isTuition(),p.isPayment(),p.isDocuments(),p.isSupport(),p.getVersion(),pushAvailable);}
 @Transactional(readOnly=true) public Preferences preferences(){access.owner();var p=em.find(NotificationPreference.class,Users.current().accountId());return view(p==null?new NotificationPreference():p);}
 public Preferences change(Change c){var s=access.owner();em.find(Student.class,s.getId(),LockModeType.PESSIMISTIC_WRITE);var p=em.find(NotificationPreference.class,Users.current().accountId());if(p==null){p=new NotificationPreference();p.setAccountId(Users.current().accountId());if(c.version()!=0)throw ApiException.conflict("Preferences changed; reload");em.persist(p);}else{em.refresh(p);if(c.version()!=p.getVersion())throw ApiException.conflict("Preferences changed; reload");}p.setPushEnabled(c.pushEnabled());p.setSchedule(c.schedule());p.setGrades(c.grades());p.setTuition(c.tuition());p.setPayment(c.payment());p.setDocuments(c.documents());p.setSupport(c.support());em.flush();return view(p);}
}
