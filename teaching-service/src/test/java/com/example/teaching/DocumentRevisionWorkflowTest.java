package com.example.teaching;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.teaching.common.*;
import com.example.teaching.document.*;
import com.example.teaching.integration.*;
import com.example.teaching.security.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:document-revision-tests;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class DocumentRevisionWorkflowTest {
  static final Path ROOT =
      Path.of(System.getProperty("java.io.tmpdir"), "lc-revision-tests-" + UUID.randomUUID());

  @DynamicPropertySource
  static void settings(DynamicPropertyRegistry r) {
    r.add("teaching.storage.path", () -> ROOT.toString());
  }

  @Autowired DocumentService service;
  @Autowired DocumentRepository documents;
  @Autowired DocumentEventRepository events;
  @Autowired DocumentStorage storage;
  @MockitoBean AcademicDirectory academic;
  DocumentDto document;

  void actor(Role role, long id) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(id, 2L, 3L, role, "actor-" + id), null, List.of()));
  }

  MockMultipartFile file(String content) {
    return new MockMultipartFile(
        "file",
        "lesson.txt",
        "text/plain",
        content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  @BeforeEach
  void setup() {
    when(academic.schoolClass(9L))
        .thenReturn(new AcademicDirectory.SchoolClass(9L, 3L, 2L, 7L, "Class"));
    actor(Role.TEACHER, 1);
    document = service.upload("Lesson", "First version", 9L, null, null, file("first"));
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  DocumentDto reject() {
    actor(Role.ADMIN, 9);
    return service.review(
        document.id(),
        new DocumentDto.Review(
            Document.Status.REJECTED,
            "Fix the explanation",
            service.require(document.id()).getVersion()));
  }

  @Test
  void ownerResubmissionKeepsOldFileAndReviewerHistory() throws Exception {
    var old = reject();
    actor(Role.TEACHER, 1);
    var next =
        service.resubmit(
            document.id(),
            "Updated lesson",
            "Fixed",
            "Corrected content",
            old.version(),
            file("second"));
    assertEquals(2, next.revision());
    assertEquals(Document.Status.PENDING, next.status());
    assertNull(next.rejectReason());
    assertEquals(1L, next.uploadedBy());
    assertNull(next.reviewedBy());
    assertEquals(
        "first",
        new String(
            storage
                .load(service.revision(document.id(), 1).getStorageKey())
                .getInputStream()
                .readAllBytes()));
    assertEquals(
        "second",
        new String(
            storage
                .load(service.revision(document.id(), 2).getStorageKey())
                .getInputStream()
                .readAllBytes()));
    assertTrue(
        service.history(document.id()).stream()
            .anyMatch(
                e ->
                    e.action().equals("REVIEWED")
                        && e.actorName().equals("actor-9")
                        && e.reason().equals("Fix the explanation")));
  }

  @Test
  void staleReviewerCannotApproveReplacementWithoutSeeingIt() {
    var old = reject();
    actor(Role.TEACHER, 1);
    var next =
        service.resubmit(document.id(), "New title", null, "Updated metadata", old.version(), null);
    actor(Role.ADMIN, 9);
    assertThrows(
        ApiException.class,
        () ->
            service.review(
                document.id(),
                new DocumentDto.Review(Document.Status.APPROVED, null, document.version())));
    assertEquals(Document.Status.PENDING, service.require(document.id()).getStatus());
    assertEquals(
        Document.Status.APPROVED,
        service
            .review(
                document.id(),
                new DocumentDto.Review(Document.Status.APPROVED, null, next.version()))
            .status());
  }

  @Test
  void onlyOriginalUploaderCanResubmit() {
    var d = reject();
    actor(Role.ADMIN, 9);
    assertThrows(
        ApiException.class,
        () ->
            service.resubmit(d.id(), "Title", null, "Manager replacing author", d.version(), null));
    actor(Role.TEACHER, 2);
    assertThrows(
        ApiException.class,
        () -> service.resubmit(d.id(), "Title", null, "Other author", d.version(), null));
  }

  @Test
  void pendingAndApprovedDocumentsCannotResubmit() {
    actor(Role.TEACHER, 1);
    assertThrows(
        ApiException.class,
        () ->
            service.resubmit(
                document.id(), "Title", null, "Attempt change", document.version(), null));
    actor(Role.ADMIN, 9);
    service.review(
        document.id(), new DocumentDto.Review(Document.Status.APPROVED, null, document.version()));
    actor(Role.TEACHER, 1);
    assertThrows(
        ApiException.class,
        () ->
            service.resubmit(
                document.id(),
                "Title",
                null,
                "Attempt approved edit",
                service.require(document.id()).getVersion(),
                null));
  }

  @Test
  void staleResubmitDoesNotCreateOrphanFile() throws Exception {
    var rejected = reject();
    actor(Role.TEACHER, 1);
    long before;
    try (var files = Files.list(ROOT)) {
      before = files.count();
    }
    assertThrows(
        ApiException.class,
        () ->
            service.resubmit(
                document.id(),
                "Title",
                null,
                "Stale replacement",
                document.version(),
                file("unused")));
    try (var files = Files.list(ROOT)) {
      assertEquals(before, files.count());
    }
    assertEquals(rejected.version(), service.require(document.id()).getVersion());
  }

  @Test
  void metadataOnlyRevisionReusesFileAndKeepsTarget() {
    var rejected = reject();
    actor(Role.TEACHER, 1);
    var old = service.require(document.id()).getStorageKey();
    var next =
        service.resubmit(
            document.id(),
            "Better explanation",
            "New summary",
            "Metadata fix only",
            rejected.version(),
            null);
    assertEquals(old, service.require(document.id()).getStorageKey());
    assertEquals(9L, next.classId());
    assertEquals(2, next.revision());
  }

  @Test
  void staffSeesApprovedCurrentFileButNeverRevisionHistory() {
    actor(Role.ADMIN, 9);
    service.review(
        document.id(), new DocumentDto.Review(Document.Status.APPROVED, null, document.version()));
    actor(Role.STAFF, 8);
    assertNotNull(service.require(document.id()));
    assertThrows(ApiException.class, () -> service.history(document.id()));
    assertThrows(ApiException.class, () -> service.revision(document.id(), 1));
  }

  @Test
  void legacySnapshotPreservesActualKnownReviewer() {
    var rejected = reject();
    events.deleteAll(events.findByDocumentIdOrderByIdDesc(document.id()));
    events.flush();
    actor(Role.TEACHER, 1);
    service.resubmit(
        document.id(), "Title", null, "Legacy content fixed", rejected.version(), null);
    var baseline =
        events.findByDocumentIdOrderByIdDesc(document.id()).stream()
            .filter(e -> e.getAction().equals("LEGACY_SNAPSHOT"))
            .findFirst()
            .orElseThrow();
    assertEquals("actor-9", baseline.getActorName());
    assertEquals("REJECTED", baseline.getStatus());
    assertEquals("Fix the explanation", baseline.getReason());
  }
}
