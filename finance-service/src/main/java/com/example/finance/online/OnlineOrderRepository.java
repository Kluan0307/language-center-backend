package com.example.finance.online;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface OnlineOrderRepository extends JpaRepository<OnlineOrder,String> {
 Optional<OnlineOrder> findByAccountIdAndRequestId(Long id,String request);
 Optional<OnlineOrder> findByMerchantRef(String ref);
 List<OnlineOrder> findByTuitionIdOrderByCreatedAtDesc(Long id);
 @org.springframework.transaction.annotation.Transactional(readOnly=true) @Query("select o.id from OnlineOrder o where o.state in ('CREATED','PENDING') and o.expiresAt<:now order by o.expiresAt") List<String> expiredIds(@Param("now") java.time.LocalDateTime now,org.springframework.data.domain.Pageable page);
 @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select o from OnlineOrder o where o.id=:id") Optional<OnlineOrder> lock(@Param("id") String id);
}
