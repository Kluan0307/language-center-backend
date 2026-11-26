package com.example.teaching.grading;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class GradingModels {
  private GradingModels() {}
  public enum Kind { OBSERVATION, INTERNAL, IELTS, TOEIC_LR }
  public enum Status { GRADED, ABSENT, EXEMPT, NOT_SUBMITTED }
  public enum Level { NEEDS_SUPPORT, DEVELOPING, MEETS_EXPECTATIONS }
  public record Component(@NotBlank @Pattern(regexp="[A-Z][A-Z0-9_]{0,39}") String code,
      @NotBlank @Size(max=120) String name, @NotNull Boolean required,
      BigDecimal minScore, BigDecimal maxScore, BigDecimal step, BigDecimal weight) {}
  public record Spec(@NotNull Kind kind, @NotEmpty @Size(max=20) List<@NotNull @Valid Component> components,
      BigDecimal passPercent, BigDecimal minAttendancePercent,BigDecimal targetScore,BigDecimal minimumSkillScore) {
    public Spec(Kind kind,List<Component> components,BigDecimal passPercent,BigDecimal minAttendancePercent){this(kind,components,passPercent,minAttendancePercent,null,null);}
  }
  public record TemplateCommand(@NotNull Kind code, @NotNull @PositiveOrZero Long expectedRevision,
      @NotBlank @Size(max=160) String name, @NotNull @Valid Spec spec,
      @NotBlank @Size(min=3,max=500) String reason) {}
  public record TemplateView(String id, Kind code, long revision, String name, Spec spec) {}
  public record Assignment(@NotBlank String templateId, @NotNull @PositiveOrZero Long expectedRevision,
      @NotBlank @Size(min=3,max=500) String reason) {}
  public record CourseAssignment(@NotNull @Positive Long courseId,@NotNull @Positive Long branchId,
      @NotBlank String templateId,@NotNull @PositiveOrZero Long expectedRevision,
      @NotBlank @Size(min=3,max=500) String reason) {}
  public record BindingView(Long classId,Long branchId,Long courseId,long revision,String templateId,
      String name,Spec spec,boolean locked,LocalDateTime assignedAt) {}
  public record ActivityCommand(@NotBlank @Size(max=160) String name,@NotNull LocalDate date,
      @NotEmpty @Size(max=20) List<@NotBlank String> components,@Positive Long retakeOf,
      @Positive Long conversionId,@Size(max=1000) List<@NotNull @Positive Long> studentIds) {
    public ActivityCommand(String name,LocalDate date,List<String> components){this(name,date,components,null,null,null);}
  }
  public record ActivityView(Long id,Long classId,String name,LocalDate date,List<String> components,
      boolean rosterFrozen,int rosterSize,Long retakeOf,Long conversionId,long version) {}
  public record ActivityEdit(@NotNull @PositiveOrZero Long expectedVersion,@NotBlank @Size(max=160) String name,@NotNull LocalDate date,@NotBlank @Size(min=3,max=500) String reason) {}
  public record Mark(@NotNull @Positive Long studentId,@NotBlank String component,
      @PositiveOrZero Long expectedVersion,@NotNull Status status,
      @Digits(integer=5,fraction=2) BigDecimal score,Level level,
      @Size(max=1000) String comment,@Min(0) @Max(100) Integer rawScore) {
    public Mark(Long studentId,String component,Long expectedVersion,Status status,BigDecimal score,Level level,String comment){this(studentId,component,expectedVersion,status,score,level,comment,null);}
  }
  public record SaveMarks(@NotEmpty @Size(max=500) List<@NotNull @Valid Mark> records,
      @NotBlank @Size(min=3,max=500) String reason) {}
  public record Candidate(Long studentId,String studentName,Long enrollmentId) {}
  public record MarkView(Long studentId,String component,long version,Status status,BigDecimal score,
      Level level,String comment,Long enteredBy,LocalDateTime updatedAt,Integer rawScore,Long conversionId) {}
  public record Sheet(BindingView binding,ActivityView activity,List<Candidate> students,List<MarkView> records) {}
}
