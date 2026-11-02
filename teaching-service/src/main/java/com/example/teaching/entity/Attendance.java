package com.example.teaching.entity; // Hoặc package tương ứng trong service của bạn

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "attendances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    private LocalDate sessionDate;

    @Column(length = 50)
    private String status;

    private Boolean hasPermission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_account_id")
    private Account recordedBy;
}