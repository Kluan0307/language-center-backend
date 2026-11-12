package com.example.teaching;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.example.teaching.common.*;
import com.example.teaching.document.*;
import com.example.teaching.security.*;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:student-documents;MODE=MySQL;DB_CLOSE_DELAY=-1") @Transactional
class StudentDocumentTest {
 @Autowired StudentDocumentService service; @Autowired EntityManager em;
 @MockitoBean RemoteApi remote; @MockitoBean DocumentService management;
 Document doc;
 void actor(Role role){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new CurrentUser(101L,11L,3L,role,"actor"),null,List.of()));}
 @BeforeEach void setup(){actor(Role.STUDENT);when(remote.get(anyString(),eq(StudentDocumentService.Scope[].class))).thenReturn(new StudentDocumentService.Scope[]{new StudentDocumentService.Scope(1L,9L,7L,3L)});doc=new Document();doc.setBranchId(3L);doc.setClassId(9L);doc.setTitle("PRIVATE MANAGEMENT metadata excluded");doc.setStorageKey(UUID.randomUUID().toString());doc.setOriginalFilename("lesson.pdf");doc.setUploadedBy(100L);em.persist(doc);em.flush();when(management.require(doc.getId())).thenReturn(doc);}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void pendingApprovedUnsharedAndRevokedAllDenyDownload(){assertEquals(0,service.list(null,0,20).totalElements());assertThrows(ApiException.class,()->service.download(doc.getId()));doc.setStatus(Document.Status.APPROVED);assertThrows(ApiException.class,()->service.download(doc.getId()));actor(Role.ADMIN);var d=service.share(doc.getId(),new StudentDocumentService.Sharing(doc.getVersion(),true,"Approved for students"));actor(Role.STUDENT);assertEquals(1,service.list(null,0,20).totalElements());assertEquals(doc.getId(),service.download(doc.getId()).getId());actor(Role.ADMIN);service.share(doc.getId(),new StudentDocumentService.Sharing(d.version(),false,"Withdrawn sharing"));actor(Role.STUDENT);assertThrows(ApiException.class,()->service.download(doc.getId()));}
 @Test void courseMustMatchBranchAndClassDocumentsMustMatchClass(){doc.setClassId(null);doc.setCourseId(7L);doc.setStatus(Document.Status.APPROVED);doc.setSharedToStudents(true);em.flush();assertEquals(1,service.list(null,0,20).totalElements());doc.setBranchId(4L);em.flush();assertEquals(0,service.list(null,0,20).totalElements());assertThrows(ApiException.class,()->service.download(doc.getId()));doc.setBranchId(3L);doc.setClassId(99L);em.flush();assertEquals(0,service.list(null,0,20).totalElements());}
 @Test void studentProjectionDoesNotContainStorageReviewOrUploader(){doc.setStatus(Document.Status.APPROVED);doc.setSharedToStudents(true);doc.setReviewedByName("SECRET REVIEWER");em.flush();var v=service.list(null,0,20).content().getFirst();var names=Arrays.stream(v.getClass().getRecordComponents()).map(java.lang.reflect.RecordComponent::getName).toList();assertFalse(names.contains("storageKey"));assertFalse(names.contains("reviewedByName"));assertFalse(names.contains("uploadedBy"));}
 @Test void noScopeAndDependencyErrorDoNotGrantAccess(){when(remote.get(anyString(),eq(StudentDocumentService.Scope[].class))).thenReturn(new StudentDocumentService.Scope[0]);assertEquals(0,service.list(null,0,20).totalElements());assertThrows(ApiException.class,()->service.download(doc.getId()));when(remote.get(anyString(),eq(StudentDocumentService.Scope[].class))).thenThrow(new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"offline"));assertThrows(ApiException.class,()->service.list(null,0,20));}
 @Test void shareNeedsApprovalCurrentVersionAndOperator(){assertThrows(ApiException.class,()->service.share(doc.getId(),new StudentDocumentService.Sharing(doc.getVersion(),true,"share")));actor(Role.STAFF);assertThrows(ApiException.class,()->service.share(doc.getId(),new StudentDocumentService.Sharing(doc.getVersion(),true,"share")));actor(Role.ADMIN);assertThrows(ApiException.class,()->service.share(doc.getId(),new StudentDocumentService.Sharing(doc.getVersion(),true,"share")));doc.setStatus(Document.Status.APPROVED);em.flush();assertThrows(ApiException.class,()->service.share(doc.getId(),new StudentDocumentService.Sharing(999L,true,"share")));}
}
