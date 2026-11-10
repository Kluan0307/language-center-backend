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
  private final LeadActivityRepository activities;
  private final LeadAssigneeDirectory assignees;
  private final BranchScope scope;
  private final BranchService branches;
  private final CourseService courses;
  private final StudentService students;
  private final jakarta.persistence.EntityManager entityManager;

  public Lead require(Long id) {
    Lead l = repository.findById(id).orElseThrow(ApiException::missing);
    scope.check(l.getBranchId());
    return l;
  }

  private Lead lock(Long id) {
    Lead initial = require(id);
    branches.lock(initial.getBranchId());
    Lead l = repository.lock(id).orElseThrow(ApiException::missing);
    scope.check(l.getBranchId());
    entityManager.refresh(l, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    return l;
  }

  private void owner(Lead l) {
    if (Users.current().role() == Role.TEACHER) throw ApiException.forbidden();
    if (l.getId() != null
        && Users.current().role() == Role.STAFF
        && !java.util.Objects.equals(Users.current().accountId(), l.getAssignedTo()))
      throw ApiException.forbidden();
  }

  private void target(Lead l, Lead.Status next) {
    if (next == Lead.Status.NEW && l.getStatus() != Lead.Status.NEW)
      throw ApiException.bad("Không đưa Lead đã xử lý về Mới; dùng Liên hệ/Tư vấn để mở lại");
  }

  private void version(Lead l, Long v) {
    if (v == null || v != l.getVersion())
      throw ApiException.conflict("Lead đã thay đổi; hãy tải lại");
  }

  private String text(String v) {
    if (v == null || v.trim().length() < 3 || v.trim().length() > 2000)
      throw ApiException.bad("Nhập nội dung từ 3 đến 2000 ký tự");
    return v.trim();
  }

  private void editable(Lead l) {
    owner(l);
    if (l.getStatus() == Lead.Status.CONVERTED)
      throw ApiException.bad("Không thể sửa Lead đã chuyển đổi");
  }

  @Transactional(readOnly = true)
  public PageResponse<LeadDto> list(
      int page, int size, String keyword, Lead.Status status, Long assignedTo, Long branchId) {
    return list(page, size, keyword, status, assignedTo, branchId, null, null);
  }

  @Transactional(readOnly = true)
  public PageResponse<LeadDto> list(
      int page,
      int size,
      String keyword,
      Lead.Status status,
      Long assignedTo,
      Long branchId,
      Lead.FollowUp followUp,
      Boolean active) {
    org.springframework.data.jpa.domain.Specification<Lead> follow =
        (r, q, b) -> {
          var open =
              r.get("status").in(Lead.Status.NEW, Lead.Status.CONTACTED, Lead.Status.CONSULTING);
          if (followUp == null) return Boolean.TRUE.equals(active) ? open : b.conjunction();
          var now = LocalDateTime.now(com.example.academic.common.BusinessTime.ZONE);
          var date = r.<LocalDateTime>get("nextContactAt");
          return b.and(
              open,
              switch (followUp) {
                case OVERDUE -> b.lessThan(date, now);
                case TODAY ->
                    b.and(
                        b.greaterThanOrEqualTo(date, now.toLocalDate().atStartOfDay()),
                        b.lessThan(date, now.toLocalDate().plusDays(1).atStartOfDay()));
                case UPCOMING -> b.greaterThan(date, now);
                case UNSCHEDULED -> b.isNull(date);
              });
        };
    return PageResponse.of(
        repository
            .findAll(
                Query.<Lead>branch(scope.filter(branchId))
                    .and(Query.keyword(keyword, "fullName", "phone", "email", "assignedToName"))
                    .and(Query.eq("status", status))
                    .and(Query.eq("assignedTo", assignedTo))
                    .and(follow),
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
    if (id != null) version(l, r.version());
    Long branch = id == null ? scope.create(r.branchId()) : l.getBranchId();
    if (id == null) branches.lock(branch);
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
    repository.save(l);
    record(
        l,
        id == null ? "CREATED" : "PROFILE_UPDATED",
        r.note() == null || r.note().isBlank() ? "Cập nhật thông tin Lead" : r.note().trim(),
        null,
        null,
        null,
        null,
        null);
    repository.flush();
    return LeadDto.of(l);
  }

  private void append(Lead l, String note) {
    if (note != null && !note.isBlank()) {
      String line =
          "["
              + LocalDateTime.now(com.example.academic.common.BusinessTime.ZONE)
                  .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
              + "] "
              + note.trim();
      l.setNote(l.getNote() == null || l.getNote().isBlank() ? line : l.getNote() + "\n" + line);
    }
  }

  public LeadDto status(Long id, LeadDto.StatusRequest r) {
    Lead l = lock(id);
    editable(l);
    version(l, r.version());
    target(l, r.status());
    if (r.status() == Lead.Status.CONVERTED)
      throw ApiException.bad("Dùng API convert để chuyển đổi Lead");
    var old = l.getStatus();
    var next = l.getNextContactAt();
    l.setStatus(r.status());
    if (!l.active()) l.setNextContactAt(null);
    record(l, "STATUS_CHANGED", text(r.note()), null, old.name(), null, null, next);
    repository.flush();
    return LeadDto.of(l);
  }

  public LeadDto convert(Long id, Long expectedVersion) {
    Lead l = lock(id);
    owner(l);
    if (l.getStatus() == Lead.Status.CONVERTED && l.getConvertedStudent() != null)
      return LeadDto.of(l);
    version(l, expectedVersion);
    if (l.getStatus() != Lead.Status.CONTACTED && l.getStatus() != Lead.Status.CONSULTING)
      throw ApiException.bad("Chỉ chuyển đổi Lead CONTACTED hoặc CONSULTING");
    l.setConvertedStudent(
        students.convert(
            l.getFullName(), l.getPhone(), l.getEmail(), l.getDesiredLevel(), l.getBranchId()));
    var old = l.getStatus();
    var next = l.getNextContactAt();
    l.setStatus(Lead.Status.CONVERTED);
    l.setNextContactAt(null);
    record(
        l,
        "CONVERTED",
        "Chuyển thành học viên #" + l.getConvertedStudent().getId(),
        null,
        old.name(),
        null,
        null,
        next);
    repository.flush();
    return LeadDto.of(l);
  }

  private void record(
      Lead l,
      String action,
      String note,
      String channel,
      String oldStatus,
      Long oldOwner,
      String oldName,
      LocalDateTime oldNext) {
    if (!"CREATED".equals(action))
      l.setUpdatedAt(LocalDateTime.now(com.example.academic.common.BusinessTime.ZONE));
    var e = new LeadActivity();
    e.setLeadId(l.getId());
    e.setAction(action);
    e.setNote(note);
    e.setChannel(channel);
    e.setOldStatus(oldStatus);
    e.setNewStatus(l.getStatus().name());
    e.setOldAssigneeId(oldOwner);
    e.setOldAssigneeName(oldName);
    e.setAssigneeId(l.getAssignedTo());
    e.setAssigneeName(l.getAssignedToName());
    e.setOldNextContactAt(oldNext);
    e.setNextContactAt(l.getNextContactAt());
    e.setActorId(Users.current().accountId());
    e.setActorName(Users.current().username());
    activities.save(e);
  }

  @Transactional(readOnly = true)
  public java.util.List<LeadActivity> history(Long id) {
    require(id);
    return activities.findByLeadIdOrderByIdDesc(id);
  }

  public LeadDto interact(Long id, LeadDto.Interaction r) {
    var l = lock(id);
    editable(l);
    version(l, r.version());
    target(l, r.status());
    if (r.status() == Lead.Status.CONVERTED)
      throw ApiException.bad("Dùng thao tác chuyển học viên");
    if (r.nextContactAt() != null
        && !r.nextContactAt()
            .isAfter(LocalDateTime.now(com.example.academic.common.BusinessTime.ZONE)))
      throw ApiException.bad("Lịch hẹn phải sau thời điểm hiện tại");
    if (r.nextContactAt() != null && r.status() == Lead.Status.LOST)
      throw ApiException.bad("Lead đã mất không giữ lịch hẹn mới");
    var old = l.getStatus();
    var next = l.getNextContactAt();
    l.setStatus(r.status());
    l.setNextContactAt(r.nextContactAt());
    if (r.channel() != LeadDto.Channel.NOTE)
      l.setLastContactedAt(LocalDateTime.now(com.example.academic.common.BusinessTime.ZONE));
    record(l, "INTERACTION", text(r.note()), r.channel().name(), old.name(), null, null, next);
    repository.flush();
    return LeadDto.of(l);
  }

  public LeadDto assign(Long id, LeadDto.Assignment r) {
    if (Users.current().role() != Role.ADMIN && Users.current().role() != Role.SUPER_ADMIN)
      throw ApiException.forbidden();
    var l = lock(id);
    editable(l);
    version(l, r.version());
    var target = assignees.require(r.accountId(), l.getBranchId());
    if (target.accountId().equals(l.getAssignedTo())) return LeadDto.of(l);
    var old = l.getAssignedTo();
    var name = l.getAssignedToName();
    l.setAssignedTo(target.accountId());
    l.setAssignedToName(target.fullName() == null ? target.username() : target.fullName());
    record(
        l,
        "REASSIGNED",
        text(r.reason()),
        null,
        l.getStatus().name(),
        old,
        name,
        l.getNextContactAt());
    repository.flush();
    return LeadDto.of(l);
  }
}
