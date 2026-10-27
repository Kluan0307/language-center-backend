package com.example.academic.student;

import com.example.academic.branch.BranchService;
import com.example.academic.common.*;
import com.example.academic.enrollment.EnrollmentRepository;
import com.example.academic.lead.LeadRepository;
import com.example.academic.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentService {
  private final StudentRepository repository;
  private final BranchScope scope;
  private final BranchService branches;
  private final EnrollmentRepository enrollments;
  private final LeadRepository leads;

  public Student require(Long id) {
    Student s = repository.findById(id).orElseThrow(ApiException::missing);
    scope.check(s.getBranchId());
    return s;
  }

  public Student lock(Long id) {
    Student s = repository.lock(id).orElseThrow(ApiException::missing);
    scope.check(s.getBranchId());
    return s;
  }

  @Transactional(readOnly = true)
  public PageResponse<StudentDto> list(
      int page, int size, String keyword, String level, Student.Status status, Long branchId) {
    return PageResponse.of(
        repository
            .findAll(
                Query.<Student>branch(scope.filter(branchId))
                    .and(Query.keyword(keyword, "fullName", "phone", "email"))
                    .and(Query.eq("currentLevel", level))
                    .and(Query.eq("status", status)),
                Query.page(page, size))
            .map(StudentDto::of));
  }

  @Transactional(readOnly = true)
  public StudentDto.Detail get(Long id) {
    Student s = require(id);
    return new StudentDto.Detail(
        StudentDto.of(s),
        enrollments.findByStudentId(id).stream()
            .map(
                e ->
                    new StudentDto.Registration(
                        e.getId(),
                        e.getSchoolClass().getId(),
                        e.getSchoolClass().getCode(),
                        e.getSchoolClass().getName(),
                        e.getEnrolledAt(),
                        e.getStudyStatus().name()))
            .toList());
  }

  public StudentDto save(Long id, StudentDto.Request r) {
    Student s = id == null ? new Student() : require(id);
    Long branch = id == null ? scope.create(r.branchId()) : s.getBranchId();
    branches.require(branch);
    duplicate(r.phone().trim(), branch, id);
    s.setBranchId(branch);
    s.setFullName(r.fullName().trim());
    s.setDateOfBirth(r.dateOfBirth());
    s.setGender(r.gender());
    s.setEmail(r.email());
    s.setPhone(r.phone().trim());
    s.setAddress(r.address());
    s.setContactPerson(r.contactPerson());
    s.setCurrentLevel(r.currentLevel());
    return StudentDto.of(repository.save(s));
  }

  public void duplicate(String phone, Long branch, Long id) {
    if (repository.existsByPhoneAndBranchIdAndIdNot(phone, branch, id == null ? -1L : id))
      throw ApiException.conflict("Số điện thoại học viên đã tồn tại trong chi nhánh");
  }

  public Student convert(String fullName, String phone, String email, String level, Long branch) {
    duplicate(phone, branch, null);
    Student s = new Student();
    s.setFullName(fullName);
    s.setPhone(phone);
    s.setEmail(email);
    s.setCurrentLevel(level);
    s.setBranchId(branch);
    return repository.saveAndFlush(s);
  }

  public StudentDto status(Long id, Student.Status status) {
    Student s = require(id);
    s.setStatus(status);
    return StudentDto.of(s);
  }

  public void delete(Long id) {
    Student s = lock(id);
    if (enrollments.existsByStudentId(id))
      throw ApiException.conflict("Không thể xóa học viên đã có đăng ký");
    if (leads.existsByConvertedStudentId(id))
      throw ApiException.conflict("Học viên đang được Lead chuyển đổi tham chiếu");
    repository.delete(s);
    repository.flush();
  }
}
