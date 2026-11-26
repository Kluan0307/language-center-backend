package com.example.auth.company;
import jakarta.validation.Valid;import java.security.Principal;import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/auth/settings")
public class SettingsController {
 private final SettingsService service;
 @GetMapping("/preferences") public Object get(Principal p){return service.preferences(p.getName());}
 @PutMapping("/preferences") public Object save(Principal p,@Valid @RequestBody SettingsService.PreferencesRequest r){return service.save(p.getName(),r);}
 @GetMapping("/company") public Object profile(Principal p){return service.profile(p.getName());}
 @PutMapping("/company") public Object profile(Principal p,@Valid @RequestBody SettingsService.ProfileRequest r){return service.profile(p.getName(),r);}
}
