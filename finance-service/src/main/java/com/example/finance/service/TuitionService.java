package com.example.finance.service;

import com.example.auth.entity.Tuition;
import java.util.List;
import java.util.Optional;

public interface TuitionService {
    List<Tuition> getAllTuitions();
    Optional<Tuition> getTuitionById(Long id);
    Tuition saveTuition(Tuition tuition);
    void deleteTuition(Long id);
}