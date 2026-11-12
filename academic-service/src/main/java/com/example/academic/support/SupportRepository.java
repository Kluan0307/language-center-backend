package com.example.academic.support;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface SupportRepository extends JpaRepository<SupportRequest,Long> {
 @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select r from SupportRequest r where r.id=:id") Optional<SupportRequest> lock(@Param("id") Long id);
}
