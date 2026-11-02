package com.example.teaching.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exams")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_class_id")
    private SchoolClass schoolClass;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 50)
    private String type; // QUIZ, MIDTERM,...

    @Column(length = 50)
    private String status; // PUBLISHED, DRAFT,...

    private LocalDateTime startAt;

    private LocalDateTime endAt;
}