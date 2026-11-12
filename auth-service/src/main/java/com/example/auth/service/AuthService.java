package com.example.auth.service;

import com.example.auth.common.*;
import com.example.auth.dto.*;
import com.example.auth.entity.*;
import com.example.auth.integration.BranchDirectory;
import com.example.auth.repository.*;
import com.example.auth.security.JwtUtil;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final AccountRepository accountRepository;
  private final SuperAdminRepository superAdminRepository;
  private final AdminRepository adminRepository;
  private final StaffRepository staffRepository;
  private final TeacherRepository teacherRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final AuthenticationManager authenticationManager;
  private final BranchDirectory branches;
  private final com.example.auth.student.StudentAccountService studentAccounts;

  private record ProfileInfo(Long profileId, String fullName) {}

  // ===================== LOGIN =====================
  @Transactional
  public LoginResponse login(LoginRequest request) {
    if (request.getPassword() == null
        || request.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new BadCredentialsException("Sai tên đăng nhập hoặc mật khẩu");
    Account account = accountRepository.lockByUsername(request.getUsername()).orElse(null);
    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
    } catch (org.springframework.security.core.AuthenticationException e) {
      throw new BadCredentialsException("Sai tên đăng nhập hoặc mật khẩu");
    }
    if (account == null) throw new BadCredentialsException("Sai tên đăng nhập hoặc mật khẩu");

    account.setLastLogin(LocalDateTime.now(com.example.auth.common.BusinessTime.ZONE));
    accountRepository.save(account);

    ProfileInfo profile = resolveProfile(account);
    if (profile.profileId() == null
        || (account.getRole() != Role.SUPER_ADMIN && account.getRole() != Role.STUDENT && account.activeBranchIds().isEmpty()))
      throw ApiException.forbidden();

    String token =
        jwtUtil.generateToken(
            account.getUsername(),
            account.getRole().name(),
            account.getId(),
            profile.profileId(),
            account.getBranchId(),
            account.getSessionVersion());

    return LoginResponse.builder()
        .mustChangePassword(account.isMustChangePassword())
        .token(token)
        .accountId(account.getId())
        .profileId(profile.profileId())
        .branchId(account.getBranchId())
        .branchIds(account.activeBranchIds())
        .username(account.getUsername())
        .role(account.getRole().name())
        .fullName(profile.fullName())
        .build();
  }

  // ===================== TẠO TÀI KHOẢN =====================
  @Transactional
  public UserResponse createAccount(CreateUserRequest request, String callerUsername) {
    Account caller = getCaller(callerUsername);
    Role target = request.getRole();
    if (request.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw ApiException.bad("Password must not exceed 72 UTF-8 bytes");
    requireManager(caller);

    if (accountRepository.existsByUsername(request.getUsername())) {
      throw ApiException.conflict("Username đã tồn tại");
    }
    if (target == Role.STUDENT) throw ApiException.bad("Use student account provisioning");
    if (target == Role.SUPER_ADMIN) {
      throw ApiException.bad("Không thể tạo tài khoản Super Admin qua API này");
    }

    if (caller.getRole() == Role.ADMIN && target == Role.ADMIN) throw ApiException.forbidden();
    Long branchId = branch(caller, request.getBranchId());
    if (branchId == null) throw ApiException.bad("Phải chọn chi nhánh cho tài khoản mới");

    branches.requireActive(branchId);
    Account savedAccount =
        accountRepository.save(
            Account.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(target)
                .status(true)
                .mustChangePassword(true)
                .branchId(branchId)
                .build());

    applyAssignments(savedAccount, java.util.List.of(branchId), branchId, caller.getId());
    accountRepository.save(savedAccount);
    String workStatus = request.getWorkStatus() != null ? request.getWorkStatus() : "ACTIVE";

    return switch (target) {
      case ADMIN -> {
        Admin saved =
            adminRepository.save(
                Admin.builder()
                    .account(savedAccount)
                    .fullName(request.getFullName())
                    .email(request.getEmail())
                    .phone(request.getPhone())
                    .hireDate(request.getHireDate())
                    .workStatus(workStatus)
                    .build());
        yield toAdminResponse(savedAccount, saved);
      }
      case STAFF -> {
        Staff saved =
            staffRepository.save(
                Staff.builder()
                    .account(savedAccount)
                    .fullName(request.getFullName())
                    .email(request.getEmail())
                    .phone(request.getPhone())
                    .hireDate(request.getHireDate())
                    .workStatus(workStatus)
                    .position(request.getPosition())
                    .build());
        yield toStaffResponse(savedAccount, saved);
      }
      case TEACHER -> {
        Teacher saved =
            teacherRepository.save(
                Teacher.builder()
                    .account(savedAccount)
                    .fullName(request.getFullName())
                    .email(request.getEmail())
                    .phone(request.getPhone())
                    .specialization(request.getSpecialization())
                    .qualification(request.getQualification())
                    .experience(request.getExperience())
                    .certificate(request.getCertificate())
                    .hireDate(request.getHireDate())
                    .workStatus(workStatus)
                    .build());
        yield toTeacherResponse(savedAccount, saved);
      }
      default -> throw ApiException.bad("Vai trò không hợp lệ");
    };
  }

  // ===================== DANH SÁCH / KHÓA MỞ / ME =====================

  @Transactional
  public UserResponse toggleStatus(Long id, String callerUsername) {
    Account caller = getCaller(callerUsername);
    requireManager(caller);
    Account account = accountRepository.lock(id).orElseThrow(ApiException::missing);
    if (account.getRole() == Role.STUDENT) throw ApiException.forbidden();
    if (account.getRole() == Role.SUPER_ADMIN)
      throw ApiException.bad("Không thể khóa tài khoản Super Admin");
    if (caller.getRole() == Role.ADMIN) {
      if (java.util.Collections.disjoint(caller.activeBranchIds(), account.activeBranchIds()))
        throw ApiException.missing();
      // Global account status must not affect branches outside this admin's authority.
      if (account.getRole() == Role.ADMIN
          || !caller.activeBranchIds().containsAll(account.activeBranchIds()))
        throw ApiException.forbidden();
    }

    account.setStatus(!account.getStatus());
    if (!account.getStatus())
      account.setSessionVersion(Math.addExact(account.getSessionVersion(), 1));
    account.recordSecurityEvent(
        account.getStatus() ? "ACCOUNT_UNLOCKED" : "ACCOUNT_LOCKED", caller.getId(), null);
    return getUserResponseByAccount(accountRepository.save(account));
  }

  @Transactional(readOnly = true)
  public UserResponse getByUsername(String username) {
    return getUserResponseByAccount(getCaller(username));
  }

  // ===================== HÀM PHỤ =====================
  private void requireManager(Account caller) {
    if (caller.getRole() != Role.ADMIN && caller.getRole() != Role.SUPER_ADMIN)
      throw ApiException.forbidden();
  }

  private Long branch(Account caller, Long requested) {
    if (requested != null && requested <= 0) throw ApiException.bad("Invalid branchId");
    if (caller.getRole() == Role.SUPER_ADMIN) return requested;
    Long selected = requested != null ? requested : selectedBranch(caller);
    if (selected == null || !caller.activeBranchIds().contains(selected))
      throw ApiException.missing();
    return selected;
  }

  private Long selectedBranch(Account caller) {
    var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
    if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes r) {
      String header = r.getRequest().getHeader("X-Branch-Id");
      if (header != null) {
        try {
          return Long.valueOf(header);
        } catch (NumberFormatException e) {
          throw ApiException.bad("Invalid X-Branch-Id");
        }
      }
    }
    return caller.getBranchId();
  }

  @Transactional(readOnly = true)
  public List<BranchAssignmentResponse> assignments(Long id, String username) {
    Account caller = getCaller(username);
    if (caller.getRole() != Role.SUPER_ADMIN) throw ApiException.forbidden();
    return accountRepository
        .findById(id)
        .orElseThrow(ApiException::missing)
        .getBranchAssignments()
        .stream()
        .map(
            a ->
                new BranchAssignmentResponse(
                    a.getBranchId(),
                    a.isStatus(),
                    a.isPrimary(),
                    a.getAssignedAt(),
                    a.getAssignedBy(),
                    a.getRevokedAt()))
        .toList();
  }

  @Transactional
  public UserResponse assignBranches(Long id, BranchAssignmentsRequest request, String username) {
    Account caller = getCaller(username);
    if (caller.getRole() != Role.SUPER_ADMIN) throw ApiException.forbidden();
    Account account = accountRepository.lock(id).orElseThrow(ApiException::missing);
    if (account.getRole() == Role.SUPER_ADMIN)
      throw ApiException.bad("Super Admin có phạm vi toàn hệ thống");
    if (account.getRole() == Role.STUDENT) throw ApiException.forbidden();
    var ids = request.branchIds();
    if (ids == null
        || ids.size() > 100
        || ids.stream().anyMatch(b -> b == null || b <= 0)
        || new java.util.HashSet<>(ids).size() != ids.size())
      throw ApiException.bad("Invalid branchIds");
    if (account.getRole() == Role.STAFF && ids.size() > 1)
      throw ApiException.bad("Staff chỉ được phân công một chi nhánh");
    if (ids.isEmpty()
        ? request.primaryBranchId() != null
        : !ids.contains(request.primaryBranchId()))
      throw ApiException.bad("Chi nhánh chính phải thuộc danh sách phân công");
    for (Long b : ids) {
      // Existing inactive branches may be retained, but cannot be newly assigned.
      if (!account.activeBranchIds().contains(b)) branches.requireActive(b);
    }
    if (account.getRole() == Role.TEACHER) {
      var removed = new java.util.ArrayList<>(account.activeBranchIds());
      removed.removeAll(ids);
      if (!removed.isEmpty()) {
        Long teacherId =
            teacherRepository.findByAccountId(id).orElseThrow(ApiException::missing).getId();
        branches.requireNoTeachingClasses(teacherId, removed);
      }
    }
    if (account.getRole() == Role.ADMIN || account.getRole() == Role.STAFF) {
      var removed = new java.util.ArrayList<>(account.activeBranchIds());
      removed.removeAll(ids);
      if (!removed.isEmpty()) branches.requireNoActiveLeads(id, removed);
    }
    applyAssignments(account, ids, request.primaryBranchId(), caller.getId());
    return getUserResponseByAccount(accountRepository.save(account));
  }

  public record AssignmentEvent(
      Long id, Long branchId, String action, LocalDateTime occurredAt, Long performedBy) {}

  @Transactional(readOnly = true)
  public List<AssignmentEvent> assignmentHistory(Long id, String username) {
    if (getCaller(username).getRole() != Role.SUPER_ADMIN) throw ApiException.forbidden();
    return accountRepository
        .findById(id)
        .orElseThrow(ApiException::missing)
        .getBranchEvents()
        .stream()
        .sorted(
            java.util.Comparator.comparing(AccountBranchEvent::getOccurredAt)
                .thenComparing(AccountBranchEvent::getId)
                .reversed())
        .map(
            e ->
                new AssignmentEvent(
                    e.getId(),
                    e.getBranchId(),
                    e.getAction(),
                    e.getOccurredAt(),
                    e.getPerformedBy()))
        .toList();
  }

  private void event(Account account, Long branch, String action, Long actor, LocalDateTime now) {
    var e = new AccountBranchEvent();
    e.setAccount(account);
    e.setBranchId(branch);
    e.setAction(action);
    e.setOccurredAt(now);
    e.setPerformedBy(actor);
    account.getBranchEvents().add(e);
  }

  private void applyAssignments(Account account, List<Long> ids, Long primary, Long assignedBy) {
    var now = LocalDateTime.now(BusinessTime.ZONE);
    for (var row : account.getBranchAssignments()) {
      if (row.isStatus() && !ids.contains(row.getBranchId()))
        event(account, row.getBranchId(), "REVOKED", assignedBy, now);
    }
    for (Long id : ids) {
      boolean existed = account.activeBranchIds().contains(id);
      if (!existed) event(account, id, "ASSIGNED", assignedBy, now);
    }
    if (!java.util.Objects.equals(account.getBranchId(), primary)) {
      if (account.getBranchId() != null)
        event(account, account.getBranchId(), "PRIMARY_CLEARED", assignedBy, now);
      if (primary != null) event(account, primary, "PRIMARY_SET", assignedBy, now);
    }
    for (AccountBranch a : account.getBranchAssignments()) {
      if (a.isStatus() && !ids.contains(a.getBranchId())) {
        a.setStatus(false);
        a.setRevokedAt(now);
      }
      a.setPrimary(a.isStatus() && java.util.Objects.equals(a.getBranchId(), primary));
    }
    for (Long id : ids) {
      var a =
          account.getBranchAssignments().stream()
              .filter(b -> b.getBranchId().equals(id))
              .findFirst()
              .orElse(null);
      if (a == null) {
        a = new AccountBranch();
        a.setAccount(account);
        a.setBranchId(id);
        account.getBranchAssignments().add(a);
      }
      if (!a.isStatus()) {
        a.setAssignedAt(now);
        a.setAssignedBy(assignedBy);
        a.setRevokedAt(null);
      }
      a.setStatus(true);
      a.setPrimary(id.equals(primary));
    }
    account.setBranchId(primary);
  }

  @Transactional(readOnly = true)
  public PageResponse<UserResponse> accounts(
      String username, int page, int size, Long branchId, Role role, String keyword) {
    Account caller = getCaller(username);
    requireManager(caller);
    if (role == Role.STUDENT) throw ApiException.bad("Use student account management");
    Long branch = branch(caller, branchId);
    return PageResponse.of(
        accountRepository
            .findAll(
                ((org.springframework.data.jpa.domain.Specification<Account>)
                        (r, q, cb) -> {
                          if (branch == null) return cb.conjunction();
                          var sub = q.subquery(Long.class);
                          var membership = sub.from(AccountBranch.class);
                          sub.select(membership.get("account").get("id"))
                              .where(
                                  cb.equal(membership.get("branchId"), branch),
                                  cb.isTrue(membership.get("status")));
                          return r.get("id").in(sub);
                        })
                    .and((r,q,cb) -> cb.notEqual(r.get("role"), Role.STUDENT)).and(Query.eq("role", role))
                    .and(
                        (r, q, cb) -> {
                          if (keyword == null || keyword.isBlank()) return cb.conjunction();
                          String pattern =
                              "%"
                                  + keyword
                                      .trim()
                                      .toLowerCase(java.util.Locale.ROOT)
                                      .replace("\\", "\\\\")
                                      .replace("%", "\\%")
                                      .replace("_", "\\_")
                                  + "%";
                          var sub = q.subquery(Long.class);
                          var t = sub.from(Teacher.class);
                          sub.select(t.get("account").get("id"))
                              .where(cb.like(cb.lower(t.get("fullName")), pattern, '\\'));
                          return cb.or(
                              Query.<Account>keyword(keyword, "username").toPredicate(r, q, cb),
                              r.get("id").in(sub));
                        }),
                Query.page(page, size))
            .map(this::getUserResponseByAccount));
  }

  @Transactional(readOnly = true)
  public List<TeacherResponse> teachers(String username, Long branchId) {
    Account caller = getCaller(username);
    if (caller.getRole() == Role.TEACHER) throw ApiException.forbidden();
    return teacherRepository.available(branch(caller, branchId)).stream()
        .map(
            t ->
                new TeacherResponse(
                    t.getId(),
                    t.getAccount().getId(),
                    t.getFullName(),
                    t.getAccount().getBranchId(),
                    t.getAccount().activeBranchIds()))
        .toList();
  }

  @Transactional(readOnly = true)
  public TeacherResponse teacher(String username, Long id) {
    return teacher(username, id, null);
  }

  @Transactional(readOnly = true)
  public TeacherResponse teacher(String username, Long id, Long branchId) {
    Account caller = getCaller(username);
    if (caller.getRole() == Role.TEACHER) throw ApiException.forbidden();
    Teacher teacher = teacherRepository.findById(id).orElseThrow(ApiException::missing);
    Long own = branch(caller, branchId);
    if (own != null && !teacher.getAccount().activeBranchIds().contains(own))
      throw ApiException.missing();
    if (!Boolean.TRUE.equals(teacher.getAccount().getStatus())
        || !"ACTIVE".equals(teacher.getWorkStatus())) throw ApiException.bad("Teacher is inactive");
    return new TeacherResponse(
        teacher.getId(),
        teacher.getAccount().getId(),
        teacher.getFullName(),
        teacher.getAccount().getBranchId(),
        teacher.getAccount().activeBranchIds());
  }

  private Account getCaller(String username) {
    Account caller = accountRepository.findByUsername(username).orElseThrow(ApiException::missing);
    if (!Boolean.TRUE.equals(caller.getStatus())) throw ApiException.forbidden();
    return caller;
  }

  private ProfileInfo resolveProfile(Account account) {
    Long id = account.getId();
    return switch (account.getRole()) {
      case STUDENT -> { var p = studentAccounts.profile(id); yield new ProfileInfo(p.id(), p.fullName()); }
      case SUPER_ADMIN ->
          superAdminRepository
              .findByAccountId(id)
              .map(p -> new ProfileInfo(p.getId(), p.getFullName()))
              .orElse(new ProfileInfo(null, null));
      case ADMIN ->
          adminRepository
              .findByAccountId(id)
              .map(p -> new ProfileInfo(p.getId(), p.getFullName()))
              .orElse(new ProfileInfo(null, null));
      case STAFF ->
          staffRepository
              .findByAccountId(id)
              .map(p -> new ProfileInfo(p.getId(), p.getFullName()))
              .orElse(new ProfileInfo(null, null));
      case TEACHER ->
          teacherRepository
              .findByAccountId(id)
              .map(p -> new ProfileInfo(p.getId(), p.getFullName()))
              .orElse(new ProfileInfo(null, null));
    };
  }

  private UserResponse getUserResponseByAccount(Account account) {
    Long id = account.getId();
    return switch (account.getRole()) {
      case STUDENT -> { var p = studentAccounts.profile(id); yield baseResponse(account).profileId(p.id()).branchId(p.branchId()).branchIds(java.util.List.of()).fullName(p.fullName()).email(account.isMustChangePassword() ? null : p.email()).phone(account.isMustChangePassword() ? null : p.phone()).build(); }
      case SUPER_ADMIN ->
          toSuperAdminResponse(account, superAdminRepository.findByAccountId(id).orElse(null));
      case ADMIN -> toAdminResponse(account, adminRepository.findByAccountId(id).orElse(null));
      case STAFF -> toStaffResponse(account, staffRepository.findByAccountId(id).orElse(null));
      case TEACHER ->
          toTeacherResponse(account, teacherRepository.findByAccountId(id).orElse(null));
    };
  }

  private UserResponse.UserResponseBuilder baseResponse(Account account) {
    return UserResponse.builder()
        .mustChangePassword(account.isMustChangePassword())
        .passwordChangedAt(account.getPasswordChangedAt())
        .accountId(account.getId())
        .username(account.getUsername())
        .role(account.getRole())
        .status(account.getStatus())
        .branchId(account.getBranchId())
        .branchIds(account.activeBranchIds())
        .createdAt(account.getCreatedAt())
        .lastLogin(account.getLastLogin());
  }

  private UserResponse toSuperAdminResponse(Account account, SuperAdmin p) {
    return baseResponse(account)
        .profileId(p != null ? p.getId() : null)
        .fullName(p != null ? p.getFullName() : null)
        .email(p != null ? p.getEmail() : null)
        .phone(p != null ? p.getPhone() : null)
        .build();
  }

  private UserResponse toAdminResponse(Account account, Admin p) {
    return baseResponse(account)
        .profileId(p != null ? p.getId() : null)
        .fullName(p != null ? p.getFullName() : null)
        .email(p != null ? p.getEmail() : null)
        .phone(p != null ? p.getPhone() : null)
        .hireDate(p != null ? p.getHireDate() : null)
        .workStatus(p != null ? p.getWorkStatus() : null)
        .build();
  }

  private UserResponse toStaffResponse(Account account, Staff p) {
    return baseResponse(account)
        .profileId(p != null ? p.getId() : null)
        .fullName(p != null ? p.getFullName() : null)
        .email(p != null ? p.getEmail() : null)
        .phone(p != null ? p.getPhone() : null)
        .hireDate(p != null ? p.getHireDate() : null)
        .workStatus(p != null ? p.getWorkStatus() : null)
        .position(p != null ? p.getPosition() : null)
        .build();
  }

  private UserResponse toTeacherResponse(Account account, Teacher p) {
    return baseResponse(account)
        .profileId(p != null ? p.getId() : null)
        .fullName(p != null ? p.getFullName() : null)
        .email(p != null ? p.getEmail() : null)
        .phone(p != null ? p.getPhone() : null)
        .specialization(p != null ? p.getSpecialization() : null)
        .qualification(p != null ? p.getQualification() : null)
        .experience(p != null ? p.getExperience() : null)
        .certificate(p != null ? p.getCertificate() : null)
        .hireDate(p != null ? p.getHireDate() : null)
        .workStatus(p != null ? p.getWorkStatus() : null)
        .build();
  }
}
