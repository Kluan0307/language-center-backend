package com.example.academic.stats;

import com.example.academic.common.Query;
import com.example.academic.lead.*;
import com.example.academic.schoolclass.*;
import com.example.academic.student.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StatsRepository {
  private final StudentRepository students;
  private final SchoolClassRepository classes;
  private final LeadRepository leads;

  public List<Student> students(Long branchId) {
    return students.findAll(Query.branch(branchId));
  }

  public List<SchoolClass> classes(Long branchId) {
    return classes.findAll(Query.branch(branchId));
  }

  public List<Lead> leads(Long branchId) {
    return leads.findAll(Query.branch(branchId));
  }
}
