package com.example.academic.notification;
import com.example.academic.common.PageResponse;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/academic/student/me") @PreAuthorize("hasRole('STUDENT')")
public class NotificationController {
 private final NotificationService service;
 @GetMapping("/notifications") public PageResponse<NotificationService.View> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(required=false) Boolean unread,@RequestParam(required=false) String category){return service.list(page,size,unread,category);}
 @GetMapping("/notifications/unread-count") public Map<String,Long> count(){return Map.of("count",service.unread());}
 @GetMapping("/notifications/{id}") public NotificationService.View detail(@PathVariable Long id){return service.detail(id);}
 @PutMapping("/notifications/{id}/read") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void read(@PathVariable Long id){service.read(id);}
 @GetMapping("/notification-preferences") public NotificationService.Preferences preferences(){return service.preferences();}
 @PutMapping("/notification-preferences") public NotificationService.Preferences change(@Valid @RequestBody NotificationService.Change c){return service.change(c);}
}
