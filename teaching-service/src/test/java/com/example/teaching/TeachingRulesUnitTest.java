package com.example.teaching;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.teaching.common.*;
import com.example.teaching.document.*;
import com.example.teaching.exam.*;
import com.example.teaching.grade.*;
import com.example.teaching.integration.*;
import com.example.teaching.security.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class TeachingRulesUnitTest {
  @TempDir java.nio.file.Path directory;

  void user(Role role) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, 2L, 3L, role, "user"), null, List.of()));
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void staffCannotDownloadPendingDocument() {
    user(Role.STAFF);
    var repository = mock(DocumentRepository.class);
    Document d = new Document();
    d.setBranchId(3L);
    d.setStatus(Document.Status.PENDING);
    when(repository.findById(1L)).thenReturn(Optional.of(d));
    var service =
        new DocumentService(
            repository,
            mock(DocumentStorage.class),
            mock(AcademicDirectory.class),
            new BranchScope(),
            mock(DocumentEventRepository.class),
            mock(jakarta.persistence.EntityManager.class));
    assertThrows(ApiException.class, () -> service.require(1L));
    d.setStatus(Document.Status.APPROVED);
    assertSame(d, service.require(1L));
  }

  @Test
  void teacherCannotReadAnotherTeachersUpload() {
    user(Role.TEACHER);
    var repository = mock(DocumentRepository.class);
    Document d = new Document();
    d.setBranchId(3L);
    d.setUploadedBy(999L);
    when(repository.findById(1L)).thenReturn(Optional.of(d));
    assertThrows(
        ApiException.class,
        () ->
            new DocumentService(
                    repository,
                    mock(DocumentStorage.class),
                    mock(AcademicDirectory.class),
                    new BranchScope(),
                    mock(DocumentEventRepository.class),
                    mock(jakarta.persistence.EntityManager.class))
                .require(1L));
  }

  @Test
  void administratorCannotCreateAnInitialGrade() {
    user(Role.ADMIN);
    var grades = mock(GradeRepository.class);
    var exams = mock(ExamService.class);
    Exam e = new Exam();
    e.setMaxScore(BigDecimal.TEN);
    when(exams.locked(5L)).thenReturn(e);
    var service =
        new GradeService(
            grades,
            mock(GradeChangeRepository.class),
            exams,
            mock(AcademicDirectory.class),
            new BranchScope(),
            mock(ExamRosterService.class));
    assertThrows(
        ApiException.class,
        () -> service.save(new GradeDto.Request(5L, 6L, BigDecimal.ONE, null, "correction")));
  }

  @Test
  void gradeCannotExceedExamMaximum() {
    user(Role.TEACHER);
    var exams = mock(ExamService.class);
    Exam e = new Exam();
    e.setMaxScore(BigDecimal.TEN);
    when(exams.locked(5L)).thenReturn(e);
    assertThrows(
        ApiException.class,
        () ->
            new GradeService(
                    mock(GradeRepository.class),
                    mock(GradeChangeRepository.class),
                    exams,
                    mock(AcademicDirectory.class),
                    new BranchScope(),
                    mock(ExamRosterService.class))
                .save(new GradeDto.Request(5L, 6L, new BigDecimal("11"), null, null)));
  }

  @Test
  void storageUsesPrivateGeneratedKeysAndRejectsTraversal() {
    var storage = new DocumentStorage(directory.toString());
    String key =
        storage.save(
            new MockMultipartFile(
                "file", "../../lesson.pdf", "application/pdf", new byte[] {1, 2, 3}));
    assertTrue(java.nio.file.Files.isRegularFile(directory.resolve(key)));
    assertEquals(36, key.length());
    assertThrows(ApiException.class, () -> storage.load("../outside"));
    assertThrows(
        ApiException.class,
        () ->
            storage.save(
                new MockMultipartFile("file", "script.html", "text/html", new byte[] {1})));
  }
}
