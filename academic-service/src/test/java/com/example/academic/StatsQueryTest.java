package com.example.academic;

import static org.junit.jupiter.api.Assertions.*;

import com.example.academic.stats.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class StatsQueryTest {
  @Autowired StatsRepository repository;

  @Test
  void groupedQueriesSupportEmptyAndScopedData() {
    for (Long branch : new Long[] {null, 1L}) {
      assertTrue(repository.studentsByStatus(branch).isEmpty());
      assertTrue(repository.studentsByLevel(branch).isEmpty());
      assertTrue(repository.classesByStatus(branch).isEmpty());
      assertTrue(repository.leadsByStatus(branch).isEmpty());
      assertEquals(0, repository.monthlyStudents(branch));
      assertArrayEquals(new long[] {0, 0}, repository.occupancy(branch));
    }
  }
}
