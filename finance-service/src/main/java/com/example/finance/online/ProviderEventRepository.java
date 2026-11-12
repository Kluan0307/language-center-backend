package com.example.finance.online;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProviderEventRepository extends JpaRepository<ProviderEvent,Long>{
 Optional<ProviderEvent> findByEventKey(String key);
 @org.springframework.transaction.annotation.Transactional(readOnly=true) @org.springframework.data.jpa.repository.Query("select e.id from ProviderEvent e where e.status='RECEIVED' order by e.id") java.util.List<Long> receivedIds(org.springframework.data.domain.Pageable page);
}
