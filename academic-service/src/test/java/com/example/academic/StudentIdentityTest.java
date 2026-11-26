package com.example.academic;

import static org.junit.jupiter.api.Assertions.*;

import com.example.academic.student.*;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:student-identity;MODE=MySQL;DB_CLOSE_DELAY=-1",
      "academic.seed.branches.enabled=false"
    })
class StudentIdentityTest {
  @Autowired EntityManager em;
  @Autowired PlatformTransactionManager transactions;
  @Autowired StudentService students;

  @Test
  void normalCreateAndEditKeepTheSameStudentCode() {
    try {
      new TransactionTemplate(transactions).execute(s -> {
        var branch = new com.example.academic.branch.Branch();
        branch.setCode("IDENTITY"); branch.setName("Identity"); branch.setPhone("0900000000");
        em.persist(branch);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                new com.example.academic.security.CurrentUser(1L, 1L, branch.getId(), com.example.academic.security.Role.SUPER_ADMIN, "identity"), null, List.of()));
        var request = new StudentDto.Request("Student", null, null, null, "0900000000", null, null, null, branch.getId(), null, null);
        var first = students.save(null, request);
        assertTrue(first.studentCode().matches("HV[0-9]{4,}"));
        assertEquals(first.studentCode(), students.save(first.id(), request).studentCode());
        s.setRollbackOnly();return null;
      });
    } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
  }

  @Test
  void concurrentNumberAllocationIsUniqueAcrossTransactions() throws Exception {
    try (var executor = Executors.newFixedThreadPool(4)) {
      var jobs = new ArrayList<Future<String>>();
      for (int i = 0; i < 16; i++)
        jobs.add(
            executor.submit(
                () ->
                    new TransactionTemplate(transactions)
                        .execute(
                            s -> {
                              var number = new StudentCodeNumber();
                              em.persist(number);
                              return number.code();
                            })));
      var codes = new HashSet<String>();
      for (var job : jobs) {
        String code = job.get(10, TimeUnit.SECONDS);
        assertTrue(code.matches("HV[0-9]{4,}"));
        assertTrue(codes.add(code));
      }
    }
  }

  @Test
  void rollbackDoesNotRecycleAllocatedNumber() {
    var tx = new TransactionTemplate(transactions);
    String first =
        tx.execute(
            s -> {
              var n = new StudentCodeNumber();
              em.persist(n);
              s.setRollbackOnly();
              return n.code();
            });
    String second =
        tx.execute(
            s -> {
              var n = new StudentCodeNumber();
              em.persist(n);
              return n.code();
            });
    assertNotEquals(first, second);
  }

  @Test
  void defaultStartupDoesNotCreateBranches() {
    Long count = new TransactionTemplate(transactions)
            .execute(
                s ->
                    em.createQuery("select count(b) from Branch b", Long.class).getSingleResult());
    assertEquals(0L, count.longValue());
  }
}
