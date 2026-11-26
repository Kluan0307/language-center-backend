package com.example.teaching.grading;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/teaching/grading")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','TEACHER')")
public class GradingOutcomeController {
 private final GradingOutcomeService service;private final GradingConversionService conversions;
 @GetMapping("/classes/{id}/preview") public GradingOutcomeService.Preview preview(@PathVariable Long id){return service.preview(id);}
 @PostMapping("/classes/{id}/workflow/{action}") public GradingOutcomeService.Preview action(@PathVariable Long id,@PathVariable GradingOutcomeService.Action action,@Valid @RequestBody GradingOutcomeService.Command r){return service.action(id,action,r);}
 @GetMapping("/classes/{id}/workflow/history") public List<GradingOutcomeService.Workflow> history(@PathVariable Long id){return service.history(id);}
 @GetMapping("/conversions") public List<GradingConversionService.View> conversions(){return conversions.list();}
 @PostMapping("/conversions") @PreAuthorize("hasRole('SUPER_ADMIN')") public GradingConversionService.View conversion(@Valid @RequestBody GradingConversionService.Command r){return conversions.create(r);}
}
