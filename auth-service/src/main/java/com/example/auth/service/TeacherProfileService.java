package com.example.auth.service;

import com.example.auth.common.*;
import com.example.auth.dto.*;
import com.example.auth.entity.*;
import com.example.auth.integration.BranchDirectory;
import com.example.auth.repository.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class TeacherProfileService {
  private final AccountRepository accounts;
  private final TeacherRepository teachers;
  private final TeacherProfileEventRepository events;
  private final BranchDirectory branches;
  private final EntityManager em;

  private Account caller(String username) {
    var a = accounts.findByUsername(username).orElseThrow(ApiException::missing);
    if (!Boolean.TRUE.equals(a.getStatus())
        || (a.getRole() != Role.SUPER_ADMIN && a.getRole() != Role.ADMIN))
      throw ApiException.forbidden();
    return a;
  }

  private boolean canManage(Account caller, Account target) {
    return caller.getRole() == Role.SUPER_ADMIN
        || (!target.activeBranchIds().isEmpty()
            && caller.activeBranchIds().containsAll(target.activeBranchIds()));
  }

  private Teacher require(Long id, Account caller, boolean write) {
    var t = teachers.findById(id).orElseThrow(ApiException::missing);
    if (write) {
      var account = accounts.lock(t.getAccount().getId()).orElseThrow(ApiException::missing);
      em.refresh(account, LockModeType.PESSIMISTIC_WRITE);
      em.refresh(t, LockModeType.PESSIMISTIC_WRITE);
    }
    if (caller.getRole() != Role.SUPER_ADMIN
        && Collections.disjoint(caller.activeBranchIds(), t.getAccount().activeBranchIds()))
      throw ApiException.missing();
    if (write && !canManage(caller, t.getAccount())) throw ApiException.forbidden();
    return t;
  }

  private TeacherProfileDto dto(Teacher t, Account caller) {
    var a = t.getAccount();
    return new TeacherProfileDto(
        t.getId(),
        a.getId(),
        a.getUsername(),
        Boolean.TRUE.equals(a.getStatus()),
        a.activeBranchIds(),
        canManage(caller, a),
        t.getVersion(),
        t.getFullName(),
        t.getEmail(),
        t.getPhone(),
        t.getSpecialization(),
        t.getQualification(),
        t.getExperience(),
        t.getCertificate(),
        t.getHireDate(),
        t.getWorkStatus());
  }

  private static String text(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  public static String snapshot(Teacher t) {
    return new Snapshot(
            t.getFullName(),
            t.getEmail(),
            t.getPhone(),
            t.getSpecialization(),
            t.getQualification(),
            t.getExperience(),
            t.getCertificate(),
            t.getHireDate(),
            t.getWorkStatus())
        .toString();
  }

  private record Snapshot(
      String fullName,
      String email,
      String phone,
      String specialization,
      String qualification,
      String experience,
      String certificate,
      java.time.LocalDate hireDate,
      String workStatus) {}

  public void record(Teacher t, String before, String action, String reason, Account actor) {
    if (before.equals(snapshot(t))) return;
    teachers.saveAndFlush(t);
    var e = new TeacherProfileEvent();
    e.setTeacherId(t.getId());
    e.setAction(action);
    e.setBeforeState(before);
    e.setAfterState(snapshot(t));
    e.setReason(reason);
    e.setActorId(actor.getId());
    e.setActorName(actor.getUsername());
    e.setTeacherVersion(t.getVersion());
    e.setOccurredAt(LocalDateTime.now(BusinessTime.ZONE));
    events.save(e);
  }

  @Transactional(readOnly = true)
  public TeacherProfileDto get(Long id, String username) {
    var a = caller(username);
    return dto(require(id, a, false), a);
  }

  public TeacherProfileDto update(Long id, TeacherProfileDto.Update r, String username) {
    var a = caller(username);
    var t = require(id, a, true);
    version(t, r.version());
    var before = snapshot(t);
    t.setFullName(r.fullName().trim());
    t.setEmail(text(r.email()));
    t.setPhone(text(r.phone()));
    t.setSpecialization(text(r.specialization()));
    t.setQualification(text(r.qualification()));
    t.setExperience(text(r.experience()));
    t.setCertificate(text(r.certificate()));
    t.setHireDate(r.hireDate());
    record(t, before, "PROFILE_UPDATED", null, a);
    return dto(t, a);
  }

  private void version(Teacher t, Long v) {
    if (v == null || v != t.getVersion())
      throw ApiException.conflict("Hồ sơ giáo viên đã thay đổi; hãy tải lại");
  }

  @Transactional(readOnly = true)
  public List<TeacherProfileDto.Event> history(Long id, String username) {
    var a = caller(username);
    var t = require(id, a, false);
    if (!canManage(a, t.getAccount())) throw ApiException.forbidden();
    return events.findByTeacherIdOrderByIdDesc(id).stream()
        .map(
            e ->
                new TeacherProfileDto.Event(
                    e.getId(),
                    e.getAction(),
                    e.getBeforeState(),
                    e.getAfterState(),
                    e.getReason(),
                    e.getActorId(),
                    e.getActorName(),
                    e.getTeacherVersion(),
                    e.getOccurredAt()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<TeacherProfileDto.Responsibility> responsibilities(Long id, String username) {
    var a = caller(username);
    var t = require(id, a, false);
    if (!canManage(a, t.getAccount())) throw ApiException.forbidden();
    return branches.teachingResponsibilities(
        id, a.getRole() == Role.SUPER_ADMIN ? null : t.getAccount().activeBranchIds());
  }

  public TeacherProfileDto workStatus(Long id, TeacherProfileDto.WorkStatus r, String username) {
    var a = caller(username);
    var t = require(id, a, true);
    version(t, r.version());
    if (!Set.of("ACTIVE", "INACTIVE").contains(r.workStatus()))
      throw ApiException.bad("Trạng thái làm việc không hợp lệ");
    if (r.reason() == null || r.reason().trim().length() < 3 || r.reason().trim().length() > 500)
      throw ApiException.bad("Nhập lý do từ 3 đến 500 ký tự");
    if (Objects.equals(t.getWorkStatus(), r.workStatus())) return dto(t, a);
    if ("INACTIVE".equals(r.workStatus())
        && !branches
            .teachingResponsibilities(
                id, a.getRole() == Role.SUPER_ADMIN ? null : t.getAccount().activeBranchIds())
            .isEmpty())
      throw ApiException.conflict(
          "Cần bàn giao lớp và xử lý mọi buổi chưa hoàn tất trước khi ngừng làm việc");
    var before = snapshot(t);
    t.setWorkStatus(r.workStatus());
    record(t, before, "WORK_STATUS_CHANGED", r.reason().trim(), a);
    return dto(t, a);
  }
}
