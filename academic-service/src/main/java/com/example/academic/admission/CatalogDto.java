package com.example.academic.admission;

import jakarta.validation.constraints.*;

public final class CatalogDto {
  public record ProgramDto(Long id, long version, String code, String name, boolean active) {}

  public record LevelDto(
      Long id, Long programId, long version, String code, String name, int rank, boolean active) {}

  public record ProgramRequest(
      @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,49}") String code,
      @NotBlank @Size(max = 255) String name,
      @NotNull Boolean active,
      @PositiveOrZero Long version) {}

  public record LevelRequest(
      @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,49}") String code,
      @NotBlank @Size(max = 255) String name,
      @NotNull @PositiveOrZero Integer rank,
      @NotNull Boolean active,
      @PositiveOrZero Long version) {}
}
