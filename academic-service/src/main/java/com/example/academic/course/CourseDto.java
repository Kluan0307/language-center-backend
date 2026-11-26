package com.example.academic.course;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CourseDto(
    Long id,
    String code,
    String name,
    String type,
    String level,
    Integer duration,
    BigDecimal tuitionFee,
    String content,
    String entryRequirement,
    Integer maxStudents,
    Course.Status status,
    Long branchId,
    Integer expectedMinutes,
    long durationRevision, Long programId,java.util.List<AudienceDto> audiences,long classificationRevision,boolean classificationComplete) {
  public record AudienceDto(Long id,String code,String name,boolean active) {}
  public static CourseDto of(Course c) {
    return new CourseDto(
        c.getId(),
        c.getCode(),
        c.getName(),
        c.getType(),
        c.getLevel(),
        c.getDuration(),
        c.getTuitionFee(),
        c.getContent(),
        c.getEntryRequirement(),
        c.getMaxStudents(),
        c.getStatus(),
        c.getBranchId(),
        c.getExpectedMinutes(),
        c.getDurationRevision(),c.getProgramId(),c.getAudienceGroups().stream().sorted(java.util.Comparator.comparing(AudienceGroup::getSortOrder)).map(a->new AudienceDto(a.getId(),a.getCode(),a.getName(),a.isActive())).toList(),c.getClassificationRevision(),c.getProgramId()!=null&&!c.getAudienceGroups().isEmpty());
  }

  public record Request(
      @NotBlank @Size(max = 255) String code,
      @NotBlank @Size(max = 255) String name,
      @Size(max = 255) String type,
      @Size(max = 255) String level,
      @Positive Integer duration,
      @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 2)
          BigDecimal tuitionFee,
      String content,
      String entryRequirement,
      @NotNull @Positive Integer maxStudents,
      Long branchId,
      @Min(1) @Max(com.example.academic.duration.DurationDto.MAX_MINUTES) Integer expectedMinutes,
      Long programId,java.util.List<Long> audienceIds,Long classificationRevision) {
    public Request(String code,String name,String type,String level,Integer duration,BigDecimal fee,String content,String entry,Integer maxStudents,Long branchId,Integer minutes){this(code,name,type,level,duration,fee,content,entry,maxStudents,branchId,minutes,null,null,null);}
    public Request(
        String code,
        String name,
        String type,
        String level,
        Integer duration,
        BigDecimal fee,
        String content,
        String entry,
        Integer maxStudents,
        Long branchId) {
      this(code, name, type, level, duration, fee, content, entry, maxStudents, branchId, null);
    }
  }

  public record StatusRequest(@NotNull Course.Status status) {}
}
