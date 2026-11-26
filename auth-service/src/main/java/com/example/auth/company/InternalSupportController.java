package com.example.auth.company;

import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/internal-support")
public class InternalSupportController {
  private final InternalSupportService service;

  @GetMapping("/guides")
  public Object guides(Principal p) {
    return service.guides(p.getName());
  }

  @GetMapping("/tickets")
  public Object list(
      Principal p,
      @RequestParam(defaultValue = "false") boolean queue,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.list(p.getName(), queue, status, page, size);
  }

  @PostMapping("/tickets")
  public Object create(@Valid @RequestBody InternalSupportService.Create r, Principal p) {
    return service.create(r, p.getName());
  }

  @GetMapping("/tickets/{id}")
  public Object detail(@PathVariable Long id, Principal p) {
    return service.detail(id, p.getName());
  }

  @PostMapping("/tickets/{id}/replies")
  public Object reply(
      @PathVariable Long id, @Valid @RequestBody InternalSupportService.Reply r, Principal p) {
    return service.reply(id, r, p.getName());
  }
}
