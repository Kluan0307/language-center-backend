package com.example.academic.results;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

@Component
@RequiredArgsConstructor
public class BarrierBootstrap {
  private final EntityManager em;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void ensure(Long id, Long branch) {
    em.createNativeQuery(
            "INSERT IGNORE INTO"
                + " class_result_barriers(class_id,branch_id,token,source_revision,result_revision,confirmed_count)"
                + " VALUES (:id,:branch,:token,0,0,0)")
        .setParameter("id", id)
        .setParameter("branch", branch)
        .setParameter("token", java.util.UUID.randomUUID().toString())
        .executeUpdate();
  }
}
