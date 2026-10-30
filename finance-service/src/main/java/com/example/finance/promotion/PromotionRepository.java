package com.example.finance.promotion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PromotionRepository
    extends JpaRepository<Promotion, Long>, JpaSpecificationExecutor<Promotion> {}
