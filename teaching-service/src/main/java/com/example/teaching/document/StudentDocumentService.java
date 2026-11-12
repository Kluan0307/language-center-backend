package com.example.teaching.document;
import com.example.teaching.common.*;
import com.example.teaching.security.*;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service @RequiredArgsConstructor @Transactional(isolation=Isolation.READ_COMMITTED)
public class StudentDocumentService {
 private final DocumentRepository documents; private final DocumentService management; private final RemoteApi remote; private final EntityManager em;
 @Value("${services.academic.url}") private String academicUrl;
 public record Scope(Long enrollmentId,Long classId,Long courseId,Long branchId) {}
 public record Sharing(@NotNull @PositiveOrZero Long version,boolean sharedToStudents,@NotBlank @Size(min=3,max=500) String reason) {}
 public record View(Long id,String title,String description,String filename,String mimeType,Long size,int revision,LocalDateTime sharedAt) {}
 public DocumentDto share(Long id,Sharing r){var u=Users.current();if(!Set.of(Role.SUPER_ADMIN,Role.ADMIN,Role.TEACHER).contains(u.role()))throw ApiException.forbidden();management.require(id);var d=documents.lock(id).orElseThrow(ApiException::missing);em.refresh(d);management.require(id);if(!Objects.equals(d.getVersion(),r.version()))throw ApiException.conflict("Document changed; reload");if(r.sharedToStudents()&&d.getStatus()!=Document.Status.APPROVED)throw ApiException.conflict("Only approved documents can be shared");if(d.isSharedToStudents()!=r.sharedToStudents()){d.setSharedToStudents(r.sharedToStudents());d.setSharedAt(r.sharedToStudents()?LocalDateTime.now(BusinessTime.ZONE):null);var e=new DocumentSharing();e.setDocumentId(id);e.setRevision(d.getRevision());e.setSharedToStudents(r.sharedToStudents());e.setActorId(u.accountId());e.setReason(r.reason().trim());e.setOccurredAt(LocalDateTime.now(BusinessTime.ZONE));em.persist(e);em.flush();com.example.teaching.notification.StudentEvents.add(em,"teaching:document:"+e.getId(),"DOCUMENTS",r.sharedToStudents()?"DOCUMENT_SHARED":"DOCUMENT_REVOKED","Tài liệu học tập có cập nhật","Mở danh sách tài liệu để xem nội dung hiện được chia sẻ.",null,null,d.getClassId(),d.getCourseId(),d.getBranchId(),"DOCUMENT",id.toString(),null,null);}return DocumentDto.of(d);}
 private List<Scope> scopes(Long enrollment){if(Users.current().role()!=Role.STUDENT)throw ApiException.forbidden();return Arrays.asList(remote.get(academicUrl+"/api/v1/academic/student/me/document-scopes"+(enrollment==null?"":"?enrollmentId="+enrollment),Scope[].class));}
 private String predicate(List<Scope> scopes){StringJoiner join=new StringJoiner(" or ","(",")");for(int i=0;i<scopes.size();i++)join.add("(d.branchId=:b"+i+" and (d.classId=:c"+i+" or (d.classId is null and d.courseId=:k"+i+")))");return "d.status='APPROVED' and d.sharedToStudents=true and "+join;}
 private <T> jakarta.persistence.TypedQuery<T> bind(jakarta.persistence.TypedQuery<T> q,List<Scope> scopes){for(int i=0;i<scopes.size();i++){var s=scopes.get(i);q.setParameter("b"+i,s.branchId());q.setParameter("c"+i,s.classId());q.setParameter("k"+i,s.courseId());}return q;}
 public static String mime(String name){String n=name==null?"":name.toLowerCase(Locale.ROOT);if(n.endsWith(".pdf"))return "application/pdf";if(n.endsWith(".txt"))return "text/plain";if(n.endsWith(".png"))return "image/png";if(n.endsWith(".jpg")||n.endsWith(".jpeg"))return "image/jpeg";return "application/octet-stream";}
 @Transactional(readOnly=true) public PageResponse<View> list(Long enrollment,int page,int size){if(page<0||size<1||size>100||(long)page*size>Integer.MAX_VALUE)throw ApiException.bad("Invalid page");var scopes=scopes(enrollment).stream().distinct().toList();if(scopes.isEmpty())return new PageResponse<>(List.of(),page,size,0,0);String where=predicate(scopes);var rows=bind(em.createQuery("select d from Document d where "+where+" order by d.sharedAt desc,d.id desc",Document.class),scopes).setFirstResult(page*size).setMaxResults(size).getResultList();long total=bind(em.createQuery("select count(d) from Document d where "+where,Long.class),scopes).getSingleResult();return new PageResponse<>(rows.stream().map(d->new View(d.getId(),d.getTitle(),d.getDescription(),d.getOriginalFilename(),mime(d.getOriginalFilename()),d.getFileSize(),d.getRevision(),d.getSharedAt())).toList(),page,size,total,(int)Math.ceil((double)total/size));}
 @Transactional(readOnly=true) public Document download(Long id){var scopes=scopes(null).stream().distinct().toList();if(scopes.isEmpty())throw ApiException.missing();return bind(em.createQuery("select d from Document d where d.id=:id and "+predicate(scopes),Document.class).setParameter("id",id),scopes).getResultStream().findFirst().orElseThrow(ApiException::missing);}
}
