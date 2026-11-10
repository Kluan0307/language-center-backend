package com.example.academic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.academic.common.ApiException;
import com.example.academic.common.RemoteApi;
import com.example.academic.security.TeacherDirectory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

class TeacherMembershipTest {
  @Test
  void teacherCanTeachAtAssignedBranchOtherThanPrimary() {
    var api = mock(RemoteApi.class);
    var directory = new TeacherDirectory(api, mock(JdbcTemplate.class));
    when(api.get(anyString(), eq(TeacherDirectory.Teacher.class)))
        .thenReturn(new TeacherDirectory.Teacher(7L, 8L, "Teacher", 11L, List.of(11L, 12L)));
    assertEquals(7L, directory.require(7L, 12L).profileId());
    assertThrows(ApiException.class, () -> directory.require(7L, 13L));
  }

  @Test
  void missingTeacherLockReturnsNotFound() {
    var jdbc = mock(JdbcTemplate.class);
    when(jdbc.queryForObject(anyString(), eq(Long.class), eq(999L)))
        .thenThrow(new EmptyResultDataAccessException(1));
    var directory = new TeacherDirectory(mock(RemoteApi.class), jdbc);
    assertEquals(
        org.springframework.http.HttpStatus.NOT_FOUND,
        assertThrows(ApiException.class, () -> directory.lock(999L)).getStatus());
  }
}
