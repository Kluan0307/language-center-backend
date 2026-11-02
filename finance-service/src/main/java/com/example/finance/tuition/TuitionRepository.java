package com.example.finance.tuition;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TuitionRepository
    extends JpaRepository<Tuition, Long>, JpaSpecificationExecutor<Tuition> {}
