package com.example.auth.student;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/auth") @RequiredArgsConstructor
public class StudentAccountController {
 private final StudentAccountService service;
 @GetMapping("/student-accounts") public StudentAccountService.View get(@RequestParam Long studentId,Authentication a){return service.get(studentId,a.getName());}
 @PostMapping("/student-accounts") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) public StudentAccountService.View create(@Valid @RequestBody StudentAccountService.Create r,Authentication a){return service.create(r,a.getName());}
 @GetMapping("/student-account-provisionings/{requestId}") public StudentAccountService.View request(@PathVariable java.util.UUID requestId,Authentication a){return service.request(requestId.toString(),a.getName());}
 @PatchMapping("/student-accounts/{id}/status") public StudentAccountService.View status(@PathVariable Long id,@Valid @RequestBody StudentAccountService.Status r,Authentication a){return service.status(id,r,a.getName());}
 @PostMapping("/student-accounts/{id}/reset-password") public StudentAccountService.View reset(@PathVariable Long id,@Valid @RequestBody StudentAccountService.Reset r,Authentication a){return service.reset(id,r,a.getName());}
}
