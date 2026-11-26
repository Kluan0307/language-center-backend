package com.example.academic.website;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
public final class WebsiteDto {
  private WebsiteDto() {}
  public record Edit(@NotNull @Positive Long sourceId, @NotNull Long version,
      @NotBlank @Size(max=120) @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
      @NotNull WebsitePublication.State state,
      @Size(max=2000) String summary,@Size(max=2000) String audience,
      @Size(max=4000) String outcomes,@Size(max=4000) String topics,
      @Size(max=1000) String imageUrl,@Size(max=500) String openingHours,
      @Size(max=1000) String mapUrl,@Positive Long programId,boolean showTuition) {}
  public record Item(String slug,String code,String name,String summary,String audience,
      String outcomes,List<String> topics,String imageUrl,String address,String phone,
      String openingHours,String mapUrl,String programSlug,String courseSlug,String branchSlug,
      String entryRequirement,Integer expectedMinutes,BigDecimal tuitionFee,
      LocalDate startDate,LocalDate endDate,LocalDateTime updatedAt,
      String branchName,List<Meeting> meetings,com.example.academic.course.CourseClassificationView classification) {}
  public record Meeting(String dayOfWeek,LocalTime startTime,LocalTime endTime) {}
  public record Page(List<Item> items,int page,int size,boolean hasNext) {}
}
