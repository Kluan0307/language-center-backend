package com.example.academic.stats;

import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StatsRepository {
  private final JdbcTemplate jdbc;

  private Map<String, Long> counts(String sql, Long branch, List<Object> parameters) {
    String where = branch == null ? "" : " where branch_id=?";
    if (branch != null) parameters.add(branch);
    Map<String, Long> result = new LinkedHashMap<>();
    jdbc.query(
        sql + where + " group by 1",
        rs -> {
          result.put(rs.getString(1), rs.getLong(2));
        },
        parameters.toArray());
    return result;
  }

  public Map<String, Long> studentsByStatus(Long branch) {
    return counts("select status,count(*) from students", branch, new ArrayList<>());
  }

  public Map<String, Long> studentsByLevel(Long branch) {
    return counts(
        "select coalesce(nullif(trim(current_level),''),'UNSPECIFIED'),count(*) from students",
        branch,
        new ArrayList<>());
  }

  public Map<String, Long> leadsByStatus(Long branch) {
    return counts("select status,count(*) from leads", branch, new ArrayList<>());
  }

  public Map<String, Long> classesByStatus(Long branch) {
    return counts(
        "select case when status='FINISHED' or (case when session_end_date>end_date then session_end_date else end_date end)<? then 'FINISHED' when start_date>? then"
            + " 'UPCOMING' else 'ONGOING' end,count(*) from school_classes",
        branch,
        new ArrayList<>(
            List.of(
                LocalDate.now(com.example.academic.common.BusinessTime.ZONE),
                LocalDate.now(com.example.academic.common.BusinessTime.ZONE))));
  }

  public long monthlyStudents(Long branch) {
    var parameters =
        new ArrayList<Object>(
            List.of(
                LocalDate.now(com.example.academic.common.BusinessTime.ZONE).withDayOfMonth(1),
                LocalDate.now(com.example.academic.common.BusinessTime.ZONE)
                    .withDayOfMonth(1)
                    .plusMonths(1)));
    if (branch != null) parameters.add(branch);
    return jdbc.queryForObject(
        "select count(*) from students where registered_at>=? and registered_at<?"
            + (branch == null ? "" : " and branch_id=?"),
        Long.class,
        parameters.toArray());
  }

  public long[] occupancy(Long branch) {
    var parameters =
        new ArrayList<Object>(
            List.of(LocalDate.now(com.example.academic.common.BusinessTime.ZONE)));
    if (branch != null) parameters.add(branch);
    return jdbc.queryForObject(
        "select coalesce(sum(current_size),0),coalesce(sum(max_size),0) from school_classes where"
            + " status<>'FINISHED' and (case when session_end_date>end_date then session_end_date else end_date end)>=?"
            + (branch == null ? "" : " and branch_id=?"),
        (rs, row) -> new long[] {rs.getLong(1), rs.getLong(2)},
        parameters.toArray());
  }
}
