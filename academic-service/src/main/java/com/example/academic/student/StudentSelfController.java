package com.example.academic.student;
import com.example.academic.branch.BranchRepository;
import com.example.academic.common.ApiException;
import com.example.academic.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/academic/student/me") @RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentSelfController {
 private final StudentRepository students;
 private final BranchRepository branches;
 public record BranchView(Long id,String name,String phone,String address) {}
 public record Profile(Long studentId,String studentCode,String fullName,java.time.LocalDate dateOfBirth,Student.Gender gender,String email,String phone,String address,Student.Status status,BranchView branch,long version) {}
 @GetMapping("/profile") @Transactional(readOnly=true)
 public Profile profile(){
  var user=Users.current();if(user.role()!=Role.STUDENT)throw ApiException.forbidden();
  var s=students.findByAccountId(user.accountId()).orElseThrow(ApiException::forbidden);
  if(!s.getId().equals(user.profileId()))throw ApiException.forbidden();
  var b=branches.findById(s.getBranchId()).orElseThrow(ApiException::missing);
  return new Profile(s.getId(),s.getStudentCode(),s.getFullName(),s.getDateOfBirth(),s.getGender(),s.getEmail(),s.getPhone(),s.getAddress(),s.getStatus(),new BranchView(b.getId(),b.getName(),b.getPhone(),b.getAddress()),s.getVersion());
 }
}
