package com.example.auth.repository;

import com.example.auth.entity.Account;
import com.example.auth.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByAccountId(Long accountId);
    Optional<Teacher> findByAccount(Account account);
}
