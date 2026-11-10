package com.example.auth.repository;

import com.example.auth.entity.Account;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository
    extends JpaRepository<Account, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Account> {
  Optional<Account> findByUsername(String username);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select a from Account a where a.id=:id")
  Optional<Account> lock(@org.springframework.data.repository.query.Param("id") Long id);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query(
      "select a from Account a where a.username=:username")
  Optional<Account> lockByUsername(
      @org.springframework.data.repository.query.Param("username") String username);

  boolean existsByUsername(String username);

  List<Account> findByBranchId(Long branchId);
}
