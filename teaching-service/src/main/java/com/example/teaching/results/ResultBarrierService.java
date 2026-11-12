package com.example.teaching.results;

import com.example.teaching.common.ApiException;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResultBarrierService {
  private final EntityManager em;
  private final BarrierBootstrap bootstrap;

  public ResultBarrier read(Long id) {
    return em.find(ResultBarrier.class, id);
  }

  public ResultBarrier lock(Long id, Long branch) {
    em.flush();
    var cached = read(id);
    if (cached == null) bootstrap.ensure(id, branch);
    else em.detach(cached);
    // Explicit locking query is a MySQL current read, even when REQUIRES_NEW
    // just inserted the row after this transaction's first consistent read.
    var b = em.createQuery("select b from ResultBarrier b where b.classId=:id",ResultBarrier.class)
        .setParameter("id",id).setLockMode(LockModeType.PESSIMISTIC_WRITE).getSingleResult();
    if (!b.getBranchId().equals(branch)) throw ApiException.missing();
    return b;
  }

  public ResultBarrier mutable(Long id, Long branch) {
    var b = lock(id, branch);
    if (b.getConfirmedCount() > 0)
      throw ApiException.conflict(
          "Lớp đã có kết quả chính thức. Mở lại kết quả toàn lớp với lý do trước khi sửa nguồn.");
    return b;
  }
}
