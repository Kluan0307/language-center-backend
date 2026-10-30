package com.example.teaching.schedule;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.List;

@RestController
@RequestMapping("/api/v1/teaching/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping
    public List<ScheduleResponse> getAll(
            @RequestParam(required = false) Long schoolClassId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long teacherAccountId,
            @RequestParam(required = false) DayOfWeek dayOfWeek
    ) {
        return scheduleService.getAll(schoolClassId, roomId, teacherAccountId, dayOfWeek);
    }

    @GetMapping("/my")
    public List<ScheduleResponse> getMySchedules(@RequestParam(required = false) Long teacherAccountId) {
        return scheduleService.getMySchedules(teacherAccountId);
    }

    @GetMapping("/{id}")
    public ScheduleResponse getById(@PathVariable Long id) {
        return scheduleService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleResponse create(@Valid @RequestBody ScheduleRequest request) {
        return scheduleService.create(request);
    }

    @PutMapping("/{id}")
    public ScheduleResponse update(@PathVariable Long id, @Valid @RequestBody ScheduleRequest request) {
        return scheduleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        scheduleService.delete(id);
    }
}
