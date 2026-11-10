package com.example.teaching.document;

import com.example.teaching.common.*;
import com.example.teaching.integration.AcademicDirectory;
import com.example.teaching.security.*;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService {
  private final DocumentRepository repository;
  private final DocumentStorage storage;
  private final AcademicDirectory academic;
  private final BranchScope scope;
  private final DocumentEventRepository events;
  private final jakarta.persistence.EntityManager em;

  @Transactional(readOnly = true)
  public Document require(Long id) {
    Document d = repository.findById(id).orElseThrow(ApiException::missing);
    scope.check(d.getBranchId());
    var u = Users.current();
    if (u.role() == Role.STAFF && d.getStatus() != Document.Status.APPROVED)
      throw ApiException.missing();
    if (u.role() == Role.TEACHER && !Objects.equals(u.accountId(), d.getUploadedBy()))
      throw ApiException.missing();
    return d;
  }

  @Transactional(readOnly = true)
  public PageResponse<DocumentDto> list(
      int page, int size, Long branchId, Long classId, Long courseId, Document.Status status) {
    var u = Users.current();
    var query =
        Query.<Document>branch(scope.filter(branchId))
            .and(Query.eq("classId", classId))
            .and(Query.eq("courseId", courseId));
    if (u.role() == Role.STAFF) query = query.and(Query.eq("status", Document.Status.APPROVED));
    else if (u.role() == Role.TEACHER) query = query.and(Query.eq("uploadedBy", u.accountId()));
    query = query.and(Query.eq("status", status));
    return PageResponse.of(
        repository
            .findAll(
                query,
                org.springframework.data.domain.PageRequest.of(
                    Query.page(page, size).getPageNumber(),
                    size,
                    org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC,
                        "lastSubmittedAt",
                        "id")))
            .map(DocumentDto::of));
  }

  public DocumentDto upload(
      String title,
      String description,
      Long classId,
      Long courseId,
      Long branchId,
      MultipartFile file) {
    var u = Users.current();
    if (u.role() == Role.STAFF) throw ApiException.forbidden();
    if (title == null
        || title.isBlank()
        || title.length() > 255
        || description != null && description.length() > 1000)
      throw ApiException.bad("Invalid document metadata");
    if ((classId == null) == (courseId == null))
      throw ApiException.bad("Choose exactly one class or course");
    Long branch =
        classId != null ? academic.schoolClass(classId).branchId() : scope.create(branchId);
    if (courseId != null) academic.course(courseId, branch);
    String key = storage.save(file);
    if (TransactionSynchronizationManager.isSynchronizationActive())
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
              if (status != STATUS_COMMITTED) storage.delete(key);
            }
          });
    try {
      Document d = new Document();
      d.setBranchId(branch);
      d.setClassId(classId);
      d.setCourseId(courseId);
      d.setTitle(title.trim());
      d.setDescription(description);
      d.setStorageKey(key);
      d.setOriginalFilename(storage.filename(file));
      d.setFileSize(file.getSize());
      d.setUploadedBy(u.accountId());
      d.setUploadedByName(u.username());
      d.setLastSubmittedAt(d.getUploadedAt());
      repository.saveAndFlush(d);
      record(d, "UPLOADED", null);
      return DocumentDto.of(d);
    } catch (RuntimeException ex) {
      storage.delete(key);
      throw ex;
    }
  }

  public DocumentDto review(Long id, DocumentDto.Review r) {
    var u = Users.current();
    if (u.role() != Role.ADMIN && u.role() != Role.SUPER_ADMIN) throw ApiException.forbidden();
    Document d = repository.lock(id).orElseThrow(ApiException::missing);
    scope.check(d.getBranchId());
    em.refresh(d, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    version(d, r.version());
    if (d.getStatus() != Document.Status.PENDING)
      throw ApiException.conflict("Document has already been reviewed");
    if (r.status() == Document.Status.PENDING)
      throw ApiException.bad("Choose APPROVED or REJECTED");
    if (r.status() == Document.Status.REJECTED && (r.reason() == null || r.reason().isBlank()))
      throw ApiException.bad("Rejection requires a reason");
    baseline(d);
    d.setStatus(r.status());
    d.setReviewedBy(u.accountId());
    d.setReviewedByName(u.username());
    d.setReviewedAt(LocalDateTime.now(com.example.teaching.common.BusinessTime.ZONE));
    d.setRejectReason(r.status() == Document.Status.REJECTED ? r.reason().trim() : null);
    record(d, "REVIEWED", r.reason());
    repository.flush();
    return DocumentDto.of(d);
  }

  private void version(Document d, Long expected) {
    if (expected == null || !Objects.equals(expected, d.getVersion()))
      throw ApiException.conflict("Phiên bản tài liệu đã thay đổi; hãy tải lại và kiểm tra file");
  }

  private DocumentEvent snapshot(Document d, String action, String reason) {
    var e = new DocumentEvent();
    e.setDocumentId(d.getId());
    e.setRevision(d.getRevision());
    e.setAction(action);
    e.setStatus(d.getStatus().name());
    e.setStorageKey(d.getStorageKey());
    e.setTitle(d.getTitle());
    e.setDescription(d.getDescription());
    e.setFilename(d.getOriginalFilename());
    e.setFileSize(d.getFileSize());
    e.setReason(reason);
    e.setActorId(Users.current().accountId());
    e.setActorName(Users.current().username());
    return e;
  }

  private void record(Document d, String action, String reason) {
    events.save(snapshot(d, action, reason));
  }

  private void baseline(Document d) {
    if (!events.existsByDocumentId(d.getId())) {
      var e = snapshot(d, "LEGACY_SNAPSHOT", d.getRejectReason());
      e.setActorId(d.getReviewedBy() == null ? d.getUploadedBy() : d.getReviewedBy());
      e.setActorName(d.getReviewedByName() == null ? d.getUploadedByName() : d.getReviewedByName());
      e.setOccurredAt(d.getReviewedAt() == null ? d.getUploadedAt() : d.getReviewedAt());
      events.save(e);
    }
  }

  public record Event(
      Long id,
      int revision,
      String action,
      String status,
      String title,
      String description,
      String filename,
      Long fileSize,
      String reason,
      String actorName,
      LocalDateTime occurredAt) {
    static Event of(DocumentEvent e) {
      return new Event(
          e.getId(),
          e.getRevision(),
          e.getAction(),
          e.getStatus(),
          e.getTitle(),
          e.getDescription(),
          e.getFilename(),
          e.getFileSize(),
          e.getReason(),
          e.getActorName(),
          e.getOccurredAt());
    }
  }

  private Document historyAccess(Long id) {
    var d = require(id);
    if (Users.current().role() == Role.STAFF) throw ApiException.forbidden();
    return d;
  }

  @Transactional(readOnly = true)
  public java.util.List<Event> history(Long id) {
    historyAccess(id);
    return events.findByDocumentIdOrderByIdDesc(id).stream().map(Event::of).toList();
  }

  @Transactional(readOnly = true)
  public DocumentEvent revision(Long id, int revision) {
    historyAccess(id);
    return events
        .findFirstByDocumentIdAndRevisionOrderByIdDesc(id, revision)
        .orElseThrow(ApiException::missing);
  }

  public DocumentDto resubmit(
      Long id, String title, String description, String reason, Long version, MultipartFile file) {
    var initial = require(id);
    if (!Objects.equals(initial.getUploadedBy(), Users.current().accountId())
        || Users.current().role() == Role.STAFF) throw ApiException.forbidden();
    var d = repository.lock(id).orElseThrow(ApiException::missing);
    em.refresh(d, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    scope.check(d.getBranchId());
    version(d, version);
    if (d.getStatus() != Document.Status.REJECTED)
      throw ApiException.conflict("Chỉ gửi lại tài liệu đã bị từ chối");
    if (title == null
        || title.isBlank()
        || title.length() > 255
        || description != null && description.length() > 1000
        || reason == null
        || reason.trim().length() < 3
        || reason.trim().length() > 500)
      throw ApiException.bad("Kiểm tra tiêu đề, mô tả và lý do gửi lại (3–500 ký tự)");
    if (d.getClassId() != null) academic.schoolClass(d.getClassId());
    else academic.course(d.getCourseId(), d.getBranchId());
    baseline(d);
    String key = file == null ? null : storage.save(file);
    if (key != null && TransactionSynchronizationManager.isSynchronizationActive())
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
              if (status != STATUS_COMMITTED) storage.delete(key);
            }
          });
    try {
      if (key != null) {
        d.setStorageKey(key);
        d.setOriginalFilename(storage.filename(file));
        d.setFileSize(file.getSize());
      }
      d.setTitle(title.trim());
      d.setDescription(description);
      d.setRevision(d.getRevision() + 1);
      d.setLastSubmittedAt(LocalDateTime.now(com.example.teaching.common.BusinessTime.ZONE));
      d.setStatus(Document.Status.PENDING);
      d.setReviewedBy(null);
      d.setReviewedByName(null);
      d.setReviewedAt(null);
      d.setRejectReason(null);
      record(d, "RESUBMITTED", reason.trim());
      repository.flush();
      return DocumentDto.of(d);
    } catch (RuntimeException ex) {
      if (key != null) storage.delete(key);
      throw ex;
    }
  }
}
