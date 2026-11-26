package com.example.academic.lead;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/academic/leads/{id}") @RequiredArgsConstructor @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
public class LeadIntakeController {
 private final LeadIntakeService service;
 @GetMapping("/intake/requests/{requestId}") public org.springframework.http.ResponseEntity<LeadIntakeService.Result> recover(@PathVariable Long id,@PathVariable java.util.UUID requestId){return org.springframework.http.ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(service.recover(id,requestId));}
 @GetMapping("/preferences") public LeadIntakeService.Preferences preferences(@PathVariable Long id){return service.preferences(id);}
 @PutMapping("/preferences") public LeadDto preference(@PathVariable Long id,@Valid @RequestBody LeadIntakeService.Preference r){return service.preference(id,r);}
 @PostMapping("/intake/preview") public LeadIntakeService.Preview preview(@PathVariable Long id,@Valid @RequestBody LeadIntakeService.Selection r){return service.preview(id,r);}
 @PostMapping("/intake/confirm") public LeadIntakeService.Result confirm(@PathVariable Long id,@Valid @RequestBody LeadIntakeService.Confirm r){return service.confirm(id,r);}
}
