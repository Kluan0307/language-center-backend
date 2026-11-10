package com.example.academic.admission;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/academic")
public class ProgramCatalogController {
  private final ProgramCatalogService service;

  @GetMapping("/programs")
  public List<CatalogDto.ProgramDto> list() {
    return service.list();
  }

  @PostMapping("/programs")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public CatalogDto.ProgramDto create(@Valid @RequestBody CatalogDto.ProgramRequest r) {
    return service.save(null, r);
  }

  @PutMapping("/programs/{id}")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public CatalogDto.ProgramDto update(
      @PathVariable Long id, @Valid @RequestBody CatalogDto.ProgramRequest r) {
    return service.save(id, r);
  }

  @GetMapping("/programs/{id}/levels")
  public List<CatalogDto.LevelDto> levels(@PathVariable Long id) {
    return service.levels(id);
  }

  @PostMapping("/programs/{id}/levels")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public CatalogDto.LevelDto createLevel(
      @PathVariable Long id, @Valid @RequestBody CatalogDto.LevelRequest r) {
    return service.saveLevel(id, null, r);
  }

  @PutMapping("/program-levels/{id}")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public CatalogDto.LevelDto updateLevel(
      @PathVariable Long id, @Valid @RequestBody CatalogDto.LevelRequest r) {
    return service.saveLevel(null, id, r);
  }
}
