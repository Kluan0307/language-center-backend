package com.example.finance.tuition;

import com.example.finance.common.*;
import com.example.finance.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TuitionService {
  private final TuitionRepository repository;
  private final BranchScope scope;

  public Tuition require(Long id) {
    Tuition t = repository.findById(id).orElseThrow(ApiException::missing);
    scope.finance(t.getBranchId());
    return t;
  }

  public Specification<Tuition> visible(Long requested) {
    Long branch = scope.filter(requested);
    return (r, q, b) ->
        branch == null ? b.conjunction() : b.or(b.isNull(r.get("branchId")), b.equal(r.get("branchId"), branch));
  }

  public PageResponse<TuitionDto> list(
      int page, int size, String keyword, Tuition.Status status, Long branchId) {
    return PageResponse.of(
        repository
            .findAll(
                visible(branchId)
                    .and(Query.keyword(keyword, "currency"))
                    .and(Query.eq("status", status)),
                Query.page(page, size))
            .map(TuitionDto::of));
  }

  public TuitionDto get(Long id) {
    return TuitionDto.of(require(id));
  }

  @Transactional
  public TuitionDto save(Long id, TuitionDto.Request r) {
    Tuition t = id == null ? new Tuition() : require(id);
    Long branch = id == null ? scope.create(r.branchId()) : t.getBranchId();
    t.setStudentId(r.studentId());
    t.setClassId(r.classId());
    t.setAmount(r.amount());
    t.setCurrency(r.currency().trim().toUpperCase());
    t.setDueDate(r.dueDate());
    t.setBranchId(branch);
    if (id != null) {
      t.setStatus(t.getStatus());
    }
    return TuitionDto.of(repository.save(t));
  }

  @Transactional
  public TuitionDto status(Long id, Tuition.Status status) {
    Tuition t = require(id);
    t.setStatus(status);
    return TuitionDto.of(repository.save(t));
  }
}
