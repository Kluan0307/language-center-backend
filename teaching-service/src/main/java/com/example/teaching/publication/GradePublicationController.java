package com.example.teaching.publication;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/teaching/exams/{id}") @RequiredArgsConstructor @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','TEACHER')")
public class GradePublicationController {
 private final GradePublicationService service;
 @GetMapping("/student-publication") public GradePublicationService.State state(@PathVariable Long id){return service.state(id);}
 @GetMapping("/student-publication/requests/{requestId}") public GradePublicationService.State recover(@PathVariable Long id,@PathVariable UUID requestId){return service.recover(id,requestId);}
 @PostMapping("/student-publications") public GradePublicationService.State publish(@PathVariable Long id,@Valid @RequestBody GradePublicationService.Command c){return service.apply(id,c,false);}
 @PostMapping("/student-publications/retract") public GradePublicationService.State retract(@PathVariable Long id,@Valid @RequestBody GradePublicationService.Command c){return service.apply(id,c,true);}
}
