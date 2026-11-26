package com.example.academic.assistant;

import com.example.academic.common.*;
import com.example.academic.security.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fixed, parameterized read projections; no generated SQL or writes. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF','TEACHER')")
public class AcademicInsights {
  private final AcademicReadService read;
  private final ReadQuery query;
  private final NamedParameterJdbcTemplate jdbc;
  private final ClassificationFilters classification;

  @Getter
  @Setter
  public static class Request {
    private ReadQuery.Mode scopeMode = ReadQuery.Mode.ACTIVE_BRANCH;
    private Long branchId, classId, studentId, teacherId, courseId;
    private String keyword, status, relationship, currentLevel;
    private LocalDate from, to;
    private LocalTime startTime, endTime;
    private int page = 0, size = 5, minAbsences = 3, minCapacity = 1;
    private boolean recruitingOnly, substituteOnly, upcomingOnly;
    private String kind, entryLevel, programCode, audienceCode, metric = "CLASSES_ONGOING";
  }

  private ReadQuery.Scope scope(Request r) {
    query.page(r.page, r.size);
    for (Long id : Arrays.asList(r.classId, r.studentId, r.teacherId, r.courseId)) query.id(id);
    return query.scope(r.scopeMode, r.branchId);
  }

  private void value(String value, String... allowed) {
    if (value != null && !Arrays.asList(allowed).contains(value))
      throw ApiException.bad("Invalid filter");
  }

  private void dates(LocalDate from, LocalDate to, int days) {
    if (from == null || to == null || to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > days)
      throw ApiException.bad("Chọn khoảng ngày hợp lệ, tối đa " + (days + 1) + " ngày");
  }

  private String eq(Map<String, Object> p, String key, Object val, String column) {
    if (val == null) return "";
    p.put(key, val);
    return " and " + column + "=:" + key;
  }

  private String dated(Map<String, Object> p, Request r, String column) {
    if (r.from == null && r.to == null) return "";
    dates(r.from, r.to, 365);
    p.put("from", r.from);
    p.put("to", r.to);
    return " and " + column + " between :from and :to";
  }

  private String state() {
    return "case when c.status='FINISHED' or "
        + AcademicReadService.END
        + "<:asOf then 'FINISHED' when c.start_date>:asOf then 'UPCOMING' else 'ONGOING' end";
  }

  private String effective(String day) {
    return "(exists(select 1 from enrollment_periods ep where ep.enrollment_id=e.id and"
        + " ep.active_from<="
        + day
        + " and (ep.active_until is null or ep.active_until>"
        + day
        + ")) or (not exists(select 1 from enrollment_periods ep where ep.enrollment_id=e.id) and"
        + " e.study_status='STUDYING' and cast(e.enrolled_at as date)<="
        + day
        + "))";
  }

  public ReadQuery.Result admissions(Request r) {
    var s = scope(r);
    var p = read.params(s);
    query.keyword(r.entryLevel);
    query.keyword(r.programCode);
    String w =
        "(co.branch_id is null or "
            + read.scope(s, "co.branch_id")
            + ") and co.status='OPEN' and ap.mode<>'UNVERIFIED'"
            + read.keyword(p, r.keyword, "co.code", "co.name")
            + classification.sql(p, "co", r.programCode, r.audienceCode)
            + " and (co.program_id is null or exists(select 1 from programs cp where cp.id=co.program_id and cp.code=ap.program_code))";
    if (r.entryLevel != null) {
      p.put("entry", r.entryLevel);
      w +=
          " and ap.mode='REQUIRES_ASSESSMENT' and exists(select 1 from course_admission_levels al"
              + " where al.policy_id=ap.id and al.code=:entry)";
    }
    return read.page(
        s,
        "search_course_admissions",
        "co.id,co.code,co.name,co.branch_id,co.tuition_fee,ap.mode as"
            + " admission_mode,ap.program_name,ap.output_level_name",
        "from courses co join course_admission_policies ap on ap.course_id=co.id and"
            + " ap.revision=(select max(p2.revision) from course_admission_policies p2 where"
            + " p2.course_id=co.id)",
        w,
        "co.id",
        p,
        r.page,
        r.size);
  }

  public ReadQuery.Result classes(Request r) {
    var s = scope(r);
    var p = read.params(s);
    value(r.status, "UPCOMING", "ONGOING", "FULL", "FINISHED");
    String w =
        read.scope(s, "c.branch_id")
            + read.teacher(p)
            + read.keyword(p, r.keyword, "c.code", "c.name", "co.name")
            + eq(p, "course", r.courseId, "c.course_id")
            + eq(p, "class", r.classId, "c.id");
    if (r.status != null)
      w +=
          r.status.equals("FULL")
              ? " and c.current_size>=c.max_size and (" + state() + ")<>'FINISHED'"
              : eq(p, "state", r.status, "(" + state() + ")");
    if (r.recruitingOnly)
      w += " and (" + state() + ")<>'FINISHED' and c.current_size<c.max_size and co.status='OPEN'";
    w +=
        dated(
            p,
            r,
            r.status != null && r.status.equals("FINISHED")
                ? AcademicReadService.END
                : "c.start_date");
    String active =
        "(select count(distinct e.student_id) from enrollments e join students st on"
            + " st.id=e.student_id where e.class_id=c.id and st.branch_id=c.branch_id and "
            + AcademicReadService.ACTIVE
            + ")";
    var result =
        read.page(
            s,
            "search_classes",
            "c.id,c.code,c.name,c.branch_id,c.course_id,co.name as"
                + " course_name,c.teacher_id,c.teacher_name,c.current_size,c.max_size,case when"
                + " c.max_size>c.current_size then c.max_size-c.current_size else 0 end as"
                + " remaining_seats,"
                + active
                + " as effective_student_count,c.start_date,"
                + AcademicReadService.END
                + " as end_date,"
                + state()
                + " as status",
            "from school_classes c join courses co on co.id=c.course_id",
            w,
            "c.id",
            p,
            r.page,
            r.size);
    if ("FINISHED".equals(r.status) && r.from != null)
      return new ReadQuery.Result(
          result.requestId(),
          result.status(),
          result.scope(),
          result.observedAt(),
          result.source(),
          result.data(),
          List.of(
              "Khoảng ngày lọc theo ngày kết thúc lịch hiện tại, không phải thời điểm đóng lớp thực"
                  + " tế. Chưa có dữ liệu để xác nhận lịch sử đóng lớp trong kỳ."),
          result.nextPage(),
          result.hasMore());
    return result;
  }

  public ReadQuery.Result students(Request r) {
    var s = scope(r);
    var p = read.params(s);
    value(r.status, "STUDYING", "PAUSED", "GRADUATED");
    value(r.relationship, "STUDYING", "RESERVED", "TRANSFERRED", "DROPPED", "COMPLETED");
    String w =
        read.scope(s, "s.branch_id")
            + read.keyword(p, r.keyword, "s.full_name", "s.student_code")
            + eq(p, "student", r.studentId, "s.id")
            + eq(p, "status", r.status, "s.status")
            + eq(p, "level", r.currentLevel, "s.current_level");
    String ownership =
        read.teacher(p)
            + eq(p, "class", r.classId, "c.id")
            + eq(p, "relation", r.relationship, "e.study_status");
    if (r.classId != null || r.relationship != null || Users.current().role() == Role.TEACHER) {
      // Teacher visibility requires a current effective relation to their primary class,
      // separately from the requested historical relationship filter.
      String base =
          "e.student_id=s.id and c.branch_id=s.branch_id and " + read.scope(s, "c.branch_id");
      if (Users.current().role() == Role.TEACHER) {
        w +=
            " and exists(select 1 from enrollments e join school_classes c on c.id=e.class_id where"
                + " "
                + base
                + read.teacher(p)
                + " and "
                + AcademicReadService.ACTIVE
                + ")";
      }
      w +=
          " and exists(select 1 from enrollments e join school_classes c on c.id=e.class_id where "
              + base
              + ownership
              + (r.relationship == null ? " and " + AcademicReadService.ACTIVE : "")
              + ")";
    }
    return read.page(
        s,
        "find_students",
        "s.id,s.student_code,s.full_name,s.branch_id,s.current_level,s.status",
        "from students s",
        w,
        "s.id",
        p,
        r.page,
        r.size);
  }

  public ReadQuery.Result profile(Request r) {
    if (r.studentId == null) throw ApiException.bad("studentId bắt buộc");
    r.page = 0;
    r.size = 1;
    var found = students(r);
    var page = (PageResponse<?>) found.data();
    if (page.content().isEmpty()) throw ApiException.missing();
    return query.result(
        query.scope(r.scopeMode, r.branchId), "get_student_profile", page.content().getFirst());
  }

  public ReadQuery.Result rosterCount(Request r) {
    var s = scope(r);
    var p = read.params(s);
    if (r.classId != null) {
      p.put("selectedClass", r.classId);
      if (jdbc.queryForObject(
              "select count(*) from school_classes c where c.id=:selectedClass and "
                  + read.scope(s, "c.branch_id")
                  + read.teacher(p),
              p,
              Long.class)
          == 0) throw ApiException.missing();
    }
    String w =
        read.scope(s, "c.branch_id")
            + read.teacher(p)
            + eq(p, "class", r.classId, "c.id")
            + " and st.branch_id=c.branch_id and "
            + AcademicReadService.ACTIVE;
    long n =
        jdbc.queryForObject(
            "select count(distinct e.student_id) from enrollments e join school_classes c on"
                + " c.id=e.class_id join students st on st.id=e.student_id where "
                + w,
            p,
            Long.class);
    return query.result(
        s,
        "get_roster_statistics",
        Map.of("metric", "DISTINCT_EFFECTIVE_STUDENTS", "value", n, "asOf", p.get("asOf")));
  }

  public ReadQuery.Result sessions(Request r) {
    var s = scope(r);
    dates(r.from, r.to, 30);
    var p = read.params(s);
    value(r.status, "SCHEDULED", "CANCELLED", "COMPLETED");
    value(r.kind, "REGULAR", "MAKEUP", "LEGACY");
    if (Users.current().role() == Role.TEACHER) {
      Long own = Users.current().profileId();
      if (own == null || r.teacherId != null && !own.equals(r.teacherId))
        throw ApiException.forbidden();
    }
    p.put("from", r.from);
    p.put("to", r.to);
    p.put("nowTime", LocalTime.now(BusinessTime.ZONE));
    String w =
        read.scope(s, "c.branch_id")
            + " and se.session_date between :from and :to"
            + eq(p, "class", r.classId, "c.id")
            + eq(
                p,
                "teacher",
                Users.current().role() == Role.TEACHER ? Users.current().profileId() : r.teacherId,
                "se.teacher_id")
            + eq(p, "status", r.status, "se.status")
            + eq(p, "kind", r.kind, "se.kind");
    if (r.substituteOnly) w += " and se.teacher_id<>c.teacher_id";
    if (r.upcomingOnly)
      w +=
          " and se.status='SCHEDULED' and (se.session_date>:asOf or (se.session_date=:asOf and"
              + " se.start_time>:nowTime))";
    return read.page(
        s,
        "list_sessions",
        "se.id,c.id as class_id,c.name as"
            + " class_name,c.branch_id,se.session_date,se.start_time,se.end_time,se.teacher_id,se.teacher_name,se.room_name,se.status,se.kind,case"
            + " when se.teacher_id<>c.teacher_id then true else false end as substitute",
        "from class_sessions se join school_classes c on c.id=se.class_id",
        w,
        "se.session_date,se.start_time,se.id",
        p,
        r.page,
        r.size);
  }

  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
  public ReadQuery.Result rooms(Request r) {
    var s = scope(r);
    var p = read.params(s);
    value(r.status, "AVAILABLE", "MAINTENANCE");
    if (r.minCapacity < 1 || r.minCapacity > 10000) throw ApiException.bad("Invalid capacity");
    p.put("capacity", r.minCapacity);
    String w =
        read.scope(s, "r.branch_id")
            + " and r.capacity>=:capacity"
            + read.keyword(p, r.keyword, "r.code", "r.name")
            + eq(p, "status", r.status, "r.status");
    if (r.from != null || r.to != null || r.startTime != null || r.endTime != null) {
      window(r, p);
      w +=
          " and r.status='AVAILABLE' and not exists(select 1 from class_sessions se where"
              + " se.room_id=r.id and "
              + conflict()
              + ")";
    }
    var result =
        read.page(
            s,
            "search_rooms",
            "r.id,r.code,r.name,r.branch_id,r.capacity,r.status",
            "from rooms r",
            w,
            "r.id",
            p,
            r.page,
            r.size);
    return r.from == null ? result : availability(result);
  }

  private void window(Request r, Map<String, Object> p) {
    if (r.from == null
        || r.startTime == null
        || r.endTime == null
        || !r.endTime.isAfter(r.startTime)
        || r.to != null && !r.to.equals(r.from))
      throw ApiException.bad("Cần ngày và khoảng giờ cùng ngày");
    p.put("day", r.from);
    p.put("start", r.startTime);
    p.put("end", r.endTime);
  }

  private String conflict() {
    return "se.session_date=:day and se.status<>'CANCELLED' and (se.start_time is null or"
        + " se.end_time is null or (se.start_time<:end and se.end_time>:start))";
  }

  private ReadQuery.Result availability(ReadQuery.Result r) {
    return new ReadQuery.Result(
        r.requestId(),
        r.status(),
        r.scope(),
        r.observedAt(),
        r.source(),
        r.data(),
        List.of(
            "Chỉ đối chiếu các buổi học đã lưu; chưa xét nghỉ phép, giờ làm, thời gian di chuyển"
                + " hoặc lịch chưa sinh. Đây không phải xác nhận sẵn sàng đặt lịch."),
        r.nextPage(),
        r.hasMore());
  }

  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
  public ReadQuery.Result availableTeachers(Request r) {
    var s = scope(r);
    var p = read.params(s);
    window(r, p);
    // The project already shares this MySQL schema (TeacherDirectory.lock uses these tables).
    String membership =
        "exists(select 1 from account_branches b where b.account_id=a.id and b.status=true and "
            + read.scope(s, "b.branch_id")
            + ")";
    String w =
        "a.role='TEACHER' and a.status=true and t.work_status='ACTIVE' and "
            + membership
            + read.keyword(p, r.keyword, "t.full_name")
            + " and not exists(select 1 from class_sessions se where se.teacher_id=t.id and "
            + conflict()
            + ")";
    return availability(
        read.page(
            s,
            "find_available_teachers",
            "t.id,t.full_name",
            "from teachers t join accounts a on a.id=t.account_id",
            w,
            "t.id",
            p,
            r.page,
            r.size));
  }

  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public ReadQuery.Result grouped(Request r) {
    var s = scope(r);
    var p = read.params(s);
    value(r.metric, "CLASSES_ONGOING", "CLASSES_FINISHED", "ACTIVE_STUDENTS");
    String from, w;
    if ("ACTIVE_STUDENTS".equals(r.metric)) {
      from =
          "from enrollments e join students st on st.id=e.student_id join school_classes c on"
              + " c.id=e.class_id";
      w = "st.branch_id=c.branch_id and " + AcademicReadService.ACTIVE;
    } else {
      from = "from school_classes c";
      w =
          "("
              + state()
              + ")='"
              + ("CLASSES_FINISHED".equals(r.metric) ? "FINISHED" : "ONGOING")
              + "'";
    }
    w += " and " + read.scope(s, "c.branch_id");
    String count =
        "count(distinct " + ("ACTIVE_STUDENTS".equals(r.metric) ? "e.student_id" : "c.id") + ")";
    String grouped =
        "select c.branch_id,"
            + count
            + " as metric_value "
            + from
            + " where "
            + w
            + " group by c.branch_id";
    long n =
        jdbc.queryForObject("select count(*) from (" + grouped + ") grouped_rows", p, Long.class);
    p.put("limit", r.size);
    p.put("offset", r.page * r.size);
    var rows =
        jdbc.query(
            grouped + " order by metric_value desc,c.branch_id limit :limit offset :offset",
            p,
            (rs, i) ->
                Map.of("branchId", rs.getLong(1), "value", rs.getLong(2), "metric", r.metric));
    return query.result(
        s,
        "get_academic_statistics",
        new PageResponse<>(rows, r.page, r.size, n, (int) Math.ceil((double) n / r.size)));
  }

  public ReadQuery.Result absences(Request r) {
    var s = scope(r);
    dates(r.from, r.to, 89);
    if (r.minAbsences < 1 || r.minAbsences > 90) throw ApiException.bad("Invalid threshold");
    var p = read.params(s);
    p.put("from", r.from);
    p.put("to", r.to);
    p.put("minimum", r.minAbsences);
    p.put("time", LocalTime.now(BusinessTime.ZONE));
    String w =
        read.scope(s, "c.branch_id")
            + read.teacher(p)
            + eq(p, "class", r.classId, "c.id")
            + eq(p, "student", r.studentId, "st.id")
            + " and st.branch_id=c.branch_id and a.status='ABSENT' and se.status<>'CANCELLED' and"
            + " se.session_date between :from and :to and (se.session_date<:asOf or"
            + " (se.session_date=:asOf and se.end_time<=:time)) and "
            + effective("se.session_date");
    String grouped =
        "select st.id,st.student_code,st.full_name,c.branch_id,count(distinct a.id) as absent_count"
            + " from attendances a join enrollments e on e.id=a.enrollment_id join school_classes c"
            + " on c.id=e.class_id join students st on st.id=e.student_id join class_sessions se on"
            + " se.class_id=c.id and se.session_date=a.session_date and (a.class_session_id is null"
            + " or a.class_session_id=se.id) where "
            + w
            + " group by st.id,st.student_code,st.full_name,c.branch_id having count(distinct"
            + " a.id)>=:minimum";
    var result =
        read.page(
            s,
            "get_attendance_summary",
            "g.*",
            "from (" + grouped + ") g",
            "1=1",
            "g.absent_count desc,g.id",
            p,
            r.page,
            r.size);
    return new ReadQuery.Result(
        result.requestId(),
        result.status(),
        result.scope(),
        result.observedAt(),
        result.source(),
        result.data(),
        List.of(
            "Tiêu chí: ít nhất "
                + r.minAbsences
                + " buổi ABSENT đã ghi nhận từ "
                + r.from
                + " đến "
                + r.to
                + "; loại buổi hủy, tương lai và ngoài kỳ học hiệu lực. Không coi chưa điểm danh là"
                + " vắng."),
        result.nextPage(),
        result.hasMore());
  }

  public ReadQuery.Result missingAttendance(Request r) {
    var s = scope(r);
    dates(r.from, r.to, 89);
    var p = read.params(s);
    p.put("from", r.from);
    p.put("to", r.to);
    p.put("time", LocalTime.now(BusinessTime.ZONE));
    String w =
        read.scope(s, "c.branch_id")
            + eq(p, "class", r.classId, "c.id")
            + " and se.status<>'CANCELLED' and se.session_date between :from and :to and"
            + " (se.session_date<:asOf or (se.session_date=:asOf and se.end_time<=:time))";
    if (Users.current().role() == Role.TEACHER) {
      if (Users.current().profileId() == null) throw ApiException.forbidden();
      p.put("teacher", Users.current().profileId());
      w += " and (se.teacher_id=:teacher or c.teacher_id=:teacher)";
    }
    String missing =
        "(select count(*) from enrollments e join students st on st.id=e.student_id where"
            + " e.class_id=c.id and st.branch_id=c.branch_id and "
            + effective("se.session_date")
            + " and not exists(select 1 from attendances a where a.enrollment_id=e.id and"
            + " a.session_date=se.session_date))";
    return read.page(
        s,
        "list_missing_attendance",
        "se.id,c.id as class_id,c.name as"
            + " class_name,c.branch_id,se.session_date,se.start_time,se.end_time,se.teacher_name,"
            + missing
            + " as not_recorded",
        "from class_sessions se join school_classes c on c.id=se.class_id",
        w + " and " + missing + ">0",
        "se.session_date,se.id",
        p,
        r.page,
        r.size);
  }
}
