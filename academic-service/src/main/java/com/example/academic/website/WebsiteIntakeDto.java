package com.example.academic.website;
import jakarta.validation.constraints.*;
public final class WebsiteIntakeDto {
  private WebsiteIntakeDto() {}
  public record Request(
      @NotBlank @Pattern(regexp="[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}") String requestId,
      @NotBlank @Size(min=2,max=100) String fullName,
      @NotBlank @Size(max=30) String phone,
      @Email @Size(max=150) String email,
      @NotBlank @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max=120) String branchSlug,
      @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max=120) String programSlug,
      @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max=120) String courseSlug,
      @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max=120) String classSlug,
      @Size(max=1000) String note,
      @NotNull @AssertTrue Boolean consent,
      @Size(max=0) String website) {}
  public record Accepted(String message) {
    public static Accepted received(){return new Accepted("Yêu cầu đã được ghi nhận. Trung tâm sẽ liên hệ qua số điện thoại bạn cung cấp.");}
  }
}
