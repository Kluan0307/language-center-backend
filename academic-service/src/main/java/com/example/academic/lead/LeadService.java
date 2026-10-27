package com.example.academic.lead;

import com.example.academic.branch.BranchService;
import com.example.academic.common.*;
import com.example.academic.course.CourseService;
import com.example.academic.security.*;
import com.example.academic.student.StudentService;
import java.time.*;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LeadService {
  private final LeadRepository repository;
  private final BranchScope scope;
  private final BranchService branches;
  private final CourseService courses;
  private final StudentService students;

  public Lead require(Long id) {
    Lead l = repository.findById(id).orElseThrow(ApiException::missing);
    scope.check(l.getBranchId());
    return l;
  }

  private Lead lock(Long id) {
    Lead l = repository.lock(id).orElseThrow(ApiException::missing);
    scope.check(l.getBranchId());
    return l;
  }

  private void editable(Lead l) {
    if (l.getStatus() == Lead.Status.CONVERTED)
      throw ApiException.bad("Không thể sửa Lead đã chuyển đổi");
  }

  @Transactional(readOnly = true)
  public PageResponse<LeadDto> list(
      int page, int size, String keyword, Lead.Status status, Long assignedTo, Long branchId) {
    return PageResponse.of(
        repository
            .findAll(
                Query.<Lead>branch(scope.filter(branchId))
                    .and(Query.keyword(keyword, "fullName", "phone", "email"))
                    .and(Query.eq("status", status))
                    .and(Query.eq("assignedTo", assignedTo)),
                Query.page(page, size))
            .map(LeadDto::of));
  }

  @Transactional(readOnly = true)
  public LeadDto get(Long id) {
    return LeadDto.of(require(id));
  }

  public LeadDto save(Long id, LeadDto.Request r) {
    Lead l = id == null ? new Lead() : lock(id);
    editable(l);
    Long branch = id == null ? scope.create(r.branchId()) : l.getBranchId();
    branches.require(branch);
    l.setBranchId(branch);
    l.setFullName(r.fullName().trim());
    l.setPhone(r.phone().trim());
    l.setEmail(r.email());
    l.setSource(r.source());
    l.setDesiredLevel(r.desiredLevel());
    l.setInterestedCourse(
        r.interestedCourseId() == null ? null : courses.forBranch(r.interestedCourseId(), branch));
    if (id == null) {
      var u = Users.current();
      l.setAssignedTo(u.accountId());
      l.setAssignedToName(u.username());
      l.setNote(r.note());
    } else append(l, r.note());
    return LeadDto.of(repository.save(l));
  }

  private void append(Lead l, String note) {
    if (note != null && !note.isBlank()) {
      String line =
          "["
              + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
              + "] "
              + note.trim();
      l.setNote(l.getNote() == null || l.getNote().isBlank() ? line : l.getNote() + "\n" + line);
    }
  }

  public LeadDto status(Long id, LeadDto.StatusRequest r) {
    Lead l = lock(id);
    editable(l);
    if (r.status() == Lead.Status.CONVERTED)
      throw ApiException.bad("Dùng API convert để chuyển đổi Lead");
    l.setStatus(r.status());
    append(l, r.note());
    l.setLastContactedAt(LocalDateTime.now());
    return LeadDto.of(l);
  }

  public LeadDto convert(Long id) {
    Lead l = lock(id);
    if (l.getStatus() != Lead.Status.CONTACTED && l.getStatus() != Lead.Status.CONSULTING)
      throw ApiException.bad("Chỉ chuyển đổi Lead CONTACTED hoặc CONSULTING");
    l.setConvertedStudent(
        students.convert(
            l.getFullName(), l.getPhone(), l.getEmail(), l.getDesiredLevel(), l.getBranchId()));
    l.setStatus(Lead.Status.CONVERTED);
    return LeadDto.of(l);
  }
}
