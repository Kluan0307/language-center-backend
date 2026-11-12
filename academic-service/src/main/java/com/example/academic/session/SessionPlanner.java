package com.example.academic.session;

import com.example.academic.attendance.AttendanceRepository;
import com.example.academic.branch.BranchService;
import com.example.academic.common.*;
import com.example.academic.enrollment.*;
import com.example.academic.room.*;
import com.example.academic.schedule.*;
import com.example.academic.schoolclass.*;
import com.example.academic.security.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class SessionPlanner {
  private final com.example.academic.results.ResultBarrierService resultBarrier;
  private final ClassSessionRepository repository;
  private final SessionEventRepository events;
  private final SchoolClassRepository classes;
  private final ScheduleRepository schedules;
  private final BranchService branches;
  private final BranchScope scope;
  private final TeacherDirectory teachers;
  private final RoomRepository rooms;
  private final AttendanceRepository attendance;
  private final EnrollmentRepository enrollments;
  private final EnrollmentPeriodRepository periods;
  private final EntityManager em;

  public static LocalDate today() {
    return LocalDate.now(BusinessTime.ZONE);
  }

  private boolean manager() {
    return Users.current().role() == Role.SUPER_ADMIN || Users.current().role() == Role.ADMIN;
  }

  private void manage() {
    if (!manager()) throw ApiException.forbidden();
  }

  public boolean canAttend(ClassSession s) {
    if (s.getStatus() == ClassSession.Status.CANCELLED || s.getSessionDate().isAfter(today()))
      return false;
    if (s.getStatus() == ClassSession.Status.COMPLETED) return manager();
    if (s.getSchoolClass().getStatus() == SchoolClass.Status.FINISHED) return false;
    var u = Users.current();
    return u.role() != Role.TEACHER || Objects.equals(u.profileId(), s.getTeacherId());
  }

  public ClassSession require(Long id, boolean write) {
    var s = repository.findById(id).orElseThrow(ApiException::missing);
    var c = s.getSchoolClass();
    scope.check(c.getBranchId());
    var u = Users.current();
    if (u.role() == Role.TEACHER
        && (u.profileId() == null
            || (!Objects.equals(u.profileId(), s.getTeacherId())
                && !Objects.equals(u.profileId(), c.getTeacherId())))) throw ApiException.missing();
    if (write && !canAttend(s)) throw ApiException.forbidden();
    return s;
  }

  public SessionDto dto(ClassSession s) {
    return SessionDto.of(s, canAttend(s));
  }

  public ClassSession resolve(Long id, Long classId, LocalDate date, boolean write) {
    if (id != null) {
      var s = require(id, write);
      if (classId != null && !classId.equals(s.getSchoolClass().getId())
          || date != null && !date.equals(s.getSessionDate()))
        throw ApiException.bad("Buổi học không khớp lớp/ngày");
      return s;
    }
    if (classId == null || date == null)
      throw ApiException.bad("Chọn sessionId hoặc classId và date");
    var c = classes.findById(classId).orElseThrow(ApiException::missing);
    scope.check(c.getBranchId());
    var all = repository.findBySchoolClassIdAndSessionDate(classId, date);
    var active = all.stream().filter(s -> s.getStatus() != ClassSession.Status.CANCELLED).toList();
    if (active.size() > 1) throw ApiException.conflict("Chọn sessionId vì ngày này có nhiều buổi");
    if (active.size() == 1) return require(active.getFirst().getId(), write);
    if (all.size() == 1) return require(all.getFirst().getId(), write);
    throw ApiException.bad("Ngày này chưa có buổi học thực tế. Hãy sinh/chọn buổi học");
  }

  @Transactional(readOnly = true)
  public PageResponse<SessionDto> list(
      int page,
      int size,
      Long branch,
      Long classId,
      Long teacher,
      ClassSession.Status status,
      LocalDate from,
      LocalDate to) {
    if (from != null && to != null && from.isAfter(to))
      throw ApiException.bad("Từ ngày không được sau đến ngày");
    if (classId != null) {
      var c = classes.findById(classId).orElseThrow(ApiException::missing);
      scope.check(c.getBranchId());
    }
    if (Users.current().role() == Role.TEACHER) {
      teacher = Users.current().profileId();
      if (teacher == null) throw ApiException.forbidden();
    }
    Specification<ClassSession> spec =
        Query.<ClassSession>eq("schoolClass.branchId", scope.filter(branch))
            .and(Query.eq("schoolClass.id", classId))
            .and(Query.eq("teacherId", teacher))
            .and(Query.eq("status", status));
    spec =
        spec.and(
            (r, q, b) -> {
              var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
              if (from != null) predicates.add(b.greaterThanOrEqualTo(r.get("sessionDate"), from));
              if (to != null) predicates.add(b.lessThanOrEqualTo(r.get("sessionDate"), to));
              return b.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
            });
    Query.page(page, size);
    return PageResponse.of(
        repository
            .findAll(spec, PageRequest.of(page, size, Sort.by("sessionDate", "startTime", "id")))
            .map(this::dto));
  }

  @Transactional(readOnly = true)
  public List<SessionDto.Event> history(Long id) {
    require(id, false);
    return events.findBySessionIdOrderByIdDesc(id).stream().map(SessionDto.Event::of).toList();
  }

  private SchoolClass lockClass(Long id) {
    var c = classes.findById(id).orElseThrow(ApiException::missing);
    scope.check(c.getBranchId());
    branches.lock(c.getBranchId());
    c = classes.lock(id).orElseThrow(ApiException::missing);
    em.refresh(c, LockModeType.PESSIMISTIC_WRITE);
    return c;
  }

  public ClassSession lockSession(Long id) {
    var initial = require(id, false);
    lockClass(initial.getSchoolClass().getId());
    resultBarrier.mutable(initial.getSchoolClass().getId(),initial.getSchoolClass().getBranchId());
    var s = repository.lock(id).orElseThrow(ApiException::missing);
    em.refresh(s, LockModeType.PESSIMISTIC_WRITE);
    return require(id, false);
  }

  private void version(ClassSession s, Long version) {
    if (version == null || s.getVersion() != version)
      throw ApiException.conflict("Buổi học đã thay đổi. Hãy tải lại trước khi lưu");
  }

  private void editable(ClassSession s) {
    if (s.getStatus() == ClassSession.Status.COMPLETED || hasAttendance(s))
      throw ApiException.conflict("Buổi đã điểm danh được khóa ngày/giờ/phòng/giáo viên");
  }

  private boolean hasAttendance(ClassSession s) {
    return attendance.existsByEnrollmentSchoolClassIdAndSessionDate(
        s.getSchoolClass().getId(), s.getSessionDate());
  }

  private void openClass(SchoolClass c) {
    if (c.getStatus() == SchoolClass.Status.FINISHED)
      throw ApiException.conflict("Lớp đã được quản trị kết thúc. Hãy mở lại trước khi sửa buổi");
  }

  private String reason(String value) {
    if (value == null || value.trim().length() < 3 || value.trim().length() > 500)
      throw ApiException.bad("Nhập lý do từ 3 đến 500 ký tự");
    return value.trim();
  }

  private static String snapshot(ClassSession s) {
    return "date="
        + s.getSessionDate()
        + "; time="
        + s.getStartTime()
        + "–"
        + s.getEndTime()
        + "; room="
        + s.getRoomId()
        + "/"
        + s.getRoomName()
        + "; plannedTeacher="
        + s.getPlannedTeacherId()
        + "/"
        + s.getPlannedTeacherName()
        + "; teacher="
        + s.getTeacherId()
        + "/"
        + s.getTeacherName()
        + "; status="
        + s.getStatus()
        + "; kind="
        + s.getKind()
        + "; source="
        + s.getSourceDate()
        + "/"
        + s.getSourceScheduleId()
        + "; makeupFor="
        + s.getMakeupForId()
        + "; makeupChild="
        + s.getMakeupSessionId()
        + "; customized="
        + s.isCustomized();
  }

  public void record(ClassSession s, String action, String before, String reason) {
    var e = new SessionEvent();
    e.setSession(s);
    e.setAction(action);
    e.setBeforeState(before);
    e.setAfterState(snapshot(s));
    e.setReason(reason);
    e.setActorId(Users.current().accountId());
    e.setActorName(Users.current().username());
    e.setOccurredAt(LocalDateTime.now(BusinessTime.ZONE));
    events.save(e);
    if(java.util.Set.of("UPDATED","CANCELLED","RESTORED","MAKEUP_CREATED","MAKEUP_LINKED","TEMPLATE_SYNC","TIME_CORRECTED").contains(action)){
      java.time.LocalDate previous=null;if(before!=null&&before.startsWith("date="))previous=java.time.LocalDate.parse(before.substring(5,15));
      com.example.academic.notification.StudentEvents.add(em,"academic:session:"+e.getId(),"SCHEDULE","SESSION_"+action,"Lịch học có cập nhật","Trung tâm đã cập nhật một buổi học. Mở lịch để xem thông tin hiện hành.",null,null,s.getSchoolClass().getId(),null,s.getSchoolClass().getBranchId(),"SESSION",s.getId().toString(),s.getSessionDate(),previous);
    }
  }

  private ClassSession copy(ClassSession s) {
    var n = new ClassSession();
    n.setId(s.getId());
    n.setVersion(s.getVersion());
    apply(n, s);
    return n;
  }

  private void apply(ClassSession target, ClassSession s) {
    target.setSchoolClass(s.getSchoolClass());
    target.setSessionDate(s.getSessionDate());
    target.setStartTime(s.getStartTime());
    target.setEndTime(s.getEndTime());
    target.setRoomId(s.getRoomId());
    target.setRoomName(s.getRoomName());
    target.setPlannedTeacherId(s.getPlannedTeacherId());
    target.setPlannedTeacherName(s.getPlannedTeacherName());
    target.setTeacherId(s.getTeacherId());
    target.setTeacherName(s.getTeacherName());
    target.setStatus(s.getStatus());
    target.setKind(s.getKind());
    target.setSourceDate(s.getSourceDate());
    target.setSourceScheduleId(s.getSourceScheduleId());
    target.setMakeupForId(s.getMakeupForId());
    target.setMakeupSessionId(s.getMakeupSessionId());
    target.setCustomized(s.isCustomized());
    target.setReason(s.getReason());
  }

  private ClassSession planned(SchoolClass c, Schedule schedule, LocalDate date) {
    var s = new ClassSession();
    s.setSchoolClass(c);
    s.setSessionDate(date);
    s.setSourceDate(date);
    s.setSourceScheduleId(schedule.getId());
    s.setStartTime(schedule.getStartTime());
    s.setEndTime(schedule.getEndTime());
    s.setRoomId(c.getRoom().getId());
    s.setRoomName(c.getRoom().getName());
    s.setPlannedTeacherId(c.getTeacherId());
    s.setPlannedTeacherName(c.getTeacherName());
    s.setTeacherId(c.getTeacherId());
    s.setTeacherName(c.getTeacherName());
    return s;
  }

  private void range(LocalDate from, LocalDate to) {
    if (from == null || to == null || from.isAfter(to) || ChronoUnit.DAYS.between(from, to) > 3660)
      throw ApiException.bad("Khoảng sinh buổi phải hợp lệ và tối đa 3660 ngày");
  }

  private Map<LocalDate, Schedule> desired(SchoolClass c, LocalDate from, LocalDate to) {
    range(from, to);
    var byDay = new EnumMap<DayOfWeek, Schedule>(DayOfWeek.class);
    for (var s : schedules.findBySchoolClassId(c.getId())) {
      if (com.example.academic.duration.DurationProgressService.minutes(s.getStartTime(),s.getEndTime())==null)
        throw ApiException.bad("Giờ kết thúc phải sau giờ bắt đầu");
      if (byDay.put(s.getDayOfWeek(), s) != null)
        throw ApiException.conflict("Mỗi lớp tối đa một lịch mẫu cho mỗi thứ");
    }
    var result = new LinkedHashMap<LocalDate, Schedule>();
    for (var d = from; !d.isAfter(to); d = d.plusDays(1)) {
      var s = byDay.get(d.getDayOfWeek());
      if (s != null) result.put(d, s);
    }
    return result;
  }

  public SessionDto.Generated generate(SessionDto.Generate r) {
    manage();
    var c = lockClass(r.classId());
    resultBarrier.mutable(c.getId(),c.getBranchId());
    openClass(c);
    var from = r.from() == null ? c.getStartDate() : r.from();
    var to = r.to() == null ? c.getEndDate() : r.to();
    if (from.isBefore(c.getStartDate()) || to.isAfter(c.getEndDate()))
      throw ApiException.bad("Sinh lịch mẫu trong khoảng ngày của lớp");
    return generateRange(c, from, to);
  }

  private SessionDto.Generated generateRange(SchoolClass c, LocalDate from, LocalDate to) {
    var existing = repository.findBySchoolClassIdOrderBySessionDateAsc(c.getId());
    Set<LocalDate> sources = new HashSet<>();
    existing.forEach(s -> sources.add(s.getSourceDate()));
    var proposals = new ArrayList<ClassSession>();
    desired(c, from, to)
        .forEach(
            (d, s) -> {
              if (!sources.contains(d)) proposals.add(planned(c, s, d));
            });
    validate(c, proposals, existing, true);
    for (var s : proposals) {
      repository.save(s);
      record(s, "GENERATED", null, "Sinh từ lịch tuần");
    }
    repository.flush();
    endDate(c);
    return new SessionDto.Generated(proposals.size(), 0);
  }

  // Caller already holds the branch and class write locks. Initial generation covers the whole
  // class.
  public SessionDto.Generated sync(SchoolClass c, boolean initial) {
    resultBarrier.mutable(c.getId(),c.getBranchId());
    var existing = repository.findBySchoolClassIdOrderBySessionDateAsc(c.getId());
    var proposals = new ArrayList<ClassSession>();
    if (c.getStatus() == SchoolClass.Status.FINISHED) {
      for (var s : existing)
        if (s.getStatus() == ClassSession.Status.SCHEDULED && !hasAttendance(s)) {
          var n = copy(s);
          n.setStatus(ClassSession.Status.CANCELLED);
          n.setReason("Quản trị kết thúc lớp");
          proposals.add(n);
        }
    } else {
      range(c.getStartDate(), c.getEndDate());
      var from = initial ? c.getStartDate() : today().plusDays(1);
      Map<LocalDate, Schedule> wanted =
          from.isAfter(c.getEndDate())
              ? Map.of()
              : desired(
                  c, from.isBefore(c.getStartDate()) ? c.getStartDate() : from, c.getEndDate());
      var seen = new HashSet<LocalDate>();
      for (var s : existing) {
        if (s.getSourceDate() != null) seen.add(s.getSourceDate());
        if (s.getKind() != ClassSession.Kind.REGULAR
            || s.isCustomized()
            || !s.getSessionDate().isAfter(today())
            || s.getStatus() == ClassSession.Status.COMPLETED
            || hasAttendance(s)) continue;
        var slot = wanted.get(s.getSourceDate());
        var n = slot == null ? copy(s) : planned(c, slot, s.getSourceDate());
        n.setId(s.getId());
        n.setVersion(s.getVersion());
        if (slot == null) {
          n.setStatus(ClassSession.Status.CANCELLED);
          n.setReason("Lịch tuần đã thay đổi");
        } else n.setReason("Đồng bộ lịch tuần");
        if (!snapshot(s).equals(snapshot(n))) proposals.add(n);
      }
      wanted.forEach(
          (d, s) -> {
            if (!seen.contains(d)) proposals.add(planned(c, s, d));
          });
    }
    var validation = new ArrayList<ClassSession>(proposals);
    for (var row : existing)
      if (row.getStatus() == ClassSession.Status.SCHEDULED
          && proposals.stream().noneMatch(n -> n.getId() != null && n.getId().equals(row.getId())))
        validation.add(copy(row));
    validate(c, validation, existing, true);
    int created = 0, changed = 0;
    for (var n : proposals) {
      ClassSession s;
      if (n.getId() == null) {
        s = n;
        created++;
        repository.save(s);
        record(s, "GENERATED", null, "Sinh từ lịch tuần");
      } else {
        s = existing.stream().filter(x -> x.getId().equals(n.getId())).findFirst().orElseThrow();
        var before = snapshot(s);
        apply(s, n);
        repository.save(s);
        record(s, "TEMPLATE_SYNC", before, n.getReason());
        changed++;
      }
    }
    repository.flush();
    endDate(c);
    return new SessionDto.Generated(created, changed);
  }

  private void lockTeachers(Collection<ClassSession> proposals, Collection<ClassSession> old) {
    var ids = new TreeSet<Long>();
    for (var s : proposals) if (s.getTeacherId() != null) ids.add(s.getTeacherId());
    for (var s : old)
      if (proposals.stream().anyMatch(p -> Objects.equals(p.getId(), s.getId()))
          && s.getTeacherId() != null) ids.add(s.getTeacherId());
    ids.forEach(teachers::lock);
  }

  public void lockClassTeachers(SchoolClass c, Long nextTeacher) {
    var ids = new TreeSet<Long>();
    if (c.getTeacherId() != null) ids.add(c.getTeacherId());
    if (nextTeacher != null) ids.add(nextTeacher);
    if (c.getId() != null)
      for (var s : repository.findBySchoolClassIdOrderBySessionDateAsc(c.getId()))
        if (s.getStatus() == ClassSession.Status.SCHEDULED && s.getTeacherId() != null)
          ids.add(s.getTeacherId());
    ids.forEach(teachers::lock);
  }

  private Set<Long> roster(Long classId, LocalDate date) {
    if (classId == null) return Set.of();
    var result = new HashSet<Long>();
    var tracked = new HashSet<Long>();
    for (var p : periods.findByEnrollmentSchoolClassId(classId)) {
      tracked.add(p.getEnrollment().getId());
      if (p.includes(date)) result.add(p.getEnrollment().getStudent().getId());
    }
    for (var e : enrollments.findBySchoolClassId(classId))
      if (!tracked.contains(e.getId())
          && e.getStudyStatus() == Enrollment.StudyStatus.STUDYING
          && !date.isBefore(e.getEnrolledAt().toLocalDate())) result.add(e.getStudent().getId());
    return result;
  }

  public static boolean overlap(ClassSession a, ClassSession b) {
    return a.getSessionDate().equals(b.getSessionDate())
        && a.getStartTime() != null
        && a.getEndTime() != null
        && b.getStartTime() != null
        && b.getEndTime() != null
        && a.getStartTime().isBefore(b.getEndTime())
        && a.getEndTime().isAfter(b.getStartTime());
  }

  private void validate(
      SchoolClass c, List<ClassSession> proposals, List<ClassSession> existing, boolean lock) {
    if (proposals.isEmpty()) return;
    if (lock) lockTeachers(proposals, existing);
    var active =
        proposals.stream().filter(s -> s.getStatus() == ClassSession.Status.SCHEDULED).toList();
    if (active.isEmpty()) return;
    LocalDate
        first =
            active.stream()
                .map(ClassSession::getSessionDate)
                .min(LocalDate::compareTo)
                .orElseThrow(),
        last =
            active.stream()
                .map(ClassSession::getSessionDate)
                .max(LocalDate::compareTo)
                .orElseThrow();
    var pool =
        new ArrayList<>(
            repository
                .conflictCandidates(
                    c.getBranchId(),
                    active.stream()
                        .map(ClassSession::getTeacherId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList(),
                    first,
                    last)
                .stream()
                .filter(s -> !Objects.equals(s.getSchoolClass().getId(), c.getId()))
                .toList());
    for (var s : existing)
      if (proposals.stream().noneMatch(n -> n.getId() != null && n.getId().equals(s.getId())))
        pool.add(s);
    pool.addAll(proposals);
    var byDate =
        pool.stream().collect(java.util.stream.Collectors.groupingBy(ClassSession::getSessionDate));
    var teacherCache = new HashMap<Long, TeacherDirectory.Teacher>();
    var roomCache = new HashMap<Long, Room>();
    var rosterCache = new HashMap<String, Set<Long>>();
    for (var s : active) {
      if (s.getSessionDate().isBefore(c.getStartDate())
          || s.getStartTime() == null
          || s.getEndTime() == null
          || com.example.academic.duration.DurationProgressService.minutes(s.getStartTime(),s.getEndTime())==null)
        throw ApiException.bad("Ngày/giờ buổi học không hợp lệ");
      var room =
          roomCache.computeIfAbsent(
              s.getRoomId(), id -> rooms.findById(id).orElseThrow(ApiException::missing));
      if (!room.getBranchId().equals(c.getBranchId())) throw ApiException.missing();
      if (room.getStatus() == Room.Status.MAINTENANCE) throw ApiException.bad("Phòng đang bảo trì");
      if (c.getMaxSize() != null && room.getCapacity() < c.getMaxSize())
        throw ApiException.bad("Phòng không đủ sức chứa cho lớp");
      s.setRoomName(room.getName());
      var teacher =
          teacherCache.computeIfAbsent(
              s.getTeacherId(), id -> teachers.require(id, c.getBranchId()));
      s.setTeacherName(teacher.fullName());
      if (Objects.equals(s.getPlannedTeacherId(), s.getTeacherId()) && !s.isCustomized())
        s.setPlannedTeacherName(teacher.fullName());
      for (var other : byDate.getOrDefault(s.getSessionDate(), List.of())) {
        if (other == s
            || s.getId() != null && s.getId().equals(other.getId())
            || other.getStatus() == ClassSession.Status.CANCELLED) continue;
        if (Objects.equals(c.getId(), other.getSchoolClass().getId())
            && s.getSessionDate().equals(other.getSessionDate()))
          throw ApiException.conflict("Mỗi lớp chỉ có một buổi đang hiệu lực trong một ngày");
        if (!overlap(s, other)) continue;
        if (Objects.equals(s.getTeacherId(), other.getTeacherId()))
          throw ApiException.conflict(
              "Trùng lịch giáo viên ngày " + s.getSessionDate() + " (có thể tại chi nhánh khác)");
        if (Objects.equals(s.getRoomId(), other.getRoomId()))
          throw ApiException.conflict("Trùng phòng ngày " + s.getSessionDate());
        if (!Objects.equals(c.getBranchId(), other.getSchoolClass().getBranchId())) continue;
        var own =
            rosterCache.computeIfAbsent(
                c.getId() + ":" + s.getSessionDate(), k -> roster(c.getId(), s.getSessionDate()));
        var theirs =
            rosterCache.computeIfAbsent(
                other.getSchoolClass().getId() + ":" + s.getSessionDate(),
                k -> roster(other.getSchoolClass().getId(), s.getSessionDate()));
        if (!Collections.disjoint(own, theirs))
          throw ApiException.conflict("Trùng lịch học viên ngày " + s.getSessionDate());
      }
    }
  }

  private void endDate(SchoolClass c) {
    c.setSessionsInitialized(true);
    c.setSessionEndDate(
        repository.findBySchoolClassIdOrderBySessionDateAsc(c.getId()).stream()
            .filter(s -> s.getStatus() != ClassSession.Status.CANCELLED)
            .map(ClassSession::getSessionDate)
            .max(LocalDate::compareTo)
            .orElse(null));
  }

  private ClassSession editProposal(ClassSession s, SessionDto.Edit r) {
    var n = copy(s);
    n.setSessionDate(r.sessionDate());
    n.setStartTime(r.startTime());
    n.setEndTime(r.endTime());
    n.setRoomId(r.roomId());
    n.setTeacherId(r.teacherId());
    n.setCustomized(true);
    n.setReason(reason(r.reason()));
    return n;
  }

  public SessionDto edit(Long id, SessionDto.Edit r) {
    manage();
    var s = lockSession(id);
    version(s, r.version());
    editable(s);
    openClass(s.getSchoolClass());
    if (s.getStatus() != ClassSession.Status.SCHEDULED)
      throw ApiException.conflict("Khôi phục buổi đã hủy trước khi chỉnh sửa");
    var n = editProposal(s, r);
    var existing = repository.findBySchoolClassIdOrderBySessionDateAsc(s.getSchoolClass().getId());
    validate(s.getSchoolClass(), List.of(n), existing, true);
    var before = snapshot(s);
    apply(s, n);
    repository.saveAndFlush(s);
    record(s, "UPDATED", before, s.getReason());
    endDate(s.getSchoolClass());
    return dto(s);
  }

  public SessionDto cancel(Long id, SessionDto.Action r) {
    manage();
    var s = lockSession(id);
    version(s, r.version());
    editable(s);
    if (s.getStatus() == ClassSession.Status.CANCELLED) return dto(s);
    var n = copy(s);
    n.setStatus(ClassSession.Status.CANCELLED);
    n.setCustomized(true);
    n.setReason(reason(r.reason()));
    validate(s.getSchoolClass(), List.of(n), List.of(s), true);
    var before = snapshot(s);
    apply(s, n);
    repository.saveAndFlush(s);
    record(s, "CANCELLED", before, s.getReason());
    endDate(s.getSchoolClass());
    return dto(s);
  }

  public SessionDto restore(Long id, SessionDto.Action r) {
    manage();
    var s = lockSession(id);
    version(s, r.version());
    editable(s);
    openClass(s.getSchoolClass());
    if (s.getStatus() != ClassSession.Status.CANCELLED)
      throw ApiException.conflict("Chỉ khôi phục buổi đã hủy");
    if (s.getMakeupForId() != null
        && repository.findById(s.getMakeupForId()).orElseThrow(ApiException::missing).getStatus()
            != ClassSession.Status.CANCELLED)
      throw ApiException.conflict("Buổi gốc đang hiệu lực; không thể khôi phục thêm buổi bù");
    if (repository
        .findByMakeupForId(id)
        .filter(m -> m.getStatus() != ClassSession.Status.CANCELLED)
        .isPresent()) throw ApiException.conflict("Buổi này đã có lịch học bù đang hiệu lực");
    var n = copy(s);
    n.setStatus(ClassSession.Status.SCHEDULED);
    n.setCustomized(true);
    n.setReason(reason(r.reason()));
    validate(
        s.getSchoolClass(),
        List.of(n),
        repository.findBySchoolClassIdOrderBySessionDateAsc(s.getSchoolClass().getId()),
        true);
    var before = snapshot(s);
    apply(s, n);
    repository.saveAndFlush(s);
    record(s, "RESTORED", before, s.getReason());
    endDate(s.getSchoolClass());
    return dto(s);
  }

  public SessionDto makeup(Long id, SessionDto.Edit r) {
    manage();
    var source = lockSession(id);
    version(source, r.version());
    openClass(source.getSchoolClass());
    if (source.getStatus() != ClassSession.Status.CANCELLED)
      throw ApiException.conflict("Hủy buổi gốc trước khi tạo buổi học bù");
    if (source.getKind() == ClassSession.Kind.MAKEUP)
      throw ApiException.conflict("Khôi phục và sửa buổi bù đã hủy; không tạo chuỗi học bù");
    if (r.sessionDate().equals(source.getSessionDate()))
      throw ApiException.bad("Buổi học bù phải khác ngày buổi gốc");
    if (repository.findByMakeupForId(id).isPresent())
      throw ApiException.conflict("Buổi gốc đã có buổi học bù; sửa hoặc khôi phục buổi học bù đó");
    var n = editProposal(source, r);
    n.setId(null);
    n.setVersion(0);
    n.setStatus(ClassSession.Status.SCHEDULED);
    n.setKind(ClassSession.Kind.MAKEUP);
    n.setSourceDate(null);
    n.setSourceScheduleId(null);
    n.setMakeupForId(id);
    n.setMakeupSessionId(null);
    validate(
        source.getSchoolClass(),
        List.of(n),
        repository.findBySchoolClassIdOrderBySessionDateAsc(source.getSchoolClass().getId()),
        true);
    repository.saveAndFlush(n);
    record(n, "MAKEUP_CREATED", null, n.getReason());
    var before = snapshot(source);
    source.setMakeupSessionId(n.getId());
    repository.saveAndFlush(source);
    record(source, "MAKEUP_LINKED", before, "Học bù bằng buổi #" + n.getId());
    endDate(source.getSchoolClass());
    return dto(n);
  }

  public SessionDto correctTime(Long id,SessionDto.TimeCorrection r){manage();var s=lockSession(id);version(s,r.version());if(s.getStatus()!=ClassSession.Status.COMPLETED)throw ApiException.conflict("Chỉ đối soát giờ buổi đã hoàn tất");if(com.example.academic.duration.DurationProgressService.minutes(r.startTime(),r.endTime())==null)throw ApiException.bad("Giờ học phải cùng ngày và chính xác đến phút");var before=snapshot(s);s.setStartTime(r.startTime());s.setEndTime(r.endTime());s.setReason(reason(r.reason()));s.setCustomized(true);repository.saveAndFlush(s);record(s,"TIME_CORRECTED",before,r.reason().trim());return dto(s);}
  public void touch(ClassSession s) {
    repository.saveAndFlush(s);
  }

  public void complete(ClassSession s) {
    if (s.getStatus() == ClassSession.Status.SCHEDULED) {
      var before = snapshot(s);
      s.setStatus(ClassSession.Status.COMPLETED);
      repository.saveAndFlush(s);
      record(s, "COMPLETED", before, "Đã lưu điểm danh");
    }
  }

  public void deleteDraft(SchoolClass c) {
    var all = repository.findBySchoolClassIdOrderBySessionDateAsc(c.getId());
    for (var s : all)
      if (s.isCustomized() || s.getStatus() == ClassSession.Status.COMPLETED || hasAttendance(s))
        throw ApiException.conflict("Lớp có lịch sử buổi học; không thể xóa");
    for (var s : all) events.deleteAll(events.findBySessionIdOrderByIdDesc(s.getId()));
    events.flush();
    repository.deleteAll(all);
    repository.flush();
  }

  public void checkEnrollment(Long studentId, SchoolClass target, Long excluded) {
    enrollmentConflict(studentId, target, excluded)
        .ifPresent(
            message -> {
              throw ApiException.conflict(message);
            });
  }

  public java.util.Optional<String> enrollmentConflict(
      Long studentId, SchoolClass target, Long excluded) {
    var candidate =
        repository.findBySchoolClassIdOrderBySessionDateAsc(target.getId()).stream()
            .filter(
                s ->
                    s.getStatus() != ClassSession.Status.CANCELLED
                        && !s.getSessionDate().isBefore(today()))
            .toList();
    for (var e :
        enrollments.findByStudentIdAndStudyStatus(studentId, Enrollment.StudyStatus.STUDYING)) {
      if (Objects.equals(e.getId(), excluded)
          || Objects.equals(e.getSchoolClass().getId(), target.getId())) continue;
      for (var other :
          repository.findBySchoolClassIdOrderBySessionDateAsc(e.getSchoolClass().getId()))
        if (other.getStatus() != ClassSession.Status.CANCELLED)
          for (var s : candidate)
            if (overlap(s, other))
              return java.util.Optional.of(
                  "Trùng buổi học thực tế với lớp " + e.getSchoolClass().getCode());
    }
    return java.util.Optional.empty();
  }

  @Transactional(readOnly = true)
  public List<ScheduleDto.Conflict> previewWeekly(
      SchoolClass c, DayOfWeek day, LocalTime start, LocalTime end, Long excluded) {
    if (com.example.academic.duration.DurationProgressService.minutes(start,end)==null) throw ApiException.bad("Giờ học phải cùng ngày, kết thúc sau bắt đầu và chính xác đến phút");
    range(c.getStartDate(), c.getEndDate());
    if (c.getId() != null
        && schedules.findBySchoolClassId(c.getId()).stream()
            .anyMatch(
                slot -> slot.getDayOfWeek() == day && !Objects.equals(slot.getId(), excluded)))
      return List.of(
          new ScheduleDto.Conflict(
              null,
              c.getId(),
              c.getCode(),
              c.getName(),
              "Mỗi lớp tối đa một lịch mẫu cho mỗi thứ",
              day,
              start,
              end));
    var proposals = new ArrayList<ClassSession>();
    var existing =
        c.getId() == null
            ? List.<ClassSession>of()
            : repository.findBySchoolClassIdOrderBySessionDateAsc(c.getId());
    for (var d =
            c.getId() == null
                ? c.getStartDate()
                : (c.getStartDate().isAfter(today()) ? c.getStartDate() : today().plusDays(1));
        !d.isAfter(c.getEndDate());
        d = d.plusDays(1))
      if (d.getDayOfWeek() == day) {
        var date = d;
        var old =
            existing.stream().filter(s -> date.equals(s.getSourceDate())).findFirst().orElse(null);
        if (old != null && (old.isCustomized() || old.getStatus() == ClassSession.Status.COMPLETED))
          continue;
        var slot = new Schedule();
        slot.setDayOfWeek(day);
        slot.setStartTime(start);
        slot.setEndTime(end);
        var n = planned(c, slot, d);
        if (old != null) n.setId(old.getId());
        proposals.add(n);
      }
    try {
      validate(c, proposals, existing, false);
      return List.of();
    } catch (ApiException e) {
      if (e.getStatus() != org.springframework.http.HttpStatus.CONFLICT) throw e;
      return List.of(
          new ScheduleDto.Conflict(
              null, c.getId(), c.getCode(), c.getName(), e.getMessage(), day, start, end));
    }
  }
}
