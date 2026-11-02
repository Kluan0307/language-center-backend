package com.example.teaching.room;

import com.example.teaching.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
    boolean existsByCodeIgnoreCaseAndBranch_IdAndIdNot(String code, Long branchId, Long id);
}
