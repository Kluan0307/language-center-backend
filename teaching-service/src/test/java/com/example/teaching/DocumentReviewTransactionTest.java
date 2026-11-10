package com.example.teaching;

import static org.junit.jupiter.api.Assertions.*;

import com.example.teaching.common.ApiException;
import com.example.teaching.document.*;
import com.example.teaching.security.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(
    properties = "spring.datasource.url=jdbc:h2:mem:review-tests;MODE=MySQL;DB_CLOSE_DELAY=-1")
class DocumentReviewTransactionTest {
  @Autowired DocumentRepository documents;
  @Autowired DocumentService service;

  @Test
  void twoReviewersCannotOverwriteTheFirstDecision() throws Exception {
    Document d = new Document();
    d.setBranchId(3L);
    d.setTitle("Lesson");
    d.setStorageKey(UUID.randomUUID().toString());
    d.setUploadedBy(99L);
    Long id = documents.saveAndFlush(d).getId();
    CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      List<Future<Boolean>> results = new ArrayList<>();
      for (var decision : List.of(Document.Status.APPROVED, Document.Status.REJECTED)) {
        results.add(
            pool.submit(
                () -> {
                  SecurityContextHolder.getContext()
                      .setAuthentication(
                          new UsernamePasswordAuthenticationToken(
                              new CurrentUser(1L, 2L, 3L, Role.ADMIN, "reviewer"),
                              null,
                              List.of()));
                  ready.countDown();
                  start.await();
                  try {
                    service.review(id, new DocumentDto.Review(decision, "Checked", 0L));
                    return true;
                  } catch (ApiException e) {
                    assertEquals(409, e.getStatus().value());
                    return false;
                  } finally {
                    SecurityContextHolder.clearContext();
                  }
                }));
      }
      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();
      int accepted = 0;
      for (var result : results) if (result.get(15, TimeUnit.SECONDS)) accepted++;
      assertEquals(1, accepted);
    }
    assertNotEquals(Document.Status.PENDING, documents.findById(id).orElseThrow().getStatus());
  }
}
