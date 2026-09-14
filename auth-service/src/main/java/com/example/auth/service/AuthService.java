package com.example.auth.service;

import com.example.auth.dto.*;
import com.example.auth.entity.*;
import com.example.auth.repository.AccountRepository;
import com.example.auth.repository.AdminRepository;
import com.example.auth.repository.StaffRepository;
import com.example.auth.repository.TeacherRepository;
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
    private final AdminRepository adminRepository;
    private final StaffRepository staffRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

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

        // Cập nhật lastLogin
        account.setLastLogin(LocalDateTime.now());
        accountRepository.save(account);

        // Lấy thông tin Profile theo Role
        Long profileId = null;
        String fullName = null;

        if (account.getRole() == Role.ADMIN) {
            Admin admin = adminRepository.findByAccountId(account.getId()).orElse(null);
            if (admin != null) {
                profileId = admin.getId();
                fullName = admin.getFullName();
            }
        } else if (account.getRole() == Role.STAFF) {
            Staff staff = staffRepository.findByAccountId(account.getId()).orElse(null);
            if (staff != null) {
                profileId = staff.getId();
                fullName = staff.getFullName();
            }
        } else if (account.getRole() == Role.TEACHER) {
            Teacher teacher = teacherRepository.findByAccountId(account.getId()).orElse(null);
            if (teacher != null) {
                profileId = teacher.getId();
                fullName = teacher.getFullName();
            }
        }

        String token = jwtUtil.generateToken(account.getUsername(), account.getRole().name(), account.getId(), profileId);

        return LoginResponse.builder()
                .token(token)
                .accountId(account.getId())
                .profileId(profileId)
                .username(account.getUsername())
                .role(account.getRole().name())
                .fullName(fullName)
                .build();
    }

    @Transactional
    public UserResponse createAccount(CreateUserRequest request) {
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username đã tồn tại");
        }
        if (request.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Không thể tự tạo tài khoản Admin qua API này");
        }

        // Bước 1: Lưu Account
        Account account = Account.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .status(true)
                .build();
        Account savedAccount = accountRepository.save(account);

        // Bước 2: Lưu Staff hoặc Teacher tương ứng
        if (request.getRole() == Role.STAFF) {
            Staff staff = Staff.builder()
                    .account(savedAccount)
                    .fullName(request.getFullName())
                    .email(request.getEmail())
                    .phone(request.getPhone())
                    .build();
            Staff savedStaff = staffRepository.save(staff);
            return toStaffResponse(savedAccount, savedStaff);
        } else if (request.getRole() == Role.TEACHER) {
            Teacher teacher = Teacher.builder()
                    .account(savedAccount)
                    .fullName(request.getFullName())
                    .email(request.getEmail())
                    .phone(request.getPhone())
                    .specialization(request.getSpecialization())
                    .qualification(request.getQualification())
                    .experience(request.getExperience())
                    .certificate(request.getCertificate())
                    .hireDate(request.getHireDate())
                    .workStatus(request.getWorkStatus() != null ? request.getWorkStatus() : "ACTIVE")
                    .build();
            Teacher savedTeacher = teacherRepository.save(teacher);
            return toTeacherResponse(savedAccount, savedTeacher);
        }

        throw new IllegalArgumentException("Vai trò không hợp lệ");
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return accountRepository.findAll().stream()
                .map(this::getUserResponseByAccount)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse toggleStatus(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));
        account.setStatus(!account.getStatus());
        Account saved = accountRepository.save(account);
        return getUserResponseByAccount(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getByUsername(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));
        return getUserResponseByAccount(account);
    }

    private UserResponse getUserResponseByAccount(Account account) {
        if (account.getRole() == Role.ADMIN) {
            Admin admin = adminRepository.findByAccountId(account.getId()).orElse(null);
            return toAdminResponse(account, admin);
        } else if (account.getRole() == Role.STAFF) {
            Staff staff = staffRepository.findByAccountId(account.getId()).orElse(null);
            return toStaffResponse(account, staff);
        } else if (account.getRole() == Role.TEACHER) {
            Teacher teacher = teacherRepository.findByAccountId(account.getId()).orElse(null);
            return toTeacherResponse(account, teacher);
        }

        return UserResponse.builder()
                .accountId(account.getId())
                .username(account.getUsername())
                .role(account.getRole())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .lastLogin(account.getLastLogin())
                .build();
    }

    private UserResponse toAdminResponse(Account account, Admin admin) {
        return UserResponse.builder()
                .accountId(account.getId())
                .profileId(admin != null ? admin.getId() : null)
                .username(account.getUsername())
                .role(account.getRole())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .lastLogin(account.getLastLogin())
                .fullName(admin != null ? admin.getFullName() : null)
                .email(admin != null ? admin.getEmail() : null)
                .phone(admin != null ? admin.getPhone() : null)
                .build();
    }

    private UserResponse toStaffResponse(Account account, Staff staff) {
        return UserResponse.builder()
                .accountId(account.getId())
                .profileId(staff != null ? staff.getId() : null)
                .username(account.getUsername())
                .role(account.getRole())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .lastLogin(account.getLastLogin())
                .fullName(staff != null ? staff.getFullName() : null)
                .email(staff != null ? staff.getEmail() : null)
                .phone(staff != null ? staff.getPhone() : null)
                .build();
    }

    private UserResponse toTeacherResponse(Account account, Teacher teacher) {
        return UserResponse.builder()
                .accountId(account.getId())
                .profileId(teacher != null ? teacher.getId() : null)
                .username(account.getUsername())
                .role(account.getRole())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .lastLogin(account.getLastLogin())
                .fullName(teacher != null ? teacher.getFullName() : null)
                .email(teacher != null ? teacher.getEmail() : null)
                .phone(teacher != null ? teacher.getPhone() : null)
                .specialization(teacher != null ? teacher.getSpecialization() : null)
                .qualification(teacher != null ? teacher.getQualification() : null)
                .experience(teacher != null ? teacher.getExperience() : null)
                .certificate(teacher != null ? teacher.getCertificate() : null)
                .hireDate(teacher != null ? teacher.getHireDate() : null)
                .workStatus(teacher != null ? teacher.getWorkStatus() : null)
                .build();
    }
}