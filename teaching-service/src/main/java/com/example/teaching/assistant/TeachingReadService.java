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
    var s = query.scope(mode, branch);
    query.id(examId);
    query.page(page, size);
    if (examId == null) throw ApiException.bad("examId bắt buộc");
    // AcademicDirectory inside require verifies the teacher's primary class, even for direct tool
    // calls.
    var exam = exams.require(examId);
    if (!s.contains(exam.getBranchId())) throw ApiException.missing();
    var p =
        Map.<String, Object>of(
            "exam", examId, "branch", exam.getBranchId(), "limit", size, "offset", page * size);
    boolean frozen = exam.getRosterFrozenAt() != null;
    String from =
        frozen
            ? "from exam_candidates ec left join grades g on g.exam_id=ec.exam_id and"
                + " g.student_id=ec.student_id and g.branch_id=:branch where ec.exam_id=:exam"
            : "from grades g where g.exam_id=:exam and g.branch_id=:branch";
    String identity = frozen ? "ec.student_id,ec.student_name" : "g.student_id,g.student_name";
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
