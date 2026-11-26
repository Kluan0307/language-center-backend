package com.example.auth.entity;

import jakarta.persistence.*;

/** One shared number series for staff/admin/teachers; gaps are intentional. */
@Entity
@Table(name = "personnel_code_numbers")
public class PersonnelCodeNumber {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  public String code(Role role) {
    if (role != Role.TEACHER && role != Role.ADMIN && role != Role.STAFF)
      throw new IllegalArgumentException("Not a personnel role");
    return (role == Role.TEACHER ? "GV" : "NS") + String.format(java.util.Locale.ROOT, "%03d", id);
  }
}
