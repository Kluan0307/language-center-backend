package com.example.teaching.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RoomRequest(
        @NotBlank(message = "Mã phòng không được để trống")
        @Size(max = 50, message = "Mã phòng tối đa 50 ký tự")
        String code,

        @NotBlank(message = "Tên phòng không được để trống")
        @Size(max = 100, message = "Tên phòng tối đa 100 ký tự")
        String name,

        @Size(max = 20, message = "Tầng tối đa 20 ký tự")
        String floor,

        @NotNull(message = "Sức chứa không được để trống")
        @Positive(message = "Sức chứa phải lớn hơn 0")
        Integer capacity,

        @Size(max = 50, message = "Trạng thái tối đa 50 ký tự")
        String status,

        @NotNull(message = "branchId không được để trống")
        Long branchId
) {
}
