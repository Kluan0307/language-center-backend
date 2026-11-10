package com.example.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.auth.common.ApiException;
import com.example.auth.dto.*;
import com.example.auth.entity.*;
import com.example.auth.integration.BranchDirectory;
import com.example.auth.repository.*;
import com.example.auth.service.AuthService;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:branch-assignment-tests;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class BranchAssignmentsTest {
  @Autowired AuthService service;
  @Autowired AccountRepository accounts;
  @Autowired AdminRepository admins;
  @Autowired TeacherRepository teachers;
  @MockitoBean BranchDirectory branches;
  Account superadmin, admin;

  Account account(Role role, String name) {
    return accounts.saveAndFlush(
        Account.builder().username(name).password("test-hash").role(role).status(true).build());
  }

  @BeforeEach
  void setup() {
    superadmin = account(Role.SUPER_ADMIN, "super-test");
    admin = account(Role.ADMIN, "admin-test");
    var profile = new Admin();
    profile.setAccount(admin);
    profile.setFullName("Admin");
    admins.save(profile);
  }

  @Test
  void assignmentsPrimaryAndRepeatedRevocationsKeepFullHistory() {
    var first =
        service.assignBranches(
            admin.getId(), new BranchAssignmentsRequest(List.of(11L, 12L), 12L), "super-test");
    assertEquals(List.of(11L, 12L), first.getBranchIds());
    assertEquals(12L, first.getBranchId());
    service.assignBranches(
        admin.getId(), new BranchAssignmentsRequest(List.of(11L), 11L), "super-test");
    assertFalse(
        service.assignments(admin.getId(), "super-test").stream()
            .filter(a -> a.branchId().equals(12L))
            .findFirst()
            .orElseThrow()
            .status());
    service.assignBranches(
        admin.getId(), new BranchAssignmentsRequest(List.of(11L, 12L), 11L), "super-test");
    var history = service.assignmentHistory(admin.getId(), "super-test");
    assertEquals(
        2,
        history.stream()
            .filter(e -> e.branchId().equals(12L) && e.action().equals("ASSIGNED"))
            .count());
    assertEquals(1, history.stream().filter(e -> e.action().equals("REVOKED")).count());
    int count = history.size();
    service.assignBranches(
        admin.getId(), new BranchAssignmentsRequest(List.of(11L, 12L), 11L), "super-test");
    assertEquals(count, service.assignmentHistory(admin.getId(), "super-test").size());
  }

  @Test
  void adminCannotExpandOwnScope() {
    assertThrows(
        ApiException.class,
        () ->
            service.assignBranches(
                admin.getId(), new BranchAssignmentsRequest(List.of(11L), 11L), "admin-test"));
  }

  @Test
  void staffCannotHaveTwoBranches() {
    var staff = account(Role.STAFF, "staff-test");
    assertThrows(
        ApiException.class,
        () ->
            service.assignBranches(
                staff.getId(), new BranchAssignmentsRequest(List.of(11L, 12L), 11L), "super-test"));
    assertTrue(staff.activeBranchIds().isEmpty());
  }

  @Test
  void primaryMustBeAssignedAndDuplicatesAreRejected() {
    assertThrows(
        ApiException.class,
        () ->
            service.assignBranches(
                admin.getId(), new BranchAssignmentsRequest(List.of(11L), 12L), "super-test"));
    assertThrows(
        ApiException.class,
        () ->
            service.assignBranches(
                admin.getId(), new BranchAssignmentsRequest(List.of(11L, 11L), 11L), "super-test"));
  }

  @Test
  void removingAllBranchesClearsPrimaryAndRetainsRevokedRows() {
    service.assignBranches(
        admin.getId(), new BranchAssignmentsRequest(List.of(11L), 11L), "super-test");
    var result =
        service.assignBranches(
            admin.getId(), new BranchAssignmentsRequest(List.of(), null), "super-test");
    assertNull(result.getBranchId());
    assertTrue(result.getBranchIds().isEmpty());
    assertEquals(1, service.assignments(admin.getId(), "super-test").size());
  }

  @Test
  void adminCannotGloballyDisableTeacherSharedWithUnmanagedBranch() {
    service.assignBranches(
        admin.getId(), new BranchAssignmentsRequest(List.of(11L, 12L), 11L), "super-test");
    var teacher = account(Role.TEACHER, "teacher-test");
    service.assignBranches(
        teacher.getId(), new BranchAssignmentsRequest(List.of(11L, 13L), 11L), "super-test");
    assertEquals(
        org.springframework.http.HttpStatus.FORBIDDEN,
        assertThrows(ApiException.class, () -> service.toggleStatus(teacher.getId(), "admin-test"))
            .getStatus());
  }

  @Test
  void adminCanCreateStaffAtSecondAssignedBranch() {
    service.assignBranches(
        admin.getId(), new BranchAssignmentsRequest(List.of(11L, 12L), 11L), "super-test");
    var request =
        CreateUserRequest.builder()
            .username("new-staff")
            .password("test123456")
            .role(Role.STAFF)
            .fullName("New Staff")
            .branchId(12L)
            .build();
    var saved = service.createAccount(request, "admin-test");
    assertEquals(12L, saved.getBranchId());
    assertEquals(List.of(12L), saved.getBranchIds());
    request.setUsername("bad-staff");
    request.setBranchId(13L);
    assertThrows(ApiException.class, () -> service.createAccount(request, "admin-test"));
  }
}
