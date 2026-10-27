package com.example.academic.room;

import org.springframework.data.jpa.repository.*;

public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {
  boolean existsByCodeIgnoreCaseAndBranchIdAndIdNot(String code, Long branchId, Long id);
}
