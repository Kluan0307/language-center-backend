package com.example.finance.controller;

import com.example.auth.entity.Tuition;
import com.example.finance.service.TuitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tuitions")
@RequiredArgsConstructor
public class TuitionController {

    private final TuitionService tuitionService;

    @GetMapping
    public ResponseEntity<List<Tuition>> getAll() {
        return ResponseEntity.ok(tuitionService.getAllTuitions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tuition> getById(@PathVariable Long id) {
        return tuitionService.getTuitionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Tuition> create(@RequestBody Tuition tuition) {
        return ResponseEntity.ok(tuitionService.saveTuition(tuition));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tuitionService.deleteTuition(id);
        return ResponseEntity.ok().build();
    }
}