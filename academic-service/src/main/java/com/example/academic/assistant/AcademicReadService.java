package com.example.academic.assistant;

import com.example.academic.common.*;
import com.example.academic.course.*;
import com.example.academic.security.*;
import com.example.academic.session.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF','TEACHER')")
public class AcademicReadService {
  private final NamedParameterJdbcTemplate jdbc;
  private final ReadQuery query;
  private final CourseService courses;
  private final SessionPlanner sessions;
  private static final String END =
      "(case when c.session_end_date>c.end_date then c.session_end_date else c.end_date end)";
  // Period end is exclusive. Legacy fallback is only used when no periods exist.
  private static final String EFFECTIVE =
      "(exists(select 1 from enrollment_periods ep where ep.enrollment_id=e.id and"
          + " ep.active_from<=:asOf and (ep.active_until is null or ep.active_until>:asOf)) or (not"
          + " exists(select 1 from enrollment_periods ep where ep.enrollment_id=e.id) and"
          + " e.study_status='STUDYING' and e.enrolled_at<:nextDay))";
  private static final String ACTIVE =
      "e.study_status='STUDYING' and "
          + EFFECTIVE
          + " and c.status<>'FINISHED' and c.start_date<=:asOf and "
          + END
          + ">=:asOf";

  private Map<String, Object> params(ReadQuery.Scope scope) {
    var p = new HashMap<String, Object>();
    p.put("branches", scope.branchIds());
    p.put("asOf", LocalDate.now(BusinessTime.ZONE));
    p.put("nextDay", LocalDate.now(BusinessTime.ZONE).plusDays(1).atStartOfDay());
    return p;
  }

  private String scope(ReadQuery.Scope scope, String column) {
    return scope.mode() == ReadQuery.Mode.WHOLE_SYSTEM
        ? "1=1"
        : scope.branchIds().isEmpty() ? "1=0" : column + " in (:branches)";
  }

  private String teacher(Map<String, Object> p) {
    var u = Users.current();
    if (u.role() != Role.TEACHER) return "";
    if (u.profileId() == null) throw ApiException.forbidden();
    p.put("teacher", u.profileId());
    return " and c.teacher_id=:teacher";
  }

  private void currentDate(LocalDate date) {
    if (date != null && !date.equals(LocalDate.now(BusinessTime.ZONE)))
      throw ApiException.bad("Chưa hỗ trợ ảnh chụp trạng thái lịch sử; asOf phải là hôm nay");
  }

  private String keyword(Map<String, Object> p, String text, String... fields) {
    query.keyword(text);
    if (text == null || text.isBlank()) return "";
    p.put("keyword", query.pattern(text));
    return " and ("
        + String.join(
            " or ",
            Arrays.stream(fields).map(f -> "lower(" + f + ") like :keyword escape '!' ").toList())
        + ")";
  }

  private Map<String, Object> row(ResultSet rs) throws SQLException {
    var m = new LinkedHashMap<String, Object>();
    var meta = rs.getMetaData();
    for (int i = 1; i <= meta.getColumnCount(); i++) {
      String name = meta.getColumnLabel(i).toLowerCase(Locale.ROOT);
      StringBuilder key = new StringBuilder();
      boolean upper = false;
      for (char ch : name.toCharArray()) {
        if (ch == '_') {
          upper = true;
          continue;
        }
        key.append(upper ? Character.toUpperCase(ch) : ch);
        upper = false;
      }
      Object value = rs.getObject(i);
      if (value instanceof java.sql.Date d) value = d.toLocalDate();
      if (value instanceof Timestamp t) value = t.toLocalDateTime();
      if (value instanceof Time t) value = t.toLocalTime();
      m.put(key.toString(), value);
    }
    return m;
  }

  private ReadQuery.Result page(
      ReadQuery.Scope scope,
      String source,
      String select,
      String from,
      String where,
      String order,
      Map<String, Object> p,
      int page,
      int size) {
    query.page(page, size);
    long count = jdbc.queryForObject("select count(*) " + from + " where " + where, p, Long.class);
    p.put("limit", size);
    p.put("offset", page * size);
    var rows =
        jdbc.query(
            "select "
                + select
                + " "
                + from
                + " where "
                + where
                + " order by "
                + order
                + " limit :limit offset :offset",
            p,
            (rs, n) -> row(rs));
    for (var r : rows) {
      boolean t = Users.current().role() == Role.TEACHER;
      String path =
          switch (source) {
            case "search_classes" -> (t ? "/my-classes" : "/classes") + "?classId=" + r.get("id");
            case "find_students" -> t ? "/my-classes" : "/students?studentId=" + r.get("id");
            case "get_student_classes" ->
                (t ? "/my-classes" : "/classes") + "?classId=" + r.get("classId");
            case "list_sessions" ->
                (t ? "/my-schedule" : "/schedule") + "?sessionId=" + r.get("id");
            case "list_priority_leads" -> "/leads?leadId=" + r.get("id");
            default -> null;
          };
      if (path != null)
        r.put("path", path + (path.contains("?") ? "&" : "?") + "branchId=" + r.get("branchId"));
    }
    return query.result(
        scope,
        source,
        new PageResponse<>(rows, page, size, count, (int) Math.ceil((double) count / size)));
  }

  public ReadQuery.Result courses(
      ReadQuery.Mode mode,
      Long branch,
      String text,
      String level,
      String type,
      Course.Status status,
      BigDecimal min,
      BigDecimal max,
      boolean inclusive,
      int page,
      int size) {
    var s = query.scope(mode, branch);
    query.page(page, size);
    query.keyword(text);
    // The legacy list keeps its default status; assistant defaults OPEN explicitly.
    var data =
        courses.list(
            page,
            size,
            text,
            status == null ? Course.Status.OPEN : status,
            level,
            type,
            s.mode() == ReadQuery.Mode.ACTIVE_BRANCH ? s.branchIds().getFirst() : null,
            min,
            max,
            inclusive,
            s.mode() == ReadQuery.Mode.ALL_ASSIGNED);
    var safe =
        data.content().stream()
            .map(
                c -> {
                  var r = new LinkedHashMap<String, Object>();
                  r.put("id", c.id());
                  r.put("code", c.code());
                  r.put("name", c.name());
                  r.put("type", c.type());
                  r.put("level", c.level());
                  r.put("tuitionFee", c.tuitionFee());
                  r.put("currency", "VND");
                  r.put("expectedMinutes", c.expectedMinutes());
                  r.put("durationVerified", c.expectedMinutes() != null);
                  r.put("status", c.status());
                  r.put("branchId", c.branchId());
                  r.put(
                      "path",
                      "/courses?keyword="
                          + java.net.URLEncoder.encode(
                              c.code(), java.nio.charset.StandardCharsets.UTF_8)
                          + (c.branchId() == null ? "" : "&branchId=" + c.branchId()));
                  return r;
                })
            .toList();
    return query.result(
        s,
        "search_courses",
        new PageResponse<>(safe, page, size, data.totalElements(), data.totalPages()));
  }

  public ReadQuery.Result classes(
      ReadQuery.Mode mode,
      Long branch,
      String text,
      Long courseId,
      com.example.academic.schoolclass.SchoolClass.Status status,
      boolean recruiting,
      int page,
      int size) {
    var s = query.scope(mode, branch);
    var p = params(s);
    query.id(courseId);
    String where = scope(s, "c.branch_id") + teacher(p) + keyword(p, text, "c.code", "c.name");
    if (courseId != null) {
      p.put("course", courseId);
      where += " and c.course_id=:course";
    }
    if (recruiting)
      where +=
          " and c.status<>'FINISHED' and "
              + END
              + ">=:asOf and c.current_size<c.max_size and co.status='OPEN'";
    String state =
        "case when c.status='FINISHED' or "
            + END
            + "<:asOf then 'FINISHED' when c.start_date>:asOf then 'UPCOMING' else 'ONGOING' end as"
            + " status";
    if (status != null) {
      if (status == com.example.academic.schoolclass.SchoolClass.Status.FULL)
        where += " and c.current_size>=c.max_size and c.status<>'FINISHED' and " + END + ">=:asOf";
      else {
        p.put("status", status.name());
        where += " and (" + state.substring(0, state.indexOf(" as status")) + ")=:status";
      }
    }
    return page(
        s,
        "search_classes",
        "c.id,c.code,c.name,c.branch_id,c.course_id,co.name as"
            + " course_name,c.teacher_name,c.current_size,c.max_size,c.start_date,"
            + END
            + " as end_date,"
            + state,
        "from school_classes c join courses co on co.id=c.course_id",
        where,
        "c.id desc",
        p,
        page,
        size);
  }

  public ReadQuery.Result students(
      ReadQuery.Mode mode, Long branch, String text, Long classId, int page, int size) {
    var s = query.scope(mode, branch);
    var p = params(s);
    query.id(classId);
    String where = scope(s, "s.branch_id") + keyword(p, text, "s.full_name", "s.student_code");
    if (classId != null || Users.current().role() == Role.TEACHER) {
      String filter = teacher(p);
      if (classId != null) {
        p.put("class", classId);
        filter += " and c.id=:class";
      }
      where +=
          " and exists(select 1 from enrollments e join school_classes c on c.id=e.class_id where"
              + " e.student_id=s.id and c.branch_id=s.branch_id and "
              + scope(s, "c.branch_id")
              + " and "
              + ACTIVE
              + filter
              + ")";
    }
    return page(
        s,
        "find_students",
        "s.id,s.student_code,s.full_name,s.branch_id,s.current_level,s.status",
        "from students s",
        where,
        "s.id asc",
        p,
        page,
        size);
  }

  public ReadQuery.Result studentClasses(
      ReadQuery.Mode mode, Long branch, Long studentId, LocalDate asOf, int page, int size) {
    query.id(studentId);
    if (studentId == null) throw ApiException.bad("studentId bắt buộc");
    currentDate(asOf);
    var s = query.scope(mode, branch);
    var p = params(s);
    p.put("student", studentId);
    String where = "e.student_id=:student and " + scope(s, "c.branch_id") + teacher(p);
    // Only relationships in the selected scope; no global Student DTO is loaded for teachers.
    String state =
        "case when "
            + ACTIVE
            + " then 'ACTIVE' when e.study_status='STUDYING' and c.start_date>:asOf and"
            + " c.status<>'FINISHED' then 'NOT_STARTED' when e.study_status='STUDYING' then"
            + " 'INACTIVE' else cast(e.study_status as char(32)) end as relationship";
    return page(
        s,
        "get_student_classes",
        "e.id as enrollment_id,e.student_id,c.id as class_id,c.code as class_code,c.name as"
            + " class_name,c.branch_id,e.study_status,"
            + state,
        "from enrollments e join school_classes c on c.id=e.class_id join students s on"
            + " s.id=e.student_id",
        where + " and s.branch_id=c.branch_id",
        "e.id desc",
        p,
        page,
        size);
  }

  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
  public ReadQuery.Result statistics(
      ReadQuery.Mode mode, Long branch, String metric, LocalDate asOf) {
    currentDate(asOf);
    var s = query.scope(mode, branch);
    var p = params(s);
    String sql =
        switch (metric) {
          case "TOTAL_PROFILES" ->
              "select count(*) from students s where " + scope(s, "s.branch_id");
          case "PROFILE_STUDYING" ->
              "select count(*) from students s where s.status='STUDYING' and "
                  + scope(s, "s.branch_id");
          case "ACTIVE_IN_CLASS" ->
              "select count(distinct e.student_id) from enrollments e join students s on"
                  + " s.id=e.student_id join school_classes c on c.id=e.class_id where"
                  + " s.branch_id=c.branch_id and "
                  + scope(s, "c.branch_id")
                  + " and "
                  + ACTIVE;
          default -> throw ApiException.bad("metric không hợp lệ");
        };
    return query.result(
        s,
        "get_student_statistics",
        Map.of(
            "metric",
            metric,
            "value",
            jdbc.queryForObject(sql, p, Long.class),
            "asOf",
            p.get("asOf")));
  }

  public ReadQuery.Result sessions(
      ReadQuery.Mode mode,
      Long branch,
      Long classId,
      Long teacherId,
      LocalDate from,
      LocalDate to,
      ClassSession.Status status,
      int page,
      int size) {
    var s = query.scope(mode, branch);
    query.id(classId);
    query.id(teacherId);
    if (from == null || to == null || to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 30)
      throw ApiException.bad("Chọn khoảng lịch từ 1 đến 31 ngày");
    if (Users.current().role() == Role.TEACHER
        && teacherId != null
        && !teacherId.equals(Users.current().profileId())) throw ApiException.forbidden();
    var p = params(s);
    p.put("from", from);
    p.put("to", to);
    String where = scope(s, "c.branch_id") + " and se.session_date between :from and :to";
    if (Users.current().role() == Role.TEACHER) {
      if (Users.current().profileId() == null) throw ApiException.forbidden();
      teacherId = Users.current().profileId();
    }
    if (teacherId != null) {
      p.put("teacher", teacherId);
      where += " and se.teacher_id=:teacher";
    }
    if (classId != null) {
      p.put("class", classId);
      where += " and c.id=:class";
    }
    if (status != null) {
      p.put("status", status.name());
      where += " and se.status=:status";
    }
    return page(
        s,
        "list_sessions",
        "se.id,c.id as class_id,c.name as"
            + " class_name,c.branch_id,se.session_date,se.start_time,se.end_time,se.teacher_id,se.teacher_name,se.room_name,se.status,se.kind,case"
            + " when se.teacher_id<>c.teacher_id then true else false end as substitute",
        "from class_sessions se join school_classes c on c.id=se.class_id",
        where,
        "se.session_date,se.start_time,se.id",
        p,
        page,
        size);
  }

  public ReadQuery.Result attendance(
      ReadQuery.Mode mode, Long branch, Long sessionId, int page, int size) {
    var s = query.scope(mode, branch);
    query.id(sessionId);
    if (sessionId == null) throw ApiException.bad("sessionId bắt buộc");
    query.page(page, size);
    var session = sessions.require(sessionId, false);
    if (!s.contains(session.getSchoolClass().getBranchId())) throw ApiException.missing();
    if (session.getSessionDate().isAfter(LocalDate.now(BusinessTime.ZONE))
        || session.getStatus() == ClassSession.Status.CANCELLED)
      return query.result(
          s,
          "get_session_attendance",
          Map.of(
              "sessionId",
              sessionId,
              "state",
              session.getStatus() == ClassSession.Status.CANCELLED ? "CANCELLED" : "NOT_STARTED"));
    // Match AttendanceService's dated roster (including saved historical rows), but page
    // in SQL rather than materialising every enrolment/student entity for an AI response.
    var p = params(s);
    p.put("asOf", session.getSessionDate());
    p.put("nextDay", session.getSessionDate().plusDays(1).atStartOfDay());
    p.put("class", session.getSchoolClass().getId());
    String from =
        "from enrollments e join students st on st.id=e.student_id left join attendances a on"
            + " a.enrollment_id=e.id and a.session_date=:asOf";
    String where = "e.class_id=:class and (" + EFFECTIVE + " or a.id is not null)";
    var records =
        page(
            s,
            "get_session_attendance",
            "e.id as enrollment_id,e.student_id,st.full_name as student_name,case when a.id is null"
                + " then false else true end as recorded,case when a.id is null then 'NOT_RECORDED'"
                + " else cast(a.status as char(32)) end as status",
            from,
            where,
            "e.id",
            p,
            page,
            size);
    long notRecorded =
        jdbc.queryForObject(
            "select count(*) " + from + " where " + where + " and a.id is null", p, Long.class);
    return query.result(
        s,
        "get_session_attendance",
        Map.of(
            "sessionId",
            sessionId,
            "state",
            "AVAILABLE",
            "records",
            records.data(),
            "notRecorded",
            notRecorded));
  }

  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
  public ReadQuery.Result leads(ReadQuery.Mode mode, Long branch, int limit) {
    var s = query.scope(mode, branch);
    query.page(0, limit);
    var p = params(s);
    var now = LocalDateTime.now(BusinessTime.ZONE);
    p.put("now", now);
    p.put("tomorrow", now.toLocalDate().plusDays(1).atStartOfDay());
    String group =
        "case when l.next_contact_at<:now then 0 when l.next_contact_at<:tomorrow then 1 when"
            + " l.next_contact_at is null then 2 else 3 end";
    String where = scope(s, "l.branch_id") + " and l.status in ('NEW','CONTACTED','CONSULTING')";
    if (Users.current().role() == Role.STAFF) {
      p.put("actor", Users.current().accountId());
      where += " and l.assigned_to=:actor";
    }
    String reason =
        "case when l.next_contact_at<:now then 'OVERDUE' when l.next_contact_at<:tomorrow then"
            + " 'TODAY' when l.next_contact_at is null then 'UNSCHEDULED' else 'UPCOMING' end as"
            + " priority_reason";
    return page(
        s,
        "list_priority_leads",
        "l.id,l.full_name,l.branch_id,l.status,l.assigned_to,l.assigned_to_name,l.next_contact_at,l.last_contacted_at,"
            + reason,
        "from leads l",
        where,
        group
            + ",l.next_contact_at,case when l.next_contact_at is null and l.last_contacted_at is"
            + " not null then 1 else 0 end,case when l.next_contact_at is null then"
            + " l.last_contacted_at else null end,case when l.next_contact_at is null then"
            + " l.created_at else null end,l.id",
        p,
        0,
        limit);
  }
}
