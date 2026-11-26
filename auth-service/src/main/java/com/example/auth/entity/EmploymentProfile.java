package com.example.auth.entity;

import java.time.LocalDate;

public interface EmploymentProfile {
  Long getId();
  Account getAccount();
  long getVersion();
  String getWorkStatus();
  LocalDate getHireDate();
  LocalDate getEndDate();
  void setWorkStatus(String value);
  void setHireDate(LocalDate value);
  void setEndDate(LocalDate value);
}
