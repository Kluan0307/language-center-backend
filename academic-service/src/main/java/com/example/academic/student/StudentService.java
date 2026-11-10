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
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class StudentService {
  private final com.example.academic.placement.PlacementExceptionRepository placementExceptions;
  private final StudentRepository repository;
  private final com.example.academic.lifecycle.LearningHistory history;
  private final BranchScope scope;
  private final BranchService branches;
  private final EnrollmentRepository enrollments;
  private final LeadRepository leads;
  private final jakarta.persistence.EntityManager entityManager;
  private final com.example.academic.assessment.PlacementAssessmentRepository assessments;

  public Student require(Long id) {
    Student s = repository.findById(id).orElseThrow(ApiException::missing);
    scope.check(s.getBranchId());
    return s;
  }

  public Student lock(Long id) {
    Student s = repository.lock(id).orElseThrow(ApiException::missing);
    scope.check(s.getBranchId());
    entityManager.refresh(s, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    return s;
  }

  @Transactional(readOnly = true)
  public PageResponse<StudentDto> list(
      int page, int size, String keyword, String level, Student.Status status, Long branchId) {
    return PageResponse.of(
        repository
            .findAll(
                Query.<Student>branch(scope.filter(branchId))
                    .and(Query.keyword(keyword, "fullName", "phone", "email", "studentCode"))
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
    branches.lock(branch);
    if (id != null) s = lock(id);
    s.setBranchId(branch);
    s.setFullName(r.fullName().trim());
    s.setDateOfBirth(r.dateOfBirth());
    s.setGender(r.gender());
    s.setEmail(r.email());
    s.setPhone(r.phone().trim());
    s.setAddress(r.address());
    s.setContactPerson(r.contactPerson());
    s.setGuardianName(r.guardianName());
    s.setGuardianPhone(r.guardianPhone());
    s.setCurrentLevel(r.currentLevel());
    return StudentDto.of(repository.saveAndFlush(s));
  }

  public Student convert(String fullName, String phone, String email, String level, Long branch) {
    branches.lock(branch);
    Student s = new Student();
    s.setFullName(fullName);
    s.setPhone(phone);
    s.setEmail(email);
    s.setCurrentLevel(level);
    s.setBranchId(branch);
    return repository.saveAndFlush(s);
  }

  public StudentDto status(Long id, StudentDto.StatusRequest request) {
    var status = request.status();
    var reason = com.example.academic.lifecycle.LearningHistory.reason(request.reason());
    Student s = require(id);
    branches.lock(s.getBranchId());
    s = lock(id);
    if (request.version() == null || request.version() != s.getVersion())
      throw ApiException.conflict("Hồ sơ đã thay đổi; hãy tải lại");
    if (s.getStatus() == status) return StudentDto.of(s);
    if (status == Student.Status.GRADUATED
        && enrollments.existsByStudentIdAndStudyStatus(
            id, com.example.academic.enrollment.Enrollment.StudyStatus.RESERVED))
      throw ApiException.conflict("Cần xử lý đăng ký đang bảo lưu trước khi hoàn thành hồ sơ");
    if (status != Student.Status.STUDYING
        && enrollments.existsByStudentIdAndStudyStatus(
            id, com.example.academic.enrollment.Enrollment.StudyStatus.STUDYING))
      throw ApiException.conflict(
          "Cần bảo lưu hoặc kết thúc các đăng ký đang học trước khi đổi trạng thái hồ sơ");
    var old = s.getStatus();
    s.setStatus(status);
    history.record(
        s,
        null,
        null,
        "PROFILE_STATUS",
        old.name(),
        status.name(),
        reason,
        com.example.academic.lifecycle.LearningHistory.today(),
        null);
    repository.flush();
    return StudentDto.of(s);
  }

  public void delete(Long id) {
    Student s = require(id);
    branches.lock(s.getBranchId());
    s = lock(id);
    if (enrollments.existsByStudentId(id))
      throw ApiException.conflict("Không thể xóa học viên đã có đăng ký");
    if (leads.existsByConvertedStudentId(id))
      throw ApiException.conflict("Học viên đang được Lead chuyển đổi tham chiếu");
    if (assessments.existsByStudentId(id))
      throw ApiException.conflict("Không thể xóa học viên đã có lịch sử đánh giá đầu vào");
    if(placementExceptions.existsByStudentId(id))throw ApiException.conflict("Học viên có lịch sử đề nghị ngoại lệ; không được xóa");
    repository.delete(s);
    repository.flush();
  }
}
