package com.example.auth.service;

import com.example.auth.common.*;
import com.example.auth.dto.*;
import com.example.auth.entity.*;
import com.example.auth.repository.*;
import com.example.auth.security.AccountPrincipal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountSecurityService {
  private final AccountRepository accounts;
  private final SuperAdminRepository superAdmins;
  private final AdminRepository admins;
  private final StaffRepository staffs;
  private final TeacherRepository teachers;
  private final PasswordEncoder encoder;
  private final AuthService auth;
  private final TeacherProfileService teacherProfiles;

  private void checkSession(Account account) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!Boolean.TRUE.equals(account.getStatus())
        || authentication == null
        || !(authentication.getPrincipal() instanceof AccountPrincipal principal)
        || !account.getId().equals(principal.accountId())
        || account.getSessionVersion() != principal.sessionVersion()) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hiệu lực");
    }
  }

  private Account lockSelf(String username) {
    var account = accounts.lockByUsername(username).orElseThrow(ApiException::missing);
    checkSession(account);
    return account;
  }

  private void validatePassword(String password, String confirmation, Account account) {
    if (password == null
        || password.isBlank()
        || password.length() < 8
        || password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw ApiException.bad("Mật khẩu mới phải có ít nhất 8 ký tự và không vượt 72 byte UTF-8");
    if (!password.equals(confirmation)) throw ApiException.bad("Mật khẩu xác nhận chưa khớp");
    if (encoder.matches(password, account.getPassword()))
      throw ApiException.bad("Mật khẩu mới phải khác mật khẩu hiện tại");
  }

  public void changePassword(String username, ChangePasswordRequest request) {
    var account = lockSelf(username);
    if (request.currentPassword() == null
        || request.currentPassword().getBytes(StandardCharsets.UTF_8).length > 72
        || !encoder.matches(request.currentPassword(), account.getPassword()))
      throw ApiException.bad("Mật khẩu hiện tại không đúng");
    validatePassword(request.newPassword(), request.confirmPassword(), account);
    account.setPassword(encoder.encode(request.newPassword()));
    account.setMustChangePassword(false);
    account.setPasswordChangedAt(LocalDateTime.now(BusinessTime.ZONE));
    account.setSessionVersion(Math.addExact(account.getSessionVersion(), 1));
    account.recordSecurityEvent("PASSWORD_CHANGED", account.getId(), null);
    accounts.saveAndFlush(account);
  }

  public void logoutAll(String username) {
    var account = lockSelf(username);
    account.setSessionVersion(Math.addExact(account.getSessionVersion(), 1));
    account.recordSecurityEvent("SESSIONS_REVOKED", account.getId(), null);
    accounts.saveAndFlush(account);
  }

  public void resetPassword(String username, Long targetId, ResetPasswordRequest request) {
    var caller = accounts.findByUsername(username).orElseThrow(ApiException::missing);
    checkSession(caller);
    if (caller.getRole() != Role.SUPER_ADMIN && caller.getRole() != Role.ADMIN)
      throw ApiException.forbidden();
    if (caller.getId().equals(targetId))
      throw ApiException.bad("Dùng chức năng đổi mật khẩu cá nhân cho tài khoản của bạn");
    var target = accounts.lock(targetId).orElseThrow(ApiException::missing);
    if (target.getRole() == Role.SUPER_ADMIN) throw ApiException.forbidden();
    if (caller.getRole() == Role.ADMIN) {
      if (Collections.disjoint(caller.activeBranchIds(), target.activeBranchIds()))
        throw ApiException.missing();
      if ((target.getRole() != Role.STAFF && target.getRole() != Role.TEACHER)
          || !caller.activeBranchIds().containsAll(target.activeBranchIds()))
        throw ApiException.forbidden();
    }
    validatePassword(request.temporaryPassword(), request.confirmPassword(), target);
    if (request.reason() == null
        || request.reason().trim().length() < 3
        || request.reason().trim().length() > 255)
      throw ApiException.bad("Nhập lý do cấp lại mật khẩu từ 3 đến 255 ký tự");
    target.setPassword(encoder.encode(request.temporaryPassword()));
    target.setMustChangePassword(true);
    target.setPasswordChangedAt(LocalDateTime.now(BusinessTime.ZONE));
    target.setSessionVersion(Math.addExact(target.getSessionVersion(), 1));
    target.recordSecurityEvent("PASSWORD_RESET", caller.getId(), request.reason().trim());
    accounts.saveAndFlush(target);
  }

  public UserResponse updateProfile(String username, ProfileUpdateRequest request) {
    var account = lockSelf(username);
    if (account.isMustChangePassword()) throw ApiException.forbidden();
    switch (account.getRole()) {
      case SUPER_ADMIN -> {
        var profile =
            superAdmins.findByAccountId(account.getId()).orElseThrow(ApiException::missing);
        profile.setEmail(request.email());
        profile.setPhone(request.phone());
        superAdmins.save(profile);
      }
      case ADMIN -> {
        var profile = admins.findByAccountId(account.getId()).orElseThrow(ApiException::missing);
        profile.setEmail(request.email());
        profile.setPhone(request.phone());
        admins.save(profile);
      }
      case STAFF -> {
        var profile = staffs.findByAccountId(account.getId()).orElseThrow(ApiException::missing);
        profile.setEmail(request.email());
        profile.setPhone(request.phone());
        staffs.save(profile);
      }
      case TEACHER -> {
        var profile = teachers.findByAccountId(account.getId()).orElseThrow(ApiException::missing);
        var before = TeacherProfileService.snapshot(profile);
        profile.setEmail(request.email());
        profile.setPhone(request.phone());
        teacherProfiles.record(profile, before, "CONTACT_UPDATED", null, account);
      }
    }
    account.recordSecurityEvent("PROFILE_UPDATED", account.getId(), null);
    accounts.saveAndFlush(account);
    return auth.getByUsername(username);
  }
}
