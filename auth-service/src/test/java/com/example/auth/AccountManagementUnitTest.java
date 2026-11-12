package com.example.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.auth.common.ApiException;
import com.example.auth.dto.CreateUserRequest;
import com.example.auth.entity.*;
import com.example.auth.integration.BranchDirectory;
import com.example.auth.repository.*;
import com.example.auth.security.JwtUtil;
import com.example.auth.service.AuthService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

class AccountManagementUnitTest {
  AccountRepository accounts;
  BranchDirectory branches;
  AuthService service;

  @BeforeEach
  void setup() {
    accounts = mock(AccountRepository.class);
    branches = mock(BranchDirectory.class);
    service =
        new AuthService(
            accounts,
            mock(SuperAdminRepository.class),
            mock(AdminRepository.class),
            mock(StaffRepository.class),
            mock(TeacherRepository.class),
            mock(PasswordEncoder.class),
            mock(JwtUtil.class),
            mock(AuthenticationManager.class),
            branches, mock(com.example.auth.student.StudentAccountService.class));
    when(accounts.findByUsername("superadmin"))
        .thenReturn(
            Optional.of(
                Account.builder()
                    .id(1L)
                    .username("superadmin")
                    .role(Role.SUPER_ADMIN)
                    .status(true)
                    .build()));
  }

  @Test
  void missingBranchIsBusinessBadRequestAndDoesNotSave() {
    var request =
        CreateUserRequest.builder()
            .username("newstaff")
            .password("password123")
            .role(Role.STAFF)
            .fullName("Staff")
            .build();
    var error =
        assertThrows(ApiException.class, () -> service.createAccount(request, "superadmin"));
    assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
    verify(accounts, never()).save(any());
    verifyNoInteractions(branches);
  }

  @Test
  void creatingSuperAdminThroughApiIsRejectedAsBusinessError() {
    var request =
        CreateUserRequest.builder()
            .username("newsuper")
            .password("password123")
            .role(Role.SUPER_ADMIN)
            .fullName("Super")
            .build();
    assertEquals(
        HttpStatus.BAD_REQUEST,
        assertThrows(ApiException.class, () -> service.createAccount(request, "superadmin"))
            .getStatus());
    verify(accounts, never()).save(any());
  }

  @Test
  void superAdminCannotBeDisabled() {
    Account superAdmin = accounts.findByUsername("superadmin").orElseThrow();
    when(accounts.lock(1L)).thenReturn(Optional.of(superAdmin));
    assertEquals(
        HttpStatus.BAD_REQUEST,
        assertThrows(ApiException.class, () -> service.toggleStatus(1L, "superadmin")).getStatus());
    verify(accounts, never()).save(any());
  }

  @Test
  void adminCannotDisableAnotherBranch() {
    when(accounts.findByUsername("admin"))
        .thenReturn(
            Optional.of(
                Account.builder().id(2L).role(Role.ADMIN).status(true).branchId(11L).build()));
    Account admin = accounts.findByUsername("admin").orElseThrow();
    var membership = new AccountBranch();
    membership.setAccount(admin);
    membership.setBranchId(11L);
    membership.setStatus(true);
    admin.getBranchAssignments().add(membership);
    when(accounts.lock(3L))
        .thenReturn(
            Optional.of(
                Account.builder().id(3L).role(Role.STAFF).status(true).branchId(12L).build()));
    assertEquals(
        HttpStatus.NOT_FOUND,
        assertThrows(ApiException.class, () -> service.toggleStatus(3L, "admin")).getStatus());
    verify(accounts, never()).save(any());
  }
}
