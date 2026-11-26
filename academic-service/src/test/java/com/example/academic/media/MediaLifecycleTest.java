package com.example.academic.media;
import com.example.academic.common.ApiException;
import com.example.academic.security.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:media-lifecycle;MODE=MySQL;DB_CLOSE_DELAY=-1") @Transactional
class MediaLifecycleTest {
 @Autowired MediaAssetService service;@Autowired JdbcTemplate jdbc;
 @BeforeEach void actor(){as(1L,Role.ADMIN);}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 void as(Long id,Role role){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new CurrentUser(id,id,1L,role,"test",List.of(1L)),null,List.of()));}
 MediaAsset image(){var key="lc_"+UUID.randomUUID().toString().replace("-","");return service.record(new CloudinaryMediaController.Uploaded(key,key,"https://res.cloudinary.com/test/image/upload/"+key+".png","png",100,2,2,"branches",1L,1L));}
 @Test void persistsMetadata(){var a=image();assertNotNull(a.getId());assertEquals(1L,a.getUploadedBy());assertEquals(100,a.getBytes());}
 @Test void ownBranchCanAttach(){var a=image();assertDoesNotThrow(()->service.validateUse(a.getUrl(),"BRANCH",1L));}
 @Test void cannotAttachAcrossBranches(){var a=image();assertThrows(ApiException.class,()->service.validateUse(a.getUrl(),"BRANCH",2L));}
 @Test void cannotAttachWrongCategory(){var a=image();assertThrows(ApiException.class,()->service.validateUse(a.getUrl(),"PROGRAM",1L));}
 @Test void anotherAdminCannotDelete(){var a=image();as(2L,Role.ADMIN);assertThrows(ApiException.class,()->service.delete(a.getId(),id->fail("Provider must not be called")));}
 @Test void deletesUnusedImageIdempotently(){var a=image();var calls=new AtomicInteger();service.delete(a.getId(),id->calls.incrementAndGet());service.delete(a.getId(),id->calls.incrementAndGet());assertEquals(1,calls.get());assertThrows(ApiException.class,()->service.validateUse(a.getUrl(),"BRANCH",1L));}
 @Test void historyPreventsDestructiveDelete(){var a=image();jdbc.update("insert into website_edit_events(kind,source_id,actor_id,actor_name,occurred_at,state,snapshot) values('BRANCH',1,1,'test',CURRENT_TIMESTAMP,'DRAFT',?)",a.getUrl());assertThrows(ApiException.class,()->service.delete(a.getId(),id->fail("History image must be retained")));}
 @Test void externalLegacyUrlRemainsSupported(){assertDoesNotThrow(()->service.validateUse("https://example.com/legacy.png","BRANCH",1L));}
 @Test void cannotBypassMetadataUsingUrlVariant(){var a=image();assertThrows(ApiException.class,()->service.validateUse(a.getUrl()+"?v=1","BRANCH",1L));}
 @Test void librarySharesImagesWithinBranchWithoutDeleteRights(){var a=image();as(2L,Role.ADMIN);var items=service.library("branches",1L,0);assertEquals(1,items.size());assertFalse(items.get(0).canDelete());assertDoesNotThrow(()->service.validateUse(a.getUrl(),"BRANCH",1L));}
 @Test void missingCourseReturns404InsteadOfDatabaseError(){String key="lc_"+UUID.randomUUID().toString().replace("-","");var a=service.record(new CloudinaryMediaController.Uploaded(key,key,"https://res.cloudinary.com/test/image/upload/"+key+".png","png",100,2,2,"courses",1L,1L));var error=assertThrows(ApiException.class,()->service.validateUse(a.getUrl(),"COURSE",Long.MAX_VALUE));assertEquals(org.springframework.http.HttpStatus.NOT_FOUND,error.getStatus());}
}
