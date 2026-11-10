package com.example.academic.results;

import com.example.academic.schoolclass.SchoolClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ResultSourceController {
  private final SchoolClassService classes;
  private final ResultBarrierService barrier;

  public record Source(String token, long sourceRevision) {}

  @GetMapping("/api/v1/academic/classes/{id}/result-source")
  @Transactional(readOnly = true)
  public Source get(@PathVariable Long id) {
    classes.require(id);
    var b = barrier.read(id);
    if (b == null)
      throw com.example.academic.common.ApiException.conflict("Chưa khởi tạo điều phối kết quả");
    return new Source(b.getToken(), b.getSourceRevision());
  }
}
