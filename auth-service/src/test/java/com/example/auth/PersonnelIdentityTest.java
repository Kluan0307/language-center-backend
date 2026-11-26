package com.example.auth;

import static org.junit.jupiter.api.Assertions.*;

import com.example.auth.dto.*;
import com.example.auth.entity.*;
import com.example.auth.integration.BranchDirectory;
import com.example.auth.service.AuthService;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:personnel-identity;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class PersonnelIdentityTest {
  @Autowired EntityManager em;
  @Autowired AuthService auth;
  @Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;
  @MockitoBean BranchDirectory branches;

  Account manager() {
    var a =
        Account.builder()
            .username("identity-sa")
            .password(passwords.encode("identity-pass"))
            .role(Role.SUPER_ADMIN)
            .status(true)
            .build();
    em.persist(a);
    var p = SuperAdmin.builder().account(a).fullName("Manager").build();
    em.persist(p);
    em.flush();
    return a;
  }

  @Test
  void accountCreationSharesNumbersAndPreservesCodesWhenLocked() {
    var caller = manager();
    var codes = new HashSet<String>();
    for (var role : List.of(Role.ADMIN, Role.STAFF, Role.TEACHER)) {
      var r =
          CreateUserRequest.builder()
              .username("identity-" + role)
              .password("password123")
              .role(role)
              .fullName("Personnel")
              .branchId(1L)
              .build();
      var created = auth.createAccount(r, caller.getUsername());
      assertTrue(
          created.getPersonnelCode().matches(role == Role.TEACHER ? "GV[0-9]{3,}" : "NS[0-9]{3,}"));
      assertTrue(codes.add(created.getPersonnelCode()));
      var changed = auth.toggleStatus(created.getAccountId(), caller.getUsername());
      assertEquals(created.getPersonnelCode(), changed.getPersonnelCode());
      assertEquals(created.getProfileId(), changed.getProfileId());
    }
  }

  @Test
  void roleCannotBeChangedWithoutMigratingItsProfile() {
    var a = manager();
    a.setRole(Role.STAFF);
    assertThrows(IllegalStateException.class, () -> em.flush());
  }

  @Test
  void duplicateRoleProfilesCannotLogin() {
    var a = manager();
    em.persist(Staff.builder().account(a).fullName("Wrong extra profile").build());
    em.flush();
    var request = new LoginRequest();
    request.setUsername(a.getUsername());
    request.setPassword("identity-pass");
    assertThrows(com.example.auth.common.ApiException.class, () -> auth.login(request));
  }

  @Test
  void issuedNumbersAreDistinctAndOnlyPersonnelRolesAreAccepted() {
    var first = new PersonnelCodeNumber();
    em.persist(first);
    var next = new PersonnelCodeNumber();
    em.persist(next);
    assertNotEquals(first.code(Role.STAFF), next.code(Role.STAFF));
    assertThrows(IllegalArgumentException.class, () -> next.code(Role.STUDENT));
  }
}
