package com.example.academic.support;
import com.example.academic.common.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/academic/student/me/support-requests") @RequiredArgsConstructor @PreAuthorize("hasRole('STUDENT')")
public class StudentSupportController {
 private final SupportService service;
 @GetMapping public PageResponse<SupportService.Row> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(required=false) SupportRequest.Status status,@RequestParam(required=false) SupportRequest.Category category){return service.list(true,page,size,status,category,null);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public SupportService.StudentDetail create(@Valid @RequestBody SupportService.Create r){return service.create(r);}
 @GetMapping("/{id}") public SupportService.StudentDetail detail(@PathVariable Long id){return service.studentDetail(id);}
 @PostMapping("/{id}/messages") public Object reply(@PathVariable Long id,@Valid @RequestBody SupportService.Message r){return service.reply(id,r.requestId(),r.version(),r.message(),false,true);}
 @PostMapping("/{id}/close") public Object close(@PathVariable Long id,@Valid @RequestBody SupportService.Close r){return service.change(id,r.requestId(),r.version(),SupportRequest.Status.CLOSED,"Học viên đã đóng yêu cầu.",true);}
 @GetMapping("/operations/{requestId}") public Object recover(@PathVariable UUID requestId){return service.recover(requestId,true);}
}
