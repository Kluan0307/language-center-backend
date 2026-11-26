package com.example.academic.assistant;

import com.example.academic.common.ApiException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/** Canonical catalog filters. Never infer audience from a name, age or admission level. */
@Component
@RequiredArgsConstructor
public class ClassificationFilters {
  private final NamedParameterJdbcTemplate jdbc;
  private static final Set<String> PROGRAMS = Set.of("GE", "COMMUNICATION", "IELTS", "TOEIC");
  private static final Set<String> AUDIENCES =
      Set.of("PRESCHOOL", "PRIMARY", "SECONDARY", "HIGH_SCHOOL", "ADULT");

  public void validate(String program, String audience) {
    if (program != null && !PROGRAMS.contains(program)
        || audience != null && !AUDIENCES.contains(audience))
      throw ApiException.bad("Chương trình hoặc đối tượng không hợp lệ");
  }

  public Long id(String code, boolean program) {
    validate(program ? code : null, program ? null : code);
    if (code == null) return null;
    var ids = jdbc.queryForList(
        "select id from " + (program ? "programs" : "audience_groups") + " where code=:code",
        Map.of("code", code), Long.class);
    // A valid but not seeded catalog code means no matches, never an unfiltered query.
    return ids.isEmpty() ? Long.MAX_VALUE : ids.getFirst();
  }

  public String sql(Map<String, Object> params, String alias, String program, String audience) {
    validate(program, audience);
    String where = "";
    if (program != null) {
      params.put("classificationProgram", program);
      where += " and exists(select 1 from programs cp where cp.id=" + alias
          + ".program_id and cp.code=:classificationProgram)";
    }
    if (audience != null) {
      params.put("classificationAudience", audience);
      where += " and exists(select 1 from course_audiences ca join audience_groups ag"
          + " on ag.id=ca.audience_group_id where ca.course_id=" + alias
          + ".id and ag.code=:classificationAudience)";
    }
    return where;
  }

  public Map<Long, String> programs() {
    var result = new HashMap<Long, String>();
    jdbc.query("select id,code from programs", Map.of(), rs -> {
      result.put(rs.getLong("id"), rs.getString("code"));
    });
    return result;
  }
}
