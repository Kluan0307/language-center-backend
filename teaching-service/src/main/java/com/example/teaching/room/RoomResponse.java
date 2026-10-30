package com.example.teaching.room;

import com.example.teaching.entity.Room;

public record RoomResponse(
        Long id,
        String code,
        String name,
        String floor,
        Integer capacity,
        String status,
        Long branchId
) {
    public static RoomResponse fromEntity(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getCode(),
                room.getName(),
                room.getFloor(),
                room.getCapacity(),
                room.getStatus(),
                room.getBranch() != null ? room.getBranch().getId() : null
        );
    }
}
