package com.example.academic.room;

import com.example.academic.branch.BranchService;
import com.example.academic.common.*;
import com.example.academic.schoolclass.SchoolClassRepository;
import com.example.academic.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class RoomService {
  private final RoomRepository repository;
  private final BranchScope scope;
  private final BranchService branches;
  private final SchoolClassRepository classes;

  public Room require(Long id) {
    Room r = repository.findById(id).orElseThrow(ApiException::missing);
    scope.check(r.getBranchId());
    return r;
  }

  @Transactional(readOnly = true)
  public PageResponse<RoomDto> list(
      int page, int size, String keyword, Room.Status status, Long branchId) {
    return PageResponse.of(
        repository
            .findAll(
                Query.<Room>branch(scope.filter(branchId))
                    .and(Query.keyword(keyword, "code", "name"))
                    .and(Query.eq("status", status)),
                Query.page(page, size))
            .map(RoomDto::of));
  }

  @Transactional(readOnly = true)
  public RoomDto get(Long id) {
    return RoomDto.of(require(id));
  }

  public RoomDto save(Long id, RoomDto.Request req) {
    Room r = id == null ? new Room() : require(id);
    Long branch = id == null ? scope.create(req.branchId()) : r.getBranchId();
    branches.lock(branch);
    if (repository.existsByCodeIgnoreCaseAndBranchIdAndIdNot(
        req.code().trim(), branch, id == null ? -1L : id))
      throw ApiException.conflict("Mã phòng đã tồn tại");
    if (id != null && classes.existsByRoomIdAndMaxSizeGreaterThan(id, req.capacity()))
      throw ApiException.bad("Sức chứa phòng nhỏ hơn sĩ số tối đa của lớp đã gán");
    r.setBranchId(branch);
    r.setCode(req.code().trim());
    r.setName(req.name().trim());
    r.setLocation(req.location());
    r.setCapacity(req.capacity());
    r.setEquipment(req.equipment());
    return RoomDto.of(repository.save(r));
  }

  public RoomDto status(Long id, Room.Status status) {
    Room r = require(id);
    r.setStatus(status);
    return RoomDto.of(r);
  }

  public void delete(Long id) {
    Room r = require(id);
    branches.lock(r.getBranchId());
    if (classes.existsByRoomId(id)) throw ApiException.conflict("Không thể xóa phòng đã gán lớp");
    repository.delete(r);
    repository.flush();
  }
}
