package com.example.auth.repository;

import com.example.auth.entity.Account;
import com.example.auth.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByAccountId(Long accountId);
    Optional<Admin> findByAccount(Account account);
}
