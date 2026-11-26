package com.example.teaching.assistant;

import com.example.teaching.common.*;
import com.example.teaching.exam.*;
import com.example.teaching.grade.*;
import com.example.teaching.publication.*;
import com.example.teaching.security.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeachingReadService {
  private final ReadQuery query;
  private final ExamService exams;
  private final ManagementProgressService published;
  private final NamedParameterJdbcTemplate jdbc;

  public record GradeRow(Long studentId, String studentName, BigDecimal score, String status) {}

  public record PublishedRow(
      Long examId,
      String examName,
      LocalDate examDate,
      BigDecimal maxScore,
      BigDecimal score,
      String status,
      long revision,
      LocalDateTime publishedAt) {}

  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','TEACHER')")
  public ReadQuery.Result current(
      ReadQuery.Mode mode, Long branch, Long examId, int page, int size) {
    return current(mode, branch, examId, null, "ALL", page, size);
  }

  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','TEACHER')")
  public ReadQuery.Result current(
      ReadQuery.Mode mode,
      Long branch,
      Long examId,
      Long studentId,
      String scoreFilter,
      int page,
      int size) {
    var s = query.scope(mode, branch);
    query.id(examId);
    query.page(page, size);
    if (examId == null) throw ApiException.bad("examId bắt buộc");
    // AcademicDirectory inside require verifies the teacher's primary class, even for direct tool
    // calls.
    var exam = exams.require(examId);
    if (!s.contains(exam.getBranchId())) throw ApiException.missing();
    query.id(studentId);
    if (!Set.of("ALL", "ZERO", "NOT_GRADED").contains(scoreFilter))
      throw ApiException.bad("Invalid score filter");
    var p =
        new HashMap<String, Object>(
            Map.of(
                "exam",
                examId,
                "branch",
                exam.getBranchId(),
                "limit",
                size,
                "offset",
                page * size));
    boolean frozen = exam.getRosterFrozenAt() != null;
    String from =
        frozen
            ? "from exam_candidates ec left join grades g on g.exam_id=ec.exam_id and"
                + " g.student_id=ec.student_id and g.branch_id=:branch where ec.exam_id=:exam"
            : "from grades g where g.exam_id=:exam and g.branch_id=:branch";
    String identity = frozen ? "ec.student_id,ec.student_name" : "g.student_id,g.student_name";
    if (studentId != null) {
      p.put("student", studentId);
      from += " and " + (frozen ? "ec.student_id" : "g.student_id") + "=:student";
    }
    if (scoreFilter.equals("ZERO")) from += " and g.score=0";
    if (scoreFilter.equals("NOT_GRADED")) {
      if (!frozen)
        throw ApiException.bad("Danh sách dự thi chưa chốt; chưa xác định đầy đủ người thiếu điểm");
      from += " and g.score is null";
    }
    long count = jdbc.queryForObject("select count(*) " + from, p, Long.class);
    var rows =
        jdbc.query(
            "select "
                + identity
                + ",g.score "
                + from
                + " order by "
                + (frozen ? "ec.student_id" : "g.student_id")
                + " limit :limit offset :offset",
            p,
            (rs, n) ->
                new GradeRow(
                    rs.getLong(1),
                    rs.getString(2),
                    rs.getBigDecimal(3),
                    rs.getBigDecimal(3) == null ? "NOT_GRADED" : "GRADED"));
    return query.result(
        s,
        "get_learning_results",
        Map.of(
            "view",
            "CURRENT",
            "examId",
            examId,
            "maxScore",
            exam.getMaxScore(),
            "rosterState",
            frozen ? "FROZEN" : "SAVED_GRADES_ONLY",
            "records",
            new PageResponse<>(rows, page, size, count, (int) Math.ceil((double) count / size)),
            "path",
            "/grades?examId=" + examId + "&branchId=" + exam.getBranchId()));
  }

  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','TEACHER')")
  public ReadQuery.Result examList(
      ReadQuery.Mode mode,
      Long branch,
      Long classId,
      Long examId,
      String keyword,
      boolean missingOnly,
      int page,
      int size) {
    var s = query.scope(mode, branch);
    query.id(classId);
    query.id(examId);
    query.page(page, size);
    var p = new HashMap<String, Object>();
    p.put("branches", s.branchIds());
    p.put("today", LocalDate.now(BusinessTime.ZONE));
    p.put("limit", size);
    p.put("offset", page * size);
    String w =
        s.mode() == ReadQuery.Mode.WHOLE_SYSTEM
            ? "1=1"
            : s.branchIds().isEmpty() ? "1=0" : "ex.branch_id in (:branches)";
    if (examId != null) {
      p.put("exam", examId);
      w += " and ex.id=:exam";
    }
    if (keyword != null && !keyword.isBlank()) {
      if (keyword.length() > 120) throw ApiException.bad("Keyword too long");
      p.put(
          "keyword",
          "%"
              + keyword
                  .toLowerCase(Locale.ROOT)
                  .replace("!", "!!")
                  .replace("%", "!%")
                  .replace("_", "!_")
              + "%");
      w += " and lower(ex.name) like :keyword escape '!'";
    }
    if (classId != null) {
      examsForClass(classId, s);
      p.put("class", classId);
      w += " and ex.class_id=:class";
    }
    if (Users.current().role() == Role.TEACHER) {
      if (Users.current().profileId() == null) throw ApiException.forbidden();
      p.put("teacher", Users.current().profileId());
      w +=
          " and exists(select 1 from school_classes c where c.id=ex.class_id and"
              + " c.branch_id=ex.branch_id and c.teacher_id=:teacher)";
    }
    String missing =
        "(select count(*) from exam_candidates ec left join grades g on g.exam_id=ec.exam_id and"
            + " g.student_id=ec.student_id and g.branch_id=ex.branch_id where ec.exam_id=ex.id and"
            + " g.score is null)";
    if (missingOnly)
      w += " and ex.roster_frozen_at is not null and ex.exam_date<=:today and " + missing + ">0";
    long count = jdbc.queryForObject("select count(*) from exams ex where " + w, p, Long.class);
    var rows =
        jdbc.query(
            "select"
                + " ex.id,ex.name,ex.class_id,ex.branch_id,ex.exam_date,ex.max_score,ex.roster_frozen_at,"
                + missing
                + " as missing_count from exams ex where "
                + w
                + " order by ex.exam_date,ex.id limit :limit offset :offset",
            p,
            (rs, i) -> {
              var row = new LinkedHashMap<String, Object>();
              row.put("id", rs.getLong("id"));
              row.put("examId", rs.getLong("id"));
              row.put("examName", rs.getString("name"));
              row.put("classId", rs.getLong("class_id"));
              row.put("branchId", rs.getLong("branch_id"));
              row.put("examDate", rs.getDate("exam_date").toLocalDate());
              row.put("maxScore", rs.getBigDecimal("max_score"));
              row.put(
                  "rosterState",
                  rs.getTimestamp("roster_frozen_at") == null ? "SAVED_GRADES_ONLY" : "FROZEN");
              row.put(
                  "missingCount",
                  rs.getTimestamp("roster_frozen_at") == null ? null : rs.getLong("missing_count"));
              row.put("deadlineKnown", false);
              return row;
            });
    var result =
        query.result(
            s,
            "list_learning_exams",
            new PageResponse<>(rows, page, size, count, (int) Math.ceil((double) count / size)));
    return new ReadQuery.Result(
        result.requestId(),
        result.status(),
        result.scope(),
        result.observedAt(),
        result.source(),
        result.data(),
        List.of(
            "Số người thiếu điểm chỉ xác định khi danh sách dự thi đã chốt. Hệ thống chưa lưu hạn"
                + " chấm điểm; không kết luận quá hạn chấm từ ngày thi."),
        result.nextPage(),
        result.hasMore());
  }

  private void examsForClass(Long id, ReadQuery.Scope scope) {
    // Reuse the existing service's class ownership check without creating/finalizing exams.
    var first = exams.list(0, 1, null, id);
    if (!first.content().isEmpty() && !scope.contains(first.content().getFirst().branchId()))
      throw ApiException.missing();
  }

  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
  public ReadQuery.Result published(
      ReadQuery.Mode mode, Long branch, Long studentId, Long enrollmentId, int page, int size) {
    var s = query.scope(mode, branch);
    query.id(studentId);
    query.id(enrollmentId);
    query.page(page, size);
    var data =
        published.grades(
            studentId,
            enrollmentId,
            page,
            size,
            s.mode() == ReadQuery.Mode.WHOLE_SYSTEM ? null : Set.copyOf(s.branchIds()));
    var rows =
        data.content().stream()
            .map(
                g ->
                    new PublishedRow(
                        g.examId(),
                        g.examName(),
                        g.examDate(),
                        g.maxScore(),
                        g.score(),
                        g.status(),
                        g.revision(),
                        g.publishedAt()))
            .toList();
    return query.result(
        s,
        "get_learning_results",
        Map.of(
            "view",
            "PUBLISHED",
            "studentId",
            studentId,
            "enrollmentId",
            enrollmentId,
            "records",
            new PageResponse<>(rows, page, size, data.totalElements(), data.totalPages()),
            "path",
            "/students?studentId=" + studentId));
  }
}
