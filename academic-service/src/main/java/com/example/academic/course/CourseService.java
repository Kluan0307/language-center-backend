package com.example.academic.course;

import com.example.academic.branch.BranchService;
import com.example.academic.common.*;
import com.example.academic.schoolclass.SchoolClassRepository;
import com.example.academic.security.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class CourseService {
  private final CourseRepository repository;
  private final BranchScope scope;
  private final BranchService branches;
  private final SchoolClassRepository classes;
  private final EntityManager entityManager;

  public Course require(Long id) {
    Course c = repository.findById(id).orElseThrow(ApiException::missing);
    scope.course(c.getBranchId());
    return c;
  }

  public Course forBranch(Long id, Long branchId) {
    Course c = require(id);
    if (c.getBranchId() != null && !c.getBranchId().equals(branchId)) throw ApiException.missing();
    return c;
  }

  public Course lockForBranch(Long id, Long branchId) {
    Course c = repository.lock(id).orElseThrow(ApiException::missing);
    entityManager.refresh(c, LockModeType.PESSIMISTIC_WRITE);
    scope.course(c.getBranchId());
    if (c.getBranchId() != null && !c.getBranchId().equals(branchId)) throw ApiException.missing();
    return c;
  }

  public Specification<Course> visible(Long requested) {
    Long branch = scope.filter(requested);
    return (r, q, b) ->
        branch == null
            ? b.conjunction()
            : b.or(b.isNull(r.get("branchId")), b.equal(r.get("branchId"), branch));
  }

  @Transactional(readOnly = true)
  public PageResponse<CourseDto> list(
      int page,
      int size,
      String keyword,
      Course.Status status,
      String level,
      String type,
      Long branchId) {
    return PageResponse.of(
        repository
            .findAll(
                visible(branchId)
                    .and(Query.keyword(keyword, "code", "name"))
                    .and(Query.eq("status", status))
                    .and(Query.eq("level", level))
                    .and(Query.eq("type", type)),
                Query.page(page, size))
            .map(CourseDto::of));
  }

  @Transactional(readOnly = true)
  public CourseDto get(Long id) {
    return CourseDto.of(require(id));
  }

  private void writable(Course c) {
    if (c.getBranchId() == null && Users.current().role() != Role.SUPER_ADMIN)
      throw ApiException.forbidden();
  }

  public CourseDto save(Long id, CourseDto.Request r) {
    Course c = id == null ? new Course() : require(id);
    if (id != null) {
      c = lockForBranch(id, c.getBranchId());
      writable(c);
    }
    Long branch = id == null ? scope.filter(r.branchId()) : c.getBranchId();
    if (branch != null) branches.require(branch);
    if (repository.existsByCodeIgnoreCaseAndBranchIdAndIdNot(
        r.code().trim(), branch, id == null ? -1L : id))
      throw ApiException.conflict("Mã khóa học đã tồn tại");
    c.setBranchId(branch);
    writable(c);
    c.setCode(r.code().trim());
    c.setName(r.name().trim());
    c.setType(r.type());
    c.setLevel(r.level());
    c.setDuration(r.duration());
    c.setTuitionFee(r.tuitionFee());
    c.setContent(r.content());
    c.setEntryRequirement(r.entryRequirement());
    c.setMaxStudents(r.maxStudents());
    if (id != null && classes.existsByCourseIdAndMaxSizeGreaterThan(id, r.maxStudents()))
      throw ApiException.bad("Giới hạn khóa học nhỏ hơn sĩ số tối đa của lớp hiện có");
    return CourseDto.of(repository.save(c));
  }

  public CourseDto status(Long id, Course.Status status) {
    Course c = require(id);
    c = lockForBranch(id, c.getBranchId());
    writable(c);
    c.setStatus(status);
    return CourseDto.of(c);
  }
}
