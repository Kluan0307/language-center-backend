package com.example.academic.branch;

import com.example.academic.common.*;
import com.example.academic.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BranchService {
  private final BranchRepository repository;
  private final BranchScope scope;

  public Branch require(Long id) {
    return repository.findById(id).orElseThrow(ApiException::missing);
  }

  public void lock(Long id) {
    repository.lock(id).orElseThrow(ApiException::missing);
  }

  @Transactional(readOnly = true)
  public PageResponse<BranchDto> list(int page, int size, String keyword, Long branchId) {
    Long id = scope.filter(branchId);
    return PageResponse.of(
        repository
            .findAll(
                Query.<Branch>eq("id", id).and(Query.keyword(keyword, "name", "code")),
                Query.page(page, size))
            .map(BranchDto::of));
  }

  @Transactional(readOnly = true)
  public BranchDto get(Long id) {
    scope.check(id);
    return BranchDto.of(require(id));
  }

  public BranchDto save(Long id, BranchDto.Request req) {
    if (repository.existsByCodeIgnoreCaseAndIdNot(req.code().trim(), id == null ? -1L : id))
      throw ApiException.conflict("Mã chi nhánh đã tồn tại");
    Branch b = id == null ? new Branch() : require(id);
    b.setCode(req.code().trim());
    b.setName(req.name().trim());
    b.setAddress(req.address());
    b.setPhone(req.phone().trim());
    return BranchDto.of(repository.save(b));
  }

  public BranchDto toggle(Long id) {
    Branch b = require(id);
    b.setStatus(
        b.getStatus() == Branch.Status.ACTIVE ? Branch.Status.INACTIVE : Branch.Status.ACTIVE);
    return BranchDto.of(b);
  }
}
