package com.example.academic.room;

import jakarta.validation.constraints.*;

public record RoomDto(
    Long id,
    String code,
    String name,
    String location,
    Integer capacity,
    String equipment,
    Room.Status status,
    Long branchId) {
  public static RoomDto of(Room r) {
    return new RoomDto(
        r.getId(),
        r.getCode(),
        r.getName(),
        r.getLocation(),
        r.getCapacity(),
        r.getEquipment(),
        r.getStatus(),
        r.getBranchId());
  }

  public record Request(
      @NotBlank @Size(max = 255) String code,
      @NotBlank @Size(max = 255) String name,
      @Size(max = 255) String location,
      @NotNull @Positive Integer capacity,
      String equipment,
      Long branchId) {}

  public record StatusRequest(@NotNull Room.Status status) {}
}
