package com.example.academic.schedule;

import com.example.academic.branch.BranchService;
import com.example.academic.common.*;
import com.example.academic.room.RoomService;
import com.example.academic.schoolclass.*;
import com.example.academic.security.*;
import java.time.DayOfWeek;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ScheduleService {
  private final ScheduleRepository repository;
  private final SchoolClassService classes;
  private final ScheduleConflictService conflicts;
  private final BranchService branches;
  private final BranchScope scope;
  private final RoomService rooms;

  private Schedule require(Long id) {
    Schedule s = repository.findById(id).orElseThrow(ApiException::missing);
    classes.require(s.getSchoolClass().getId());
    return s;
  }

  @Transactional(readOnly = true)
  public PageResponse<ScheduleDto> list(
      int page,
      int size,
      Long classId,
      DayOfWeek dayOfWeek,
      Long teacherId,
      Long roomId,
      Long branchId) {
    Long branch = scope.filter(branchId);
    if (classId != null) classes.require(classId);
    var u = Users.current();
    if (u.role() == Role.TEACHER) {
      if (u.profileId() == null) throw ApiException.forbidden();
      teacherId = u.profileId();
    }
    return PageResponse.of(
        repository
            .findAll(
                Query.<Schedule>eq("schoolClass.branchId", branch)
                    .and(Query.eq("schoolClass.id", classId))
                    .and(Query.eq("dayOfWeek", dayOfWeek))
                    .and(Query.eq("schoolClass.teacherId", teacherId))
                    .and(Query.eq("schoolClass.room.id", roomId)),
                Query.page(page, size))
            .map(ScheduleDto::of));
  }

  public ScheduleDto save(Long id, ScheduleDto.Request r) {
    Schedule s = id == null ? new Schedule() : require(id);
    SchoolClass c = classes.require(r.classId());
    if (id != null && !s.getSchoolClass().getId().equals(c.getId()))
      throw ApiException.bad("Không thể đổi lớp của lịch học");
    branches.lock(c.getBranchId());
    c = classes.lock(c.getId());
    conflicts.enforce(c, r.dayOfWeek(), r.startTime(), r.endTime(), id, false);
    s.setSchoolClass(c);
    s.setDayOfWeek(r.dayOfWeek());
    s.setStartTime(r.startTime());
    s.setEndTime(r.endTime());
    return ScheduleDto.of(repository.save(s));
  }

  public void delete(Long id) {
    Schedule s = require(id);
    branches.lock(s.getSchoolClass().getBranchId());
    repository.delete(s);
  }

  @Transactional(readOnly = true)
  public List<ScheduleDto.Conflict> preview(ScheduleDto.ConflictRequest r) {
    SchoolClass c;
    if (r.classId() != null) c = classes.require(r.classId());
    else {
      if (r.teacherId() == null
          || r.teacherId() <= 0
          || r.roomId() == null
          || r.startDate() == null
          || r.endDate() == null)
        throw ApiException.bad("Lớp mới cần teacherId, roomId, startDate, endDate");
      c = new SchoolClass();
      c.setBranchId(scope.create(r.branchId()));
      branches.require(c.getBranchId());
      c.setTeacherId(r.teacherId());
      c.setRoom(rooms.require(r.roomId()));
      c.setStartDate(r.startDate());
      c.setEndDate(r.endDate());
      if (!c.getRoom().getBranchId().equals(c.getBranchId())) throw ApiException.missing();
    }
    if (c.getEndDate().isBefore(c.getStartDate()))
      throw ApiException.bad("endDate không được trước startDate");
    if (r.excludeScheduleId() != null) {
      Schedule s = require(r.excludeScheduleId());
      if (!Objects.equals(s.getSchoolClass().getId(), c.getId()))
        throw ApiException.bad("Lịch loại trừ không thuộc lớp");
    }
    return conflicts.conflicts(
        c, r.dayOfWeek(), r.startTime(), r.endTime(), r.excludeScheduleId(), false);
  }
}
