package com.example.academic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.academic.branch.BranchService;
import com.example.academic.common.ApiException;
import com.example.academic.course.*;
import com.example.academic.schoolclass.SchoolClassRepository;
import com.example.academic.security.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CourseWriteRulesTest {
  CourseRepository repository;
  BranchService branches;
  BranchScope scope;
  EntityManager entities;
  CourseService service;

  @BeforeEach
  void setup() {
    repository = mock(CourseRepository.class);
    branches = mock(BranchService.class);
    scope = mock(BranchScope.class);
    entities = mock(EntityManager.class);
    service =
        new CourseService(repository,mock(com.example.academic.duration.DurationPlanService.class), scope, branches, mock(SchoolClassRepository.class), entities);
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  private void actor(Role role) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, 1L, 11L, role, "user"), null, List.of()));
  }

  private CourseDto.Request request() {
    return new CourseDto.Request(
        "C", "Course", null, null, 24, new BigDecimal("100"), null, null, 20, 11L);
  }

  private Course course() {
    Course c = new Course();
    c.setId(1L);
    c.setBranchId(11L);
    c.setTuitionFee(new BigDecimal("100"));
    when(repository.findById(1L)).thenReturn(Optional.of(c));
    when(repository.lock(1L)).thenReturn(Optional.of(c));
    when(repository.save(any(Course.class))).thenAnswer(call -> call.getArgument(0));
    return c;
  }

  @Test
  void inactiveBranchPreventsCourseCreation() {
    actor(Role.SUPER_ADMIN);
    when(scope.filter(11L)).thenReturn(11L);
    doThrow(ApiException.bad("Branch is inactive")).when(branches).lock(11L);
    assertEquals(
        HttpStatus.BAD_REQUEST,
        assertThrows(ApiException.class, () -> service.save(null, request())).getStatus());
    verify(repository, never()).save(any());
  }

  @Test
  void staffCannotRestoreAStalePriceAfterAdminChangesItWhileWaitingForLock() {
    actor(Role.STAFF);
    Course c = course();
    doAnswer(
            call -> {
              c.setTuitionFee(new BigDecimal("200"));
              return null;
            })
        .when(entities)
        .refresh(c, LockModeType.PESSIMISTIC_WRITE);
    assertEquals(
        HttpStatus.FORBIDDEN,
        assertThrows(ApiException.class, () -> service.save(1L, request())).getStatus());
    assertEquals(new BigDecimal("200"), c.getTuitionFee());
    verify(repository, never()).save(any());
  }

  @Test
  void courseWritesAcquireBranchBeforeCourseLikeClassCreation() {
    actor(Role.ADMIN);
    course();
    assertEquals("Course", service.save(1L, request()).name());
    var order = inOrder(branches, repository);
    order.verify(branches).lock(11L);
    order.verify(repository).lock(1L);
  }
}
