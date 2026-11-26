package com.example.auth;

import static org.junit.jupiter.api.Assertions.*;

import com.example.auth.common.ApiException;
import com.example.auth.company.*;
import com.example.auth.entity.*;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = "spring.datasource.url=jdbc:h2:mem:company-support;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class CompanySupportTest {
  @Autowired EntityManager em;
  @Autowired CompanyService company;
  @Autowired InternalSupportService support;
  @Autowired JdbcTemplate jdbc;
  @MockitoBean com.example.auth.integration.BranchDirectory branches;
  Account sa, admin, staff, other, teacher, student;

  Account account(String name, Role role, long branch) {
    var a =
        Account.builder()
            .username(name)
            .password("PRIVATE_HASH")
            .personnelCode(
                role == Role.STUDENT || role == Role.SUPER_ADMIN ? null : name.toUpperCase())
            .role(role)
            .status(true)
            .branchId(branch)
            .build();
    em.persist(a);
    if (role != Role.STUDENT && role != Role.SUPER_ADMIN) {
      var b = new AccountBranch();
      b.setAccount(a);
      b.setBranchId(branch);
      b.setStatus(true);
      b.setAssignedAt(LocalDateTime.now());
      a.getBranchAssignments().add(b);
      em.persist(b);
    }
    return a;
  }

  @BeforeEach
  void setup() {
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS branches(id BIGINT PRIMARY KEY,code VARCHAR(20),name"
            + " VARCHAR(120),address VARCHAR(120),phone VARCHAR(20),status VARCHAR(20))");
    jdbc.update(
        "MERGE INTO branches KEY(id) VALUES(1,'CN01','One','Address','0900000000','ACTIVE')");
    sa = account("sa", Role.SUPER_ADMIN, 1);
    em.persist(SuperAdmin.builder().account(sa).fullName("Director").build());
    admin = account("admin", Role.ADMIN, 1);
    em.persist(Admin.builder().account(admin).fullName("Manager").build());
    staff = account("staff", Role.STAFF, 1);
    em.persist(
        Staff.builder()
            .account(staff)
            .fullName("Staff")
            .email("PRIVATE_EMAIL")
            .phone("PRIVATE_PHONE")
            .build());
    other = account("other", Role.STAFF, 2);
    em.persist(Staff.builder().account(other).fullName("Other").build());
    teacher = account("teacher", Role.TEACHER, 1);
    em.persist(
        Teacher.builder()
            .account(teacher)
            .fullName("Teacher")
            .qualification("PRIVATE_DEGREE")
            .build());
    student = account("student", Role.STUDENT, 1);
    em.flush();
  }

  @Test
  void directoryIsSharedButContainsNoPrivateProfileFields() {
    var result = company.directory(teacher.getUsername(), null, null, null, null, 0, 20);
    assertEquals(5, result.totalElements());
    String content = result.toString();
    assertFalse(content.contains("PRIVATE_"));
    assertFalse(content.contains("username="));
    assertThrows(
        ApiException.class,
        () -> company.directory(student.getUsername(), null, null, null, null, 0, 20));
  }

  @Test
  void unitsRequireSuperadminAndCannotCycle() {
    var root =
        company.unit(
            null,
            new CompanyService.UnitRequest("Center", null, null, true, null),
            sa.getUsername());
    var child =
        company.unit(
            null,
            new CompanyService.UnitRequest("Department", root.getId(), null, true, null),
            sa.getUsername());
    assertThrows(
        ApiException.class,
        () ->
            company.unit(
                root.getId(),
                new CompanyService.UnitRequest(
                    "Center", child.getId(), null, true, root.getVersion()),
                sa.getUsername()));
    assertThrows(
        ApiException.class,
        () ->
            company.unit(
                null,
                new CompanyService.UnitRequest("Bad", null, null, true, null),
                admin.getUsername()));
  }

  @Test
  void assignmentDoesNotChangeRoleAndRequiresBranchMembership() {
    var unit =
        company.unit(
            null, new CompanyService.UnitRequest("One", null, 1L, true, null), sa.getUsername());
    var title =
        company.title(null, new CompanyService.TitleRequest("CEO", true, null), sa.getUsername());
    company.assign(
        null,
        new CompanyService.AssignmentRequest(
            staff.getId(), unit.getId(), title.getId(), null, true, null),
        sa.getUsername());
    assertEquals(Role.STAFF, staff.getRole());
    assertThrows(
        ApiException.class,
        () ->
            company.assign(
                null,
                new CompanyService.AssignmentRequest(
                    other.getId(), unit.getId(), title.getId(), null, true, null),
                sa.getUsername()));
  }

  InternalTicket ticket() {
    return support.create(
        new InternalSupportService.Create("BUG", "Schedule", "Cannot view schedule", 1L),
        staff.getUsername());
  }

  @Test
  void ownershipAndQueueNeverLeakAcrossBranches() {
    var t = ticket();
    assertThrows(ApiException.class, () -> support.detail(t.getId(), other.getUsername()));
    assertThrows(ApiException.class, () -> support.list(teacher.getUsername(), true, null, 0, 20));
    assertEquals(1, support.list(admin.getUsername(), true, null, 0, 20).totalElements());
    assertEquals(0, support.list(other.getUsername(), false, null, 0, 20).totalElements());
    assertThrows(
        ApiException.class,
        () ->
            support.create(
                new InternalSupportService.Create("BUG", "Other", "Unauthorized branch", 2L),
                staff.getUsername()));
  }

  @Test
  void authorCannotResolveButManagerCanAndStaleReplyFails() {
    var t = ticket();
    assertThrows(
        ApiException.class,
        () ->
            support.reply(
                t.getId(),
                new InternalSupportService.Reply(t.getVersion(), "Done", "RESOLVED"),
                staff.getUsername()));
    long old = t.getVersion();
    support.reply(
        t.getId(),
        new InternalSupportService.Reply(old, "Investigating", "IN_PROGRESS"),
        admin.getUsername());
    assertEquals("IN_PROGRESS", t.getStatus());
    assertThrows(
        ApiException.class,
        () ->
            support.reply(
                t.getId(),
                new InternalSupportService.Reply(old, "Stale", null),
                staff.getUsername()));
  }

  @Test
  void globalIssuesRemainPrivateFromBranchAdmin() {
    var t =
        support.create(
            new InternalSupportService.Create("ACCOUNT", "Account", "Need help", null),
            teacher.getUsername());
    assertThrows(ApiException.class, () -> support.detail(t.getId(), admin.getUsername()));
    assertDoesNotThrow(() -> support.detail(t.getId(), sa.getUsername()));
  }

  @Test
  void organizationVersionProtectsConcurrentEdits() {
    var t =
        company.title(
            null, new CompanyService.TitleRequest("Teacher", true, null), sa.getUsername());
    assertThrows(
        ApiException.class,
        () ->
            company.title(
                t.getId(),
                new CompanyService.TitleRequest("Overwrite", true, 99L),
                sa.getUsername()));
  }

  @Test
  void activeAssignmentsProtectUnitsAndTitlesFromDeactivation() {
    var u=company.unit(null,new CompanyService.UnitRequest("Team",null,null,true,null),sa.getUsername());
    var t=company.title(null,new CompanyService.TitleRequest("Lead",true,null),sa.getUsername());
    company.assign(null,new CompanyService.AssignmentRequest(staff.getId(),u.getId(),t.getId(),null,true,null),sa.getUsername());
    assertThrows(ApiException.class,()->company.unit(u.getId(),new CompanyService.UnitRequest("Team",null,null,false,u.getVersion()),sa.getUsername()));
    assertThrows(ApiException.class,()->company.title(t.getId(),new CompanyService.TitleRequest("Lead",false,t.getVersion()),sa.getUsername()));
  }

  @Test
  void managementCyclesAreRejected() {
    var u=company.unit(null,new CompanyService.UnitRequest("Team",null,null,true,null),sa.getUsername());
    var t=company.title(null,new CompanyService.TitleRequest("Lead",true,null),sa.getUsername());
    company.assign(null,new CompanyService.AssignmentRequest(staff.getId(),u.getId(),t.getId(),teacher.getId(),true,null),sa.getUsername());
    assertThrows(ApiException.class,()->company.assign(null,new CompanyService.AssignmentRequest(teacher.getId(),u.getId(),t.getId(),staff.getId(),true,null),sa.getUsername()));
  }
}
