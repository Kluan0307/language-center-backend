package com.example.academic.branch;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BranchSeed implements CommandLineRunner {
  private final BranchRepository repository;
  private final JdbcTemplate jdbc;

  @Override
  @Transactional
  public void run(String... args) {
    if (repository.count() == 0) {
      jdbc.update(
          "insert into branches(id,code,name,address,phone,status,created_at)"
              + " values(1,?,?,?,?,?,CURRENT_TIMESTAMP)",
          "CN01",
          "Chi nhánh 1",
          "",
          "0900000001",
          "ACTIVE");
      jdbc.update(
          "insert into branches(id,code,name,address,phone,status,created_at)"
              + " values(2,?,?,?,?,?,CURRENT_TIMESTAMP)",
          "CN02",
          "Chi nhánh 2",
          "",
          "0900000002",
          "ACTIVE");
    }
  }
}
