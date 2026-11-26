package com.example.auth.controller;
import com.example.auth.service.EmploymentService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/auth/accounts/{id}/employment")
public class EmploymentController {
 private final EmploymentService service;
 @GetMapping public EmploymentService.View get(@PathVariable Long id,Principal actor){return service.get(id,actor.getName());}
 @GetMapping("/history") public java.util.List<com.example.auth.entity.EmploymentEvent> history(@PathVariable Long id,Principal actor){return service.history(id,actor.getName());}
 @PatchMapping public EmploymentService.View update(@PathVariable Long id,@Valid @RequestBody EmploymentService.Update r,Principal actor){return service.update(id,r,actor.getName());}
}
