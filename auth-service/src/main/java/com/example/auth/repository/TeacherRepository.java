package com.example.auth.repository;

import com.example.auth.entity.Account;
import com.example.auth.entity.Teacher;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
  @org.springframework.data.jpa.repository.Query(
      "select t from Teacher t join fetch t.account a where a.status=true and t.workStatus='ACTIVE'"
          + " and (:branch is null or exists (select ab.id from AccountBranch ab where ab.account=a"
          + " and ab.status=true and ab.branchId=:branch))")
  java.util.List<Teacher> available(
      @org.springframework.data.repository.query.Param("branch") Long branch);

  Optional<Teacher> findByAccountId(Long accountId);

  Optional<Teacher> findByAccount(Account account);
}
