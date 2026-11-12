package com.example.auth;

import static org.junit.jupiter.api.Assertions.*;

import com.example.auth.common.ApiException;
import com.example.auth.dto.*;
import com.example.auth.entity.*;
import com.example.auth.repository.*;
import com.example.auth.security.AccountPrincipal;
import com.example.auth.service.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:account-security-tests;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class AccountSecurityTest {
  @Autowired AccountRepository accounts;
  @Autowired AdminRepository admins;
  @Autowired SuperAdminRepository supers;
  @Autowired StaffRepository staffs;
  @Autowired TeacherRepository teachers;
  @Autowired PasswordEncoder encoder;
  @Autowired AccountSecurityService service;
  @Autowired AuthService auth;

  Account account(Role role, Long... branches) {
    var a =
        Account.builder()
            .username("security-" + UUID.randomUUID().toString().substring(0, 8))
            .role(role)
            .password(encoder.encode("123456"))
            .status(true)
            .branchId(branches.length == 0 ? null : branches[0])
            .build();
    for (Long id : branches) {
      var b = new AccountBranch();
      b.setAccount(a);
      b.setBranchId(id);
      b.setStatus(true);
      b.setPrimary(id.equals(a.getBranchId()));
      b.setAssignedAt(java.time.LocalDateTime.now());
      a.getBranchAssignments().add(b);
    }
    a = accounts.saveAndFlush(a);
    switch (role) {
      case SUPER_ADMIN -> {
        var p = new SuperAdmin();
        p.setAccount(a);
        p.setFullName("Super");
        supers.saveAndFlush(p);
      }
      case ADMIN -> {
        var p = new Admin();
        p.setAccount(a);
        p.setFullName("Admin");
        admins.saveAndFlush(p);
      }
      case STAFF -> {
        var p = new Staff();
        p.setAccount(a);
        p.setFullName("Staff");
        staffs.saveAndFlush(p);
      }
      case TEACHER -> {
        var p = new Teacher();
        p.setAccount(a);
        p.setFullName("Teacher");
        teachers.saveAndFlush(p);
      }
    }
    return a;
  }

  void actor(Account a) {
    var principal = new AccountPrincipal(a);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
  }

  LoginRequest loginRequest(Account a, String password) {
    var r = new LoginRequest();
    r.setUsername(a.getUsername());
    r.setPassword(password);
    return r;
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void unknownUsernameUsesAuthenticationProviderAndReturnsGenericFailure() {
    var request = new LoginRequest();
    request.setUsername("missing-" + UUID.randomUUID().toString().substring(0, 8));
    request.setPassword("wrong-password");
    assertEquals(
        "Sai tên đăng nhập hoặc mật khẩu",
        assertThrows(BadCredentialsException.class, () -> auth.login(request)).getMessage());
  }

  @Test
  void changePasswordAcceptsLegacyCurrentPasswordAndRevokesOldPrincipal() {
    var a = account(Role.STAFF, 11L);
    a.setMustChangePassword(true);
    actor(a);
    service.changePassword(
        a.getUsername(), new ChangePasswordRequest("123456", "new-password-1", "new-password-1"));
    assertTrue(encoder.matches("new-password-1", a.getPassword()));
    assertFalse(a.isMustChangePassword());
    assertEquals(1, a.getSessionVersion());
    assertNotNull(a.getPasswordChangedAt());
    assertEquals("PASSWORD_CHANGED", a.getSecurityEvents().getFirst().getAction());
    assertEquals(
        org.springframework.http.HttpStatus.UNAUTHORIZED,
        assertThrows(
                ApiException.class,
                () ->
                    service.updateProfile(
                        a.getUsername(), new ProfileUpdateRequest("x@test.com", null)))
            .getStatus());
  }

  @Test
  void wrongCurrentPasswordDoesNotChangeState() {
    var a = account(Role.TEACHER, 11L);
    actor(a);
    var hash = a.getPassword();
    assertThrows(
        ApiException.class,
        () ->
            service.changePassword(
                a.getUsername(),
                new ChangePasswordRequest("wrong", "new-password-1", "new-password-1")));
    assertEquals(hash, a.getPassword());
    assertEquals(0, a.getSessionVersion());
    assertTrue(a.getSecurityEvents().isEmpty());
  }

  @Test
  void confirmationAndUtf8ByteLimitAreEnforced() {
    var a = account(Role.STAFF, 11L);
    actor(a);
    assertThrows(
        ApiException.class,
        () ->
            service.changePassword(
                a.getUsername(),
                new ChangePasswordRequest("123456", "new-password-1", "different")));
    String longUnicode = "ệ".repeat(25);
    assertThrows(
        ApiException.class,
        () ->
            service.changePassword(
                a.getUsername(), new ChangePasswordRequest("123456", longUnicode, longUnicode)));
    assertEquals(0, a.getSessionVersion());
  }

  @Test
  void cannotChooseSamePassword() {
    var a = account(Role.ADMIN, 11L);
    a.setPassword(encoder.encode("existing-password"));
    actor(a);
    assertThrows(
        ApiException.class,
        () ->
            service.changePassword(
                a.getUsername(),
                new ChangePasswordRequest(
                    "existing-password", "existing-password", "existing-password")));
  }

  @Test
  void logoutAllRevokesSessionWithoutChangingPassword() {
    var a = account(Role.SUPER_ADMIN);
    actor(a);
    String hash = a.getPassword();
    service.logoutAll(a.getUsername());
    assertEquals(1, a.getSessionVersion());
    assertEquals(hash, a.getPassword());
    assertEquals("SESSIONS_REVOKED", a.getSecurityEvents().getFirst().getAction());
  }

  @Test
  void resetInvalidatesPasswordAndRequiresChangeBeforeNormalLogin() {
    var manager = account(Role.SUPER_ADMIN);
    var target = account(Role.STAFF, 11L);
    actor(manager);
    service.resetPassword(
        manager.getUsername(),
        target.getId(),
        new ResetPasswordRequest("temporary-1", "temporary-1", "User forgot password"));
    assertEquals(1, target.getSessionVersion());
    assertTrue(target.isMustChangePassword());
    assertEquals("User forgot password", target.getSecurityEvents().getFirst().getReason());
    assertThrows(BadCredentialsException.class, () -> auth.login(loginRequest(target, "123456")));
    assertTrue(auth.login(loginRequest(target, "temporary-1")).isMustChangePassword());
    actor(target);
    service.changePassword(
        target.getUsername(),
        new ChangePasswordRequest("temporary-1", "private-password-1", "private-password-1"));
    assertFalse(auth.login(loginRequest(target, "private-password-1")).isMustChangePassword());
    assertEquals(2, target.getSessionVersion());
  }

  @Test
  void adminCannotResetTeacherSharedOutsideItsScope() {
    var manager = account(Role.ADMIN, 11L, 12L);
    var target = account(Role.TEACHER, 11L, 13L);
    actor(manager);
    assertEquals(
        org.springframework.http.HttpStatus.FORBIDDEN,
        assertThrows(
                ApiException.class,
                () ->
                    service.resetPassword(
                        manager.getUsername(),
                        target.getId(),
                        new ResetPasswordRequest("temporary-1", "temporary-1", "Forgot password")))
            .getStatus());
    assertEquals(0, target.getSessionVersion());
  }

  @Test
  void adminCanResetSecondAssignedBranchStaffButCannotResetAdmin() {
    var manager = account(Role.ADMIN, 11L, 12L);
    var staff = account(Role.STAFF, 12L);
    actor(manager);
    service.resetPassword(
        manager.getUsername(),
        staff.getId(),
        new ResetPasswordRequest("temporary-1", "temporary-1", "Forgot password"));
    assertTrue(staff.isMustChangePassword());
    var admin = account(Role.ADMIN, 12L);
    assertThrows(
        ApiException.class,
        () ->
            service.resetPassword(
                manager.getUsername(),
                admin.getId(),
                new ResetPasswordRequest("temporary-1", "temporary-1", "Forgot password")));
  }

  @Test
  void selfResetAndSuperAdminResetAreRejected() {
    var sa = account(Role.SUPER_ADMIN);
    actor(sa);
    assertThrows(
        ApiException.class,
        () ->
            service.resetPassword(
                sa.getUsername(),
                sa.getId(),
                new ResetPasswordRequest("temporary-1", "temporary-1", "Forgot password")));
    var another = account(Role.SUPER_ADMIN);
    assertThrows(
        ApiException.class,
        () ->
            service.resetPassword(
                sa.getUsername(),
                another.getId(),
                new ResetPasswordRequest("temporary-1", "temporary-1", "Forgot password")));
  }

  @Test
  void resetDoesNotUnlockDisabledAccount() {
    var sa = account(Role.SUPER_ADMIN);
    var target = account(Role.STAFF, 11L);
    target.setStatus(false);
    actor(sa);
    service.resetPassword(
        sa.getUsername(),
        target.getId(),
        new ResetPasswordRequest("temporary-1", "temporary-1", "Forgot password"));
    assertFalse(target.getStatus());
  }

  @Test
  void eachRoleCanUpdateOwnContactsWithoutChangingRoleOrAssignments() {
    for (Role role : java.util.List.of(Role.SUPER_ADMIN, Role.ADMIN, Role.STAFF, Role.TEACHER)) {
      var a = role == Role.SUPER_ADMIN ? account(role) : account(role, 11L);
      actor(a);
      var response =
          service.updateProfile(
              a.getUsername(), new ProfileUpdateRequest("  user@test.com  ", "0901234567"));
      assertEquals("user@test.com", response.getEmail());
      assertEquals("0901234567", response.getPhone());
      assertEquals(role, response.getRole());
      assertEquals(a.getBranchId(), response.getBranchId());
      assertEquals(0, a.getSessionVersion());
    }
  }
}
