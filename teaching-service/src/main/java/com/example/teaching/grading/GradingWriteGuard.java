package com.example.teaching.grading;
import com.example.teaching.common.ApiException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class GradingWriteGuard {
 private final EntityManager em;
 public GradingRelease latest(Long id){return em.createQuery("select r from GradingRelease r where r.classId=:id order by r.revision desc",GradingRelease.class).setParameter("id",id).setMaxResults(1).getResultStream().findFirst().orElse(null);}
 public void check(Long id){var r=latest(id);if(r!=null&&!java.util.Set.of("DRAFT","REJECTED").contains(r.getState()))throw ApiException.conflict("Kết quả đã gửi duyệt hoặc công bố; quản lý phải trả lại/mở lại với lý do trước khi sửa");}
}
