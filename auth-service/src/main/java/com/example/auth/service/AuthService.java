package com.example.auth.service;

import com.example.auth.dto.*;
import com.example.auth.entity.*;
import com.example.auth.repository.*;
import com.example.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

    private record ProfileInfo(Long profileId, String fullName) {}

    // ===================== LOGIN =====================
    @Transactional
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (Exception e) {
            throw new BadCredentialsException("Sai tên đăng nhập hoặc mật khẩu");
        }

        Account account = accountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Sai tên đăng nhập hoặc mật khẩu"));

        account.setLastLogin(LocalDateTime.now());
        accountRepository.save(account);

        ProfileInfo profile = resolveProfile(account);

        String token = jwtUtil.generateToken(
                account.getUsername(),
                account.getRole().name(),
                account.getId(),
                profile.profileId(),
                account.getBranchId()
        );

        return LoginResponse.builder()
                .token(token)
                .accountId(account.getId())
                .profileId(profile.profileId())
                .branchId(account.getBranchId())
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

        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username đã tồn tại");
        }
        if (target == Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("Không thể tạo tài khoản Super Admin qua API này");
        }

        Long branchId;
        if (caller.getRole() == Role.SUPER_ADMIN) {
            if (request.getBranchId() == null) {
                throw new IllegalArgumentException("Phải chọn chi nhánh cho tài khoản mới");
            }
            branchId = request.getBranchId();
        } else if (caller.getRole() == Role.ADMIN) {
            if (target == Role.ADMIN) {
                throw new IllegalArgumentException("Admin chi nhánh không được tạo tài khoản Admin");
            }
            if (caller.getBranchId() == null) {
                throw new IllegalArgumentException("Tài khoản Admin chưa được gán chi nhánh");
            }
            branchId = caller.getBranchId(); // ép theo chi nhánh của Admin
        } else {
            throw new IllegalArgumentException("Bạn không có quyền tạo tài khoản");
        }

        Account savedAccount = accountRepository.save(Account.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(target)
                .status(true)
                .branchId(branchId)
                .build());

        String workStatus = request.getWorkStatus() != null ? request.getWorkStatus() : "ACTIVE";

        return switch (target) {
            case ADMIN -> {
                Admin saved = adminRepository.save(Admin.builder()
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
                Staff saved = staffRepository.save(Staff.builder()
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
                Teacher saved = teacherRepository.save(Teacher.builder()
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
            default -> throw new IllegalArgumentException("Vai trò không hợp lệ");
        };
    }

    // ===================== DANH SÁCH / KHÓA MỞ / ME =====================
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers(String callerUsername) {
        Account caller = getCaller(callerUsername);

        List<Account> accounts;
        if (caller.getRole() == Role.SUPER_ADMIN) {
            accounts = accountRepository.findAll();
        } else {
            if (caller.getBranchId() == null) {
                throw new IllegalArgumentException("Tài khoản chưa được gán chi nhánh");
            }
            accounts = accountRepository.findByBranchId(caller.getBranchId());
        }

        return accounts.stream()
                .map(this::getUserResponseByAccount)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse toggleStatus(Long id, String callerUsername) {
        Account caller = getCaller(callerUsername);
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));

        if (account.getRole() == Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("Không thể khóa tài khoản Super Admin");
        }
        if (caller.getRole() == Role.ADMIN) {
            boolean sameBranch = caller.getBranchId() != null
                    && caller.getBranchId().equals(account.getBranchId());
            if (!sameBranch || account.getRole() == Role.ADMIN) {
                throw new IllegalArgumentException("Bạn không có quyền thay đổi tài khoản này");
            }
        }

        account.setStatus(!account.getStatus());
        return getUserResponseByAccount(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public UserResponse getByUsername(String username) {
        return getUserResponseByAccount(getCaller(username));
    }

    // ===================== HÀM PHỤ =====================
    private Account getCaller(String username) {
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));
    }

    private ProfileInfo resolveProfile(Account account) {
        Long id = account.getId();
        return switch (account.getRole()) {
            case SUPER_ADMIN -> superAdminRepository.findByAccountId(id)
                    .map(p -> new ProfileInfo(p.getId(), p.getFullName()))
                    .orElse(new ProfileInfo(null, null));
            case ADMIN -> adminRepository.findByAccountId(id)
                    .map(p -> new ProfileInfo(p.getId(), p.getFullName()))
                    .orElse(new ProfileInfo(null, null));
            case STAFF -> staffRepository.findByAccountId(id)
                    .map(p -> new ProfileInfo(p.getId(), p.getFullName()))
                    .orElse(new ProfileInfo(null, null));
            case TEACHER -> teacherRepository.findByAccountId(id)
                    .map(p -> new ProfileInfo(p.getId(), p.getFullName()))
                    .orElse(new ProfileInfo(null, null));
        };
    }

    private UserResponse getUserResponseByAccount(Account account) {
        Long id = account.getId();
        return switch (account.getRole()) {
            case SUPER_ADMIN -> toSuperAdminResponse(account, superAdminRepository.findByAccountId(id).orElse(null));
            case ADMIN -> toAdminResponse(account, adminRepository.findByAccountId(id).orElse(null));
            case STAFF -> toStaffResponse(account, staffRepository.findByAccountId(id).orElse(null));
            case TEACHER -> toTeacherResponse(account, teacherRepository.findByAccountId(id).orElse(null));
        };
    }

    private UserResponse.UserResponseBuilder baseResponse(Account account) {
        return UserResponse.builder()
                .accountId(account.getId())
                .username(account.getUsername())
                .role(account.getRole())
                .status(account.getStatus())
                .branchId(account.getBranchId())
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