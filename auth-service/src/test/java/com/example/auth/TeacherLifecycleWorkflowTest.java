package com.example.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.auth.common.ApiException;
import com.example.auth.dto.*;
import com.example.auth.entity.*;
import com.example.auth.integration.BranchDirectory;
import com.example.auth.repository.*;
import com.example.auth.security.AccountPrincipal;
import com.example.auth.service.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:teacher-profile-tests;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class TeacherLifecycleWorkflowTest {
  @Autowired AccountRepository accounts;
  @Autowired TeacherRepository teachers;
  @Autowired TeacherProfileEventRepository events;
  @Autowired TeacherProfileService service;
  @Autowired AccountSecurityService security;
  @Autowired AuthService auth;
  @MockitoBean BranchDirectory directory;

  Account account(Role role, Long... branches) {
    var a =
        Account.builder()
            .username("TP-" + UUID.randomUUID())
            .password("not-used")
            .role(role)
            .status(true)
            .branchId(branches.length == 0 ? null : branches[0])
            .build();
    for (var id : branches) {
      var b = new AccountBranch();
      b.setAccount(a);
      b.setBranchId(id);
      b.setStatus(true);
      b.setAssignedAt(java.time.LocalDateTime.now());
      a.getBranchAssignments().add(b);
    }
    return accounts.saveAndFlush(a);
  }

  Teacher teacher(Account a) {
    var t = new Teacher();
    t.setAccount(a);
    t.setFullName("Original teacher");
    t.setWorkStatus("ACTIVE");
    return teachers.saveAndFlush(t);
  }

  TeacherProfileDto.Update update(long v, String name) {
    return new TeacherProfileDto.Update(
        v,
        name,
        "teacher@example.test",
        "0901234567",
        "IELTS",
        "Master",
        "3 years",
        "Certificate",
        null);
  }

  void denied(Runnable r, HttpStatus status) {
    assertEquals(status, assertThrows(ApiException.class, r::run).getStatus());
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void managerUpdatesVersionAuditAndSearchWithoutChangingAccount() {
    var manager = account(Role.SUPER_ADMIN);
    var a = account(Role.TEACHER, 1L);
    var t = teacher(a);
    var saved = service.update(t.getId(), update(0, "Nguyễn Teacher"), manager.getUsername());
    assertEquals(1, saved.version());
    assertEquals("Nguyễn Teacher", saved.fullName());
    assertEquals(a.getId(), saved.accountId());
    assertTrue(accounts.findById(a.getId()).orElseThrow().getStatus());
    assertEquals(
        "Nguyễn Teacher",
        auth.accounts(manager.getUsername(), 0, 20, null, Role.TEACHER, "NGUYỄN")
            .content()
            .getFirst()
            .getFullName());
    assertEquals(1, service.history(t.getId(), manager.getUsername()).size());
    assertTrue(
        service
            .history(t.getId(), manager.getUsername())
            .getFirst()
            .beforeState()
            .contains("Original teacher"));
  }

  @Test
  void staleUpdateDoesNotOverwrite() {
    var a = account(Role.SUPER_ADMIN);
    var t = teacher(account(Role.TEACHER, 1L));
    service.update(t.getId(), update(0, "First"), a.getUsername());
    denied(
        () -> service.update(t.getId(), update(0, "Stale"), a.getUsername()), HttpStatus.CONFLICT);
    assertEquals("First", teachers.findById(t.getId()).orElseThrow().getFullName());
    assertEquals(1, events.findByTeacherIdOrderByIdDesc(t.getId()).size());
  }

  @Test
  void partialBranchAdminCanReadButCannotWriteHistoryOrResponsibilities() {
    var a = account(Role.ADMIN, 1L);
    var t = teacher(account(Role.TEACHER, 1L, 2L));
    assertFalse(service.get(t.getId(), a.getUsername()).canManage());
    denied(
        () -> service.update(t.getId(), update(0, "bad"), a.getUsername()), HttpStatus.FORBIDDEN);
    denied(() -> service.history(t.getId(), a.getUsername()), HttpStatus.FORBIDDEN);
    denied(() -> service.responsibilities(t.getId(), a.getUsername()), HttpStatus.FORBIDDEN);
    verifyNoInteractions(directory);
  }

  @Test
  void allBranchAdminCanEditAndUnassignedTeacherIsSuperOnly() {
    var a = account(Role.ADMIN, 1L, 2L);
    var t = teacher(account(Role.TEACHER, 1L, 2L));
    assertTrue(service.update(t.getId(), update(0, "Allowed"), a.getUsername()).canManage());
    var orphan = teacher(account(Role.TEACHER));
    denied(
        () -> service.update(orphan.getId(), update(0, "bad"), a.getUsername()),
        HttpStatus.NOT_FOUND);
  }

  @Test
  void staffTeacherAndForeignAdminDenied() {
    var t = teacher(account(Role.TEACHER, 1L));
    for (var role : List.of(Role.STAFF, Role.TEACHER)) {
      var a = account(role, 1L);
      denied(() -> service.get(t.getId(), a.getUsername()), HttpStatus.FORBIDDEN);
    }
    var a = account(Role.ADMIN, 2L);
    denied(() -> service.get(t.getId(), a.getUsername()), HttpStatus.NOT_FOUND);
  }

  @Test
  void selfContactUpdateInvalidatesManagerVersionAndRecordsActor() {
    var a = account(Role.TEACHER, 1L);
    var t = teacher(a);
    var principal = new AccountPrincipal(a);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    security.updateProfile(
        a.getUsername(), new ProfileUpdateRequest("new@example.test", "0901234567"));
    assertEquals(1, t.getVersion());
    var manager = account(Role.SUPER_ADMIN);
    denied(
        () -> service.update(t.getId(), update(0, "Old editor"), manager.getUsername()),
        HttpStatus.CONFLICT);
    var event = events.findByTeacherIdOrderByIdDesc(t.getId()).getFirst();
    assertEquals("CONTACT_UPDATED", event.getAction());
    assertEquals(a.getId(), event.getActorId());
  }

  @Test
  void inactiveProfileRemainsVisibleAndActivationDoesNotUnlockAccount() {
    var manager = account(Role.SUPER_ADMIN);
    var a = account(Role.TEACHER, 1L);
    a.setStatus(false);
    accounts.saveAndFlush(a);
    var t = teacher(a);
    t.setWorkStatus("INACTIVE");
    teachers.saveAndFlush(t);
    long v = t.getVersion();
    assertEquals("INACTIVE", service.get(t.getId(), manager.getUsername()).workStatus());
    var result =
        service.workStatus(
            t.getId(),
            new TeacherProfileDto.WorkStatus(v, "ACTIVE", "Return to work"),
            manager.getUsername());
    assertEquals("ACTIVE", result.workStatus());
    assertFalse(result.accountActive());
    verifyNoInteractions(directory);
  }

  @Test
  void unresolvedSubstituteOrOverdueSessionBlocksOffboarding() {
    var manager = account(Role.ADMIN, 1L);
    var t = teacher(account(Role.TEACHER, 1L));
    when(directory.teachingResponsibilities(t.getId(), List.of(1L)))
        .thenReturn(
            List.of(
                new TeacherProfileDto.Responsibility(
                    "SESSION",
                    9L,
                    1L,
                    5L,
                    "C5",
                    "Past substitute",
                    java.time.LocalDate.now().minusDays(5),
                    "SCHEDULED",
                    0L)));
    denied(
        () ->
            service.workStatus(
                t.getId(),
                new TeacherProfileDto.WorkStatus(0L, "INACTIVE", "Leaving center"),
                manager.getUsername()),
        HttpStatus.CONFLICT);
    assertEquals("ACTIVE", t.getWorkStatus());
    assertTrue(events.findByTeacherIdOrderByIdDesc(t.getId()).isEmpty());
  }

  @Test
  void clearedResponsibilitiesAllowOffboardingWithAuditAndVersion() {
    var a = account(Role.ADMIN, 1L);
    var t = teacher(account(Role.TEACHER, 1L));
    when(directory.teachingResponsibilities(t.getId(), List.of(1L))).thenReturn(List.of());
    var r =
        service.workStatus(
            t.getId(),
            new TeacherProfileDto.WorkStatus(0L, "INACTIVE", "Bàn giao xong"),
            a.getUsername());
    assertEquals("INACTIVE", r.workStatus());
    assertTrue(r.accountActive());
    assertEquals(1, r.version());
    assertEquals(
        "WORK_STATUS_CHANGED", service.history(t.getId(), a.getUsername()).getFirst().action());
  }

  @Test
  void dependencyFailureFailsClosed() {
    var a = account(Role.ADMIN, 1L);
    var t = teacher(account(Role.TEACHER, 1L));
    when(directory.teachingResponsibilities(t.getId(), List.of(1L)))
        .thenThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Academic unavailable"));
    denied(
        () ->
            service.workStatus(
                t.getId(),
                new TeacherProfileDto.WorkStatus(0L, "INACTIVE", "Leaving"),
                a.getUsername()),
        HttpStatus.SERVICE_UNAVAILABLE);
    assertEquals("ACTIVE", t.getWorkStatus());
  }

  @Test
  void globalManagerChecksOrphanResponsibilitiesToo() {
    var a = account(Role.SUPER_ADMIN);
    var t = teacher(account(Role.TEACHER));
    when(directory.teachingResponsibilities(t.getId(), null))
        .thenReturn(
            List.of(
                new TeacherProfileDto.Responsibility(
                    "CLASS", 4L, 99L, 4L, "OLD", "Legacy", null, "ONGOING", null)));
    denied(
        () ->
            service.workStatus(
                t.getId(),
                new TeacherProfileDto.WorkStatus(0L, "INACTIVE", "Leaving"),
                a.getUsername()),
        HttpStatus.CONFLICT);
    verify(directory).teachingResponsibilities(t.getId(), null);
  }

  @Test
  void lockingAccountStillWorksWithPendingClasses() {
    var a = account(Role.SUPER_ADMIN);
    var target = account(Role.TEACHER, 1L);
    teacher(target);
    auth.toggleStatus(target.getId(), a.getUsername());
    assertFalse(target.getStatus());
    verifyNoInteractions(directory);
  }
}
