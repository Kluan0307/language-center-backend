package com.example.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String receiptNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tuition_id", nullable = false)
    private Tuition tuition;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    private LocalDate paidAt;

    @Column(length = 50)
    private String method;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_account_id")
    private Account recordedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(length = 500)
    private String note;
}