package com.example.academic.student;

import jakarta.persistence.*;

/** Database-issued numbers are never calculated from the current row count. */
@Entity
@Table(name = "student_code_numbers")
public class StudentCodeNumber {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  public String code() { return "HV" + String.format(java.util.Locale.ROOT, "%04d", id); }
}
