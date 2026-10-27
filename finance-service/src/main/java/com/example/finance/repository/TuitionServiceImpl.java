package com.example.finance.service;

import com.example.auth.entity.Tuition;
import com.example.finance.repository.TuitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TuitionServiceImpl implements TuitionService {

    private final TuitionRepository tuitionRepository;

    @Override
    public List<Tuition> getAllTuitions() {
        return tuitionRepository.findAll();
    }

    @Override
    public Optional<Tuition> getTuitionById(Long id) {
        return tuitionRepository.findById(id);
    }

    @Override
    public Tuition saveTuition(Tuition tuition) {
        return tuitionRepository.save(tuition);
    }

    @Override
    public void deleteTuition(Long id) {
        tuitionRepository.deleteById(id);
    }
}