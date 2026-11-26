package com.example.auth;

import static org.junit.jupiter.api.Assertions.*;

import com.example.auth.assistant.PersonnelController;
import com.example.auth.assistant.PersonnelController.*;
import com.example.auth.common.ApiException;
import com.example.auth.entity.*;
import com.example.auth.repository.*;
import jakarta.persistence.EntityManager;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
    properties = "spring.datasource.url=jdbc:h2:mem:personnel-query;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class PersonnelQueryTest {
  @Autowired org.springframework.web.context.WebApplicationContext web;
  @Autowired com.example.auth.security.JwtUtil jwt;
  @Autowired PersonnelController api;
  @Autowired com.example.auth.assistant.OrganizationController organization;
  @Autowired AccountRepository accounts;
  @Autowired TeacherRepository teachers;
  @Autowired EntityManager em;
  @Autowired JsonMapper json;
  Account admin, superadmin, staff, teacher;
  Teacher first, second, outside;

  Account account(Role role, Long... ids) {
    var a =
        Account.builder()
            .username("PQ-" + UUID.randomUUID())
            .password("PRIVATE_PASSWORD")
            .role(role)
            .status(true)
            .branchId(ids.length == 0 ? null : ids[0])
            .build();
    for (var id : ids) {
      var b = new AccountBranch();
      b.setAccount(a);
      b.setBranchId(id);
      b.setStatus(true);
      b.setAssignedAt(LocalDateTime.now());
      a.getBranchAssignments().add(b);
    }
    return accounts.saveAndFlush(a);
  }

  Teacher profile(Account a, String name) {
    var t = new Teacher();
    t.setAccount(a);
    t.setFullName(name);
    t.setQualification("Master");
    t.setCertificate("IELTS 8.0");
    t.setSpecialization("IELTS");
    t.setEmail("PRIVATE_EMAIL");
    t.setPhone("PRIVATE_PHONE");
    t.setWorkStatus("ACTIVE");
    return teachers.saveAndFlush(t);
  }

  Principal actor(Account a) {
    return a::getUsername;
  }

  @BeforeEach
  void seed() {
    admin = account(Role.ADMIN, 1L, 2L);
    superadmin = account(Role.SUPER_ADMIN);
    staff = account(Role.STAFF, 1L);
    teacher = account(Role.TEACHER, 1L, 2L);
    first = profile(teacher, "An");
    second = profile(account(Role.TEACHER, 1L), "An");
    outside = profile(account(Role.TEACHER, 3L), "Outside");
  }

  Map<?, ?> map(Object value) {
    return json.readValue(json.writeValueAsString(value), Map.class);
  }

  Map<?, ?> data(Object value) {
    return (Map<?, ?>) map(value).get("data");
  }

  @Test
  void countsDistinctAcrossAssignmentsNotMembershipRows() {
    var result = data(api.statistics(actor(admin), Mode.ALL_ASSIGNED, null, Kind.TEACHER, false));
    assertEquals(2, ((Number) result.get("value")).intValue());
  }

  @Test
  void globalCountAndEmptyBranchKeepZero() {
    assertEquals(
        3,
        ((Number)
                data(api.statistics(
                        actor(superadmin), Mode.WHOLE_SYSTEM, null, Kind.TEACHER, false))
                    .get("value"))
            .intValue());
    assertEquals(
        0,
        ((Number)
                data(api.statistics(
                        actor(superadmin), Mode.ACTIVE_BRANCH, 99L, Kind.TEACHER, false))
                    .get("value"))
            .intValue());
  }

  @Test
  void assignmentRevocationChangesCount() {
    teacher.getBranchAssignments().getFirst().setStatus(false);
    accounts.saveAndFlush(teacher);
    assertEquals(
        1,
        ((Number)
                data(api.statistics(actor(admin), Mode.ACTIVE_BRANCH, 1L, Kind.TEACHER, false))
                    .get("value"))
            .intValue());
  }

  @Test
  void activeCountExcludesDisabledOrInactive() {
    second.setWorkStatus("INACTIVE");
    teachers.saveAndFlush(second);
    assertEquals(
        1,
        ((Number)
                data(api.statistics(actor(admin), Mode.ACTIVE_BRANCH, 1L, Kind.TEACHER, true))
                    .get("value"))
            .intValue());
    teacher.setStatus(false);
    accounts.saveAndFlush(teacher);
    assertEquals(
        0,
        ((Number)
                data(api.statistics(actor(admin), Mode.ACTIVE_BRANCH, 1L, Kind.TEACHER, true))
                    .get("value"))
            .intValue());
  }

  @Test
  void pagingDoesNotChangeTotalAndDuplicateNamesRemainSeparate() {
    var result =
        data(
            api.search(
                actor(admin), Mode.ACTIVE_BRANCH, 1L, Kind.TEACHER, "An", null, false, 0, 1));
    assertEquals(2, ((Number) result.get("totalElements")).intValue());
    assertEquals(1, ((List<?>) result.get("content")).size());
  }

  @Test
  void managerGetsQualificationsWithoutPrivateFields() {
    var result =
        data(api.profile(actor(admin), Mode.ACTIVE_BRANCH, 1L, Kind.TEACHER, first.getId()));
    assertEquals("Master", result.get("qualification"));
    assertEquals("IELTS 8.0", result.get("certificate"));
    assertEquals("IELTS", result.get("specialization"));
    assertFalse(result.toString().contains("PRIVATE"));
  }

  @Test
  void otherBranchProfileIsNotFound() {
    assertEquals(
        404,
        assertThrows(
                ApiException.class,
                () ->
                    api.profile(
                        actor(admin), Mode.ACTIVE_BRANCH, 1L, Kind.TEACHER, outside.getId()))
            .getStatus()
            .value());
  }

  @Test
  void staffCannotReadQualificationOrStaffDirectory() {
    assertEquals(
        403,
        assertThrows(
                ApiException.class,
                () ->
                    api.profile(actor(staff), Mode.ACTIVE_BRANCH, 1L, Kind.TEACHER, first.getId()))
            .getStatus()
            .value());
    assertThrows(
        ApiException.class,
        () -> api.statistics(actor(staff), Mode.ACTIVE_BRANCH, 1L, Kind.STAFF, true));
    assertThrows(
        ApiException.class,
        () ->
            api.search(
                actor(staff), Mode.ACTIVE_BRANCH, 1L, Kind.TEACHER, null, null, false, 0, 5));
  }

  @Test
  void staffTeacherDirectoryIsMinimal() {
    String result =
        json.writeValueAsString(
            api.search(actor(staff), Mode.ACTIVE_BRANCH, 1L, Kind.TEACHER, null, null, true, 0, 5));
    assertFalse(result.contains("qualification"));
    assertFalse(result.contains("PRIVATE"));
  }

  @Test
  void teacherCanOnlyReadSelfAndOwnAssignments() {
    var result = data(api.mine(actor(teacher), Mode.ACTIVE_BRANCH, 1L));
    assertEquals(first.getId().longValue(), ((Number) result.get("id")).longValue());
    assertEquals(2, ((List<?>) result.get("branchIds")).size());
    assertThrows(
        ApiException.class,
        () ->
            api.search(
                actor(teacher), Mode.ACTIVE_BRANCH, 1L, Kind.TEACHER, null, null, true, 0, 5));
    assertThrows(
        ApiException.class,
        () -> api.statistics(actor(teacher), Mode.ACTIVE_BRANCH, 1L, Kind.TEACHER, true));
  }

  @Test
  void managerCannotSeeUnassignedBranchesInProjection() {
    teacher.getBranchAssignments().getLast().setBranchId(3L);
    accounts.saveAndFlush(teacher);
    assertEquals(
        List.of(1),
        data(api.profile(actor(admin), Mode.ACTIVE_BRANCH, 1L, Kind.TEACHER, first.getId()))
            .get("branchIds"));
  }

  @Test
  void staffProfilesRemainDistinctFromTeacherIds() {
    var s = new Staff();
    s.setAccount(staff);
    s.setFullName("Staff");
    s.setPosition("Tư vấn");
    s.setWorkStatus("ACTIVE");
    em.persist(s);
    em.flush();
    assertEquals(
        1,
        ((Number)
                data(api.statistics(actor(admin), Mode.ACTIVE_BRANCH, 1L, Kind.STAFF, false))
                    .get("value"))
            .intValue());
    assertEquals(
        "Tư vấn",
        data(api.profile(actor(admin), Mode.ACTIVE_BRANCH, 1L, Kind.STAFF, s.getId()))
            .get("position"));
  }

  @Test
  void invalidScopeAndSizeAreRejected() {
    assertThrows(
        ApiException.class,
        () ->
            api.search(actor(admin), Mode.ACTIVE_BRANCH, 3L, Kind.TEACHER, null, null, true, 0, 5));
    assertThrows(
        ApiException.class,
        () ->
            api.search(
                actor(admin), Mode.ACTIVE_BRANCH, 1L, Kind.TEACHER, null, null, true, 0, 21));
    assertThrows(
        ApiException.class,
        () -> api.statistics(actor(admin), Mode.WHOLE_SYSTEM, null, Kind.TEACHER, true));
  }

  @Test
  void accountStatisticsCountDistinctMembershipsAndRestrictAdminRoles() {
    var result =
        map(organization.statistics(actor(admin), Mode.ALL_ASSIGNED, null, "TEACHER", null));
    var rows = (List<?>) ((Map<?, ?>) result.get("data")).get("content");
    assertEquals(2, ((Number) ((Map<?, ?>) rows.getFirst()).get("value")).intValue());
    assertThrows(
        ApiException.class,
        () -> organization.statistics(actor(admin), Mode.ACTIVE_BRANCH, 1L, "ADMIN", null));
    assertThrows(
        ApiException.class,
        () -> organization.statistics(actor(staff), Mode.ACTIVE_BRANCH, 1L, null, null));
  }

  @Test
  void lockedAccountDirectoryOmitsSecretsAndOtherBranchAssignments() {
    teacher.setStatus(false);
    accounts.saveAndFlush(teacher);
    var result =
        map(
            organization.accounts(
                actor(admin), Mode.ACTIVE_BRANCH, 1L, "TEACHER", false, null, null, 0, 5));
    var content = (List<?>) ((Map<?, ?>) result.get("data")).get("content");
    assertEquals(1, content.size());
    assertEquals(List.of(1), ((Map<?, ?>) content.getFirst()).get("branchIds"));
    assertFalse(result.toString().contains("PRIVATE"));
  }

  @Test
  void adminDirectoryIsSuperAdminOnlyAndNameMatchesProfile() {
    var profile = new Admin();
    profile.setAccount(admin);
    profile.setFullName("Manager An");
    em.persist(profile);
    em.flush();
    var result =
        map(
            organization.accounts(
                actor(superadmin), Mode.WHOLE_SYSTEM, null, "ADMIN", null, "Manager", null, 0, 5));
    assertEquals(1, ((Number) ((Map<?, ?>) result.get("data")).get("totalElements")).intValue());
    assertThrows(
        ApiException.class,
        () ->
            organization.accounts(
                actor(admin), Mode.ACTIVE_BRANCH, 1L, "ADMIN", null, null, null, 0, 5));
  }

  @Test
  void httpContractAuthenticatesAndRejectsStaffDetailAccess() throws Exception {
    var mvc =
        org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(web)
            .addFilters((jakarta.servlet.Filter) web.getBean("springSecurityFilterChain"))
            .build();
    String path = "/api/v1/auth/assistant-query/personnel-statistics";
    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                .isUnauthorized());
    String token = jwt.generateToken(admin.getUsername(), "ADMIN", admin.getId(), null, 1L);
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path)
                .header("Authorization", "Bearer " + token)
                .param("branchId", "1")
                .param("activeOnly", "false"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$.data.value")
                .value(2))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$.scope.mode")
                .value("ACTIVE_BRANCH"));
    String staffToken = jwt.generateToken(staff.getUsername(), "STAFF", staff.getId(), null, 1L);
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/v1/auth/assistant-query/personnel-profile")
                .header("Authorization", "Bearer " + staffToken)
                .param("personnelId", first.getId().toString()))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                .isForbidden());
  }
}
