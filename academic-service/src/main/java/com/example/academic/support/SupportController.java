package com.example.academic.support;
import com.example.academic.common.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/academic/support-requests") @RequiredArgsConstructor @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
public class SupportController {
 private final SupportService service;
 @GetMapping public PageResponse<SupportService.Row> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(required=false) SupportRequest.Status status,@RequestParam(required=false) SupportRequest.Category category,@RequestParam(required=false) Long branchId,@RequestParam(required=false) Long studentId,@RequestParam(required=false) String keyword,@RequestParam(required=false) String queue,@RequestParam(defaultValue="false") boolean allAssigned){return service.search(false,page,size,status,category,branchId,studentId,keyword,queue,allAssigned);}
 @GetMapping("/{id}") public SupportService.StaffDetail detail(@PathVariable Long id){return service.staffDetail(id);}
 @PostMapping("/{id}/messages") public Object reply(@PathVariable Long id,@Valid @RequestBody SupportService.StaffMessage r){return service.reply(id,r.requestId(),r.version(),r.message(),r.internalNote(),false);}
 @PatchMapping("/{id}/status") public Object change(@PathVariable Long id,@Valid @RequestBody SupportService.Change r){return service.change(id,r.requestId(),r.version(),r.status(),r.reason(),false);}
 @GetMapping("/operations/{requestId}") public Object recover(@PathVariable UUID requestId){return service.recover(requestId,false);}
}
