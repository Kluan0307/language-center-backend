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
    if (read(id) == null) bootstrap.ensure(id, branch);
    var b = em.find(ResultBarrier.class, id, LockModeType.PESSIMISTIC_WRITE);
    em.refresh(b, LockModeType.PESSIMISTIC_WRITE);
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
