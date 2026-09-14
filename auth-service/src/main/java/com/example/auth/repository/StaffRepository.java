package com.example.auth.repository;

import com.example.auth.entity.Account;
import com.example.auth.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {
    Optional<Staff> findByAccountId(Long accountId);
    Optional<Staff> findByAccount(Account account);
}
