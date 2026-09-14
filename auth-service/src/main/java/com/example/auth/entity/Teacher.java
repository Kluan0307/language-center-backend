package com.example.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "teachers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(length = 100)
    private String email;

    @Column(length = 15)
    private String phone;

    @Column(length = 100)
    private String specialization;

    @Column(length = 100)
    private String qualification;

    @Column(length = 50)
    private String experience;

    @Column(length = 255)
    private String certificate;

    private LocalDate hireDate;

    @Builder.Default
    @Column(length = 50)
    private String workStatus = "ACTIVE";
}
