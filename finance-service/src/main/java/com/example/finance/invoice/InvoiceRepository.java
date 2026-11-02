package com.example.finance.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InvoiceRepository
    extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {}
