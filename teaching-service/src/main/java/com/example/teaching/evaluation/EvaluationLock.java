package com.example.teaching.evaluation;

import com.example.teaching.common.ApiException;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EvaluationLock {
  private final EntityManager em;
  private final com.example.teaching.results.ResultBarrierService barrier;
  private final EvaluationStateRepository states;

  public EvaluationState lock(Long classId, Long branchId) {
    barrier.mutable(classId, branchId);
    var state = lockForResult(classId, branchId);
    state.setSourceRevision(state.getSourceRevision() + 1);
    return state;
  }

  public EvaluationState lockForResult(Long classId, Long branchId) {
    em.flush();
    em.createNativeQuery(
            "INSERT IGNORE INTO evaluation_states(class_id,branch_id,revision,source_revision)"
                + " VALUES (:classId,:branchId,0,0)")
        .setParameter("classId", classId)
        .setParameter("branchId", branchId)
        .executeUpdate();
    var s = states.lock(classId).orElseThrow(ApiException::missing);
    em.refresh(s, LockModeType.PESSIMISTIC_WRITE);
    if (!s.getBranchId().equals(branchId)) throw ApiException.missing();
    return s;
  }

  public void refresh(Object entity) {
    em.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
  }
}
