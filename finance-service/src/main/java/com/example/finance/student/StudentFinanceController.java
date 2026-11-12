package com.example.finance.student;
import com.example.finance.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/finance/student/me") @RequiredArgsConstructor @PreAuthorize("hasRole('STUDENT')")
public class StudentFinanceController {
 private final StudentFinanceService service;
 @GetMapping("/tuitions") public PageResponse<StudentFinanceService.TuitionView> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(required=false) String status,@RequestParam(required=false) Boolean overdue,@RequestParam(required=false) Long enrollmentId){return service.list(page,size,status,overdue,enrollmentId);}
 @GetMapping("/tuitions/summary") public StudentFinanceService.Summary summary(){return service.summary();}
 @GetMapping("/tuitions/{id}") public StudentFinanceService.TuitionDetail detail(@PathVariable Long id){return service.detail(id);}
 @GetMapping("/receipts") public PageResponse<StudentFinanceService.ReceiptView> receipts(@RequestParam(required=false) Long tuitionId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.receipts(tuitionId,page,size);}
 @GetMapping("/receipts/{id}") public StudentFinanceService.ReceiptDetail receipt(@PathVariable Long id){return service.receipt(id);}
}
