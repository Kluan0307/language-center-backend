package com.example.teaching.entity; // Hoặc package tương ứng trong service của bạn

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "facilities")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String type;

    private Integer quantity;

    @Column(name = "condition_value", length = 50)
    private String condition;

    private LocalDateTime purchaseDate;

    @Builder.Default
    @Column(length = 50)
    private String status = "ACTIVE";
}