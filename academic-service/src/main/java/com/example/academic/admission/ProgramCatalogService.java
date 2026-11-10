package com.example.academic.admission;

import com.example.academic.common.*;
import com.example.academic.security.*;
import jakarta.persistence.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ProgramCatalogService {
  private final ProgramRepository programs;
  private final ProgramLevelRepository levels;
  private final EntityManager em;

  private void superOnly() {
    if (Users.current().role() != Role.SUPER_ADMIN) throw ApiException.forbidden();
  }

  public Program require(Long id) {
    if (id == null) throw ApiException.bad("Chọn chương trình");
    return programs.findById(id).orElseThrow(ApiException::missing);
  }

  public Program lock(Long id) {
    if (id == null) throw ApiException.bad("Chọn chương trình");
    var p = programs.lock(id).orElseThrow(ApiException::missing);
    em.refresh(p, LockModeType.PESSIMISTIC_WRITE);
    return p;
  }

  public void active(Program p) {
    if (!p.isActive()) throw ApiException.bad("Chương trình đã ngừng dùng cho cấu hình mới");
  }

  public ProgramLevel level(Long id, Long programId, boolean requireActive) {
    if (id == null) throw ApiException.bad("Chọn trình độ");
    var l = levels.findById(id).orElseThrow(ApiException::missing);
    if (!l.getProgram().getId().equals(programId))
      throw ApiException.bad("Trình độ phải thuộc cùng chương trình");
    if (requireActive && !l.isActive()) throw ApiException.bad("Trình độ đã ngừng dùng");
    return l;
  }

  static void version(Long expected, long actual) {
    if (expected == null) throw ApiException.bad("Thiếu phiên bản đã đọc");
    if (expected != actual) throw ApiException.conflict("Dữ liệu đã thay đổi; hãy tải lại");
  }

  private String code(String value) {
    return value.trim().toUpperCase(Locale.ROOT);
  }

  @Transactional(readOnly = true)
  public List<CatalogDto.ProgramDto> list() {
    return programs.findAllByOrderByCodeAsc().stream().map(this::dto).toList();
  }

  @Transactional(readOnly = true)
  public List<CatalogDto.LevelDto> levels(Long id) {
    require(id);
    return levels.findByProgramIdOrderByRankAscIdAsc(id).stream().map(this::dto).toList();
  }

  public CatalogDto.ProgramDto save(Long id, CatalogDto.ProgramRequest r) {
    superOnly();
    var p = id == null ? new Program() : lock(id);
    if (id != null) {
      version(r.version(), p.getVersion());
      if (!p.getCode().equals(code(r.code())))
        throw ApiException.bad(
            "Mã chương trình không đổi; dùng tên hiển thị hoặc tạo chương trình mới");
    } else {
      if (programs.existsByCode(code(r.code())))
        throw ApiException.conflict("Mã chương trình đã tồn tại");
      p.setCode(code(r.code()));
    }
    p.setName(r.name().trim());
    p.setActive(r.active());
    return dto(programs.saveAndFlush(p));
  }

  public CatalogDto.LevelDto saveLevel(Long programId, Long id, CatalogDto.LevelRequest r) {
    superOnly();
    ProgramLevel l =
        id == null ? new ProgramLevel() : levels.findById(id).orElseThrow(ApiException::missing);
    Long parent = id == null ? programId : l.getProgram().getId();
    var p = lock(parent);
    if (id == null) {
      active(p);
      if (levels.existsByProgramIdAndCode(parent, code(r.code())))
        throw ApiException.conflict("Mã trình độ đã tồn tại trong chương trình");
      l.setProgram(p);
      l.setCode(code(r.code()));
    } else {
      em.refresh(l, LockModeType.PESSIMISTIC_WRITE);
      version(r.version(), l.getVersion());
      if (!l.getCode().equals(code(r.code()))) throw ApiException.bad("Mã trình độ không đổi");
    }
    l.setName(r.name().trim());
    l.setRank(r.rank());
    l.setActive(r.active());
    return dto(levels.saveAndFlush(l));
  }

  private CatalogDto.ProgramDto dto(Program p) {
    return new CatalogDto.ProgramDto(
        p.getId(), p.getVersion(), p.getCode(), p.getName(), p.isActive());
  }

  private CatalogDto.LevelDto dto(ProgramLevel l) {
    return new CatalogDto.LevelDto(
        l.getId(),
        l.getProgram().getId(),
        l.getVersion(),
        l.getCode(),
        l.getName(),
        l.getRank(),
        l.isActive());
  }
}
