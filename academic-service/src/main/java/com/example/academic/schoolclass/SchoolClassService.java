package com.example.academic.schoolclass;

import com.example.academic.branch.BranchService;
import com.example.academic.common.*;
import com.example.academic.course.*;
import com.example.academic.enrollment.Enrollment;
import com.example.academic.enrollment.EnrollmentRepository;
import com.example.academic.room.*;
import com.example.academic.schedule.*;
import com.example.academic.security.*;
import com.example.academic.student.StudentDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class SchoolClassService {
  private final SchoolClassRepository repository;
  private final BranchScope scope;
  private final BranchService branches;
  private final CourseService courses;
  private final RoomService rooms;
  private final ScheduleRepository schedules;
  private final ScheduleConflictService conflicts;
  private final EnrollmentRepository enrollments;
  private final EntityManager entityManager;

  public SchoolClass require(Long id) {
    SchoolClass c = repository.findById(id).orElseThrow(ApiException::missing);
    scope.check(c.getBranchId());
    scope.teacher(c.getTeacherId(), false);
    return c;
  }

  public SchoolClass attendance(Long id, boolean write) {
    SchoolClass c = repository.findById(id).orElseThrow(ApiException::missing);
    scope.check(c.getBranchId());
    scope.teacher(c.getTeacherId(), write);
    return c;
  }

  public SchoolClass lock(Long id) {
    SchoolClass c = repository.lock(id).orElseThrow(ApiException::missing);
    entityManager.refresh(c, LockModeType.PESSIMISTIC_WRITE);
    scope.check(c.getBranchId());
    return c;
  }

  public Specification<SchoolClass> visible(Long branchId, Long teacherId) {
    var u = Users.current();
    if (u.role() == Role.TEACHER) {
      if (u.profileId() == null) throw ApiException.forbidden();
      teacherId = u.profileId();
    }
    return Query.<SchoolClass>branch(scope.filter(branchId)).and(Query.eq("teacherId", teacherId));
  }

  @Transactional(readOnly = true)
  public PageResponse<SchoolClassDto> list(
      int page,
      int size,
      String keyword,
      SchoolClass.Status status,
      Long courseId,
      Long teacherId,
      Long branchId) {
    return PageResponse.of(
        repository
            .findAll(
                visible(branchId, teacherId)
                    .and(Query.keyword(keyword, "code", "name"))
                    .and(Query.eq("course.id", courseId))
                    .and(ClassStatus.is(status)),
                Query.page(page, size))
            .map(SchoolClassDto::of));
  }

  @Transactional(readOnly = true)
  public SchoolClassDto.Detail get(Long id) {
    SchoolClass c = require(id);
    return new SchoolClassDto.Detail(
        SchoolClassDto.of(c),
        schedules.findBySchoolClassId(id).stream().map(ScheduleDto::of).toList());
  }

  @Transactional(readOnly = true)
  public List<ScheduleDto> schedules(Long id) {
    require(id);
    return schedules.findBySchoolClassId(id).stream().map(ScheduleDto::of).toList();
  }

  @Transactional(readOnly = true)
  public List<StudentDto> students(Long id) {
    require(id);
    return enrollments
        .findBySchoolClassIdAndStudyStatus(id, Enrollment.StudyStatus.STUDYING)
        .stream()
        .map(e -> StudentDto.of(e.getStudent()))
        .toList();
  }

  public SchoolClassDto save(Long id, SchoolClassDto.Request r) {
    if (r.schedules() != null && !r.schedules().isEmpty() && Users.current().role() == Role.STAFF)
      throw ApiException.forbidden();
    SchoolClass c = id == null ? new SchoolClass() : require(id);
    Long branch = id == null ? scope.create(r.branchId()) : c.getBranchId();
    branches.lock(branch);
    if (id != null) c = lock(id);
    if (r.endDate().isBefore(r.startDate()))
      throw ApiException.bad("endDate không được trước startDate");
    Course course = courses.lockForBranch(r.courseId(), branch);
    Room room = rooms.require(r.roomId());
    if (!room.getBranchId().equals(branch)) throw ApiException.missing();
    if ((id == null || !c.getCourse().getId().equals(course.getId()))
        && course.getStatus() == Course.Status.CLOSED)
      throw ApiException.bad("Không thể tạo lớp từ khóa học CLOSED");
    if (r.maxSize() > room.getCapacity())
      throw ApiException.bad("Sức chứa phòng nhỏ hơn sĩ số tối đa");
    if (r.maxSize() > course.getMaxStudents())
      throw ApiException.bad("Sĩ số tối đa vượt giới hạn khóa học");
    if (r.maxSize() < c.getCurrentSize())
      throw ApiException.bad("Sĩ số tối đa nhỏ hơn sĩ số hiện tại");
    if (repository.existsByCodeIgnoreCaseAndBranchIdAndIdNot(
        r.code().trim(), branch, id == null ? -1L : id))
      throw ApiException.conflict("Mã lớp đã tồn tại");
    c.setBranchId(branch);
    c.setCode(r.code().trim());
    c.setName(r.name().trim());
    c.setCourse(course);
    c.setRoom(room);
    // TODO: verify teacher branch with auth-service when inter-service communication is available.
    c.setTeacherId(r.teacherId());
    c.setTeacherName(r.teacherName().trim());
    c.setStartDate(r.startDate());
    c.setEndDate(r.endDate());
    c.setMaxSize(r.maxSize());
    if (id != null && r.schedules() != null)
      throw ApiException.bad("Sửa lịch qua API schedules; PUT lớp không nhận schedules");
    List<Schedule> slots = id == null ? new ArrayList<>() : schedules.findBySchoolClassId(id);
    if (id == null && r.schedules() != null)
      for (var slot : r.schedules()) {
        Schedule s = new Schedule();
        s.setSchoolClass(c);
        s.setDayOfWeek(slot.dayOfWeek());
        s.setStartTime(slot.startTime());
        s.setEndTime(slot.endTime());
        slots.add(s);
      }
    conflicts.recheck(c, slots);
    repository.save(c);
    if (id == null) schedules.saveAll(slots);
    if (c.effectiveStatus() != SchoolClass.Status.FINISHED) {
      if (c.getCurrentSize() >= c.getMaxSize()) c.setStatus(SchoolClass.Status.FULL);
      else if (c.getStatus() == SchoolClass.Status.FULL) {
        c.setStatus(c.dateStatus());
        c.setStatusManuallySet(false);
      }
    }
    return SchoolClassDto.of(c);
  }

  public SchoolClassDto status(Long id, SchoolClass.Status status) {
    SchoolClass c = require(id);
    branches.lock(c.getBranchId());
    c = lock(id);
    if (status == SchoolClass.Status.FULL && c.getCurrentSize() < c.getMaxSize())
      throw ApiException.bad("Lớp chưa đủ sĩ số");
    if (status != SchoolClass.Status.FULL
        && status != SchoolClass.Status.FINISHED
        && c.getCurrentSize() >= c.getMaxSize()) throw ApiException.bad("Lớp đã đủ sĩ số");
    if (status != SchoolClass.Status.FINISHED)
      conflicts.recheck(c, schedules.findBySchoolClassId(id));
    c.setStatus(status);
    c.setStatusManuallySet(true);
    return SchoolClassDto.of(c);
  }

  public void delete(Long id) {
    SchoolClass c = require(id);
    branches.lock(c.getBranchId());
    if (enrollments.existsBySchoolClassId(id))
      throw ApiException.conflict("Không thể xóa lớp đã có đăng ký");
    repository.delete(c);
    repository.flush();
  }
}
