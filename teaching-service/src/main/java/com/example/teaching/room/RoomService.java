package com.example.teaching.room;

import com.example.teaching.entity.Branch;
import com.example.teaching.entity.Room;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    public List<RoomResponse> getAll() {
        return roomRepository.findAll().stream()
                .map(RoomResponse::fromEntity)
                .toList();
    }

    public RoomResponse getById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phòng với id=" + id));
        return RoomResponse.fromEntity(room);
    }

    @Transactional
    public RoomResponse create(RoomRequest request) {
        if (roomRepository.existsByCodeIgnoreCaseAndBranch_IdAndIdNot(request.code().trim(), request.branchId(), -1L)) {
            throw new IllegalArgumentException("Mã phòng đã tồn tại trong chi nhánh này");
        }

        Room room = new Room();
        room.setCode(request.code().trim());
        room.setName(request.name().trim());
        room.setFloor(request.floor());
        room.setCapacity(request.capacity());
        room.setStatus(request.status() == null || request.status().isBlank() ? "ACTIVE" : request.status());

        Branch branch = new Branch();
        branch.setId(request.branchId());
        room.setBranch(branch);

        return RoomResponse.fromEntity(roomRepository.save(room));
    }

    @Transactional
    public RoomResponse update(Long id, RoomRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phòng với id=" + id));

        if (roomRepository.existsByCodeIgnoreCaseAndBranch_IdAndIdNot(request.code().trim(), request.branchId(), id)) {
            throw new IllegalArgumentException("Mã phòng đã tồn tại trong chi nhánh này");
        }

        room.setCode(request.code().trim());
        room.setName(request.name().trim());
        room.setFloor(request.floor());
        room.setCapacity(request.capacity());
        room.setStatus(request.status() == null || request.status().isBlank() ? room.getStatus() : request.status());

        Branch branch = new Branch();
        branch.setId(request.branchId());
        room.setBranch(branch);

        return RoomResponse.fromEntity(roomRepository.save(room));
    }

    @Transactional
    public void delete(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phòng với id=" + id));
        roomRepository.delete(room);
    }
}
