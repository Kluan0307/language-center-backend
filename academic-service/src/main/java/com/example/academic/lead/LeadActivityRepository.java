package com.example.academic.lead;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadActivityRepository extends JpaRepository<LeadActivity, Long> {
  List<LeadActivity> findByLeadIdOrderByIdDesc(Long id);
}
