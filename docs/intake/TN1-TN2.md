# TN1–TN2 — Nhu cầu và API tiếp nhận học viên

Ngày cập nhật: 26/11/2026. Phạm vi: Academic, dữ liệu website liên quan đến lead, migration và kiểm thử. Giao diện tiếp nhận ba bước là TN3, chưa thay modal chuyển đổi cũ.

**Cập nhật:** TN3–TN4 đã thay modal cũ bằng quy trình tiếp nhận ba bước. Xem [hướng dẫn hiện hành](TN3-TN4.md). Nội dung dưới đây ghi lại phạm vi tại thời điểm TN1–TN2.

## TN1: dữ liệu nhu cầu

- Lead thêm `preferredClassId`, `preferredClassName`, `preferredTime` trong DTO. Khóa học quan tâm vẫn giữ `interestedCourseId`.
- Lớp mong muốn phải cùng chi nhánh, khớp khóa quan tâm; nếu chỉ chọn lớp thì xác định khóa từ lớp. Đây là nhu cầu, không giữ chỗ và không tạo đăng ký.
- API lưu Lead nhận thêm hai trường tùy chọn `preferredClassId`, `preferredTime`. Client cũ không gửi các trường này sẽ không xóa lựa chọn hiện có. Dùng API preferences để chủ động xóa/đổi lựa chọn.
- Lead mới từ website được liên kết lớp từ `classSlug` đã xác minh, không phân tích tên lớp trong ghi chú. Yêu cầu lặp giữ lựa chọn hiện hành và lưu mỗi yêu cầu trong `website_inquiries`, lịch sử lead.
- `GET /leads/{id}/preferences` trả nhu cầu hiện hành và tối đa 20 lựa chọn website gần nhất, gồm chương trình/khóa/lớp slug và thời điểm. Không suy đoán lớp của lead cũ hoặc tự ghi đè nhu cầu do nhân viên đã xác nhận.
- `PUT /leads/{id}/preferences` nhận `{version,courseId,classId,preferredTime,reason}`. `null` chủ động bỏ lựa chọn. Lưu lịch sử trước/sau; kiểm tra phiên bản, quyền chi nhánh và nhân viên phụ trách.

## TN2: hợp đồng API

Tiền tố: `/api/v1/academic`. Quyền: SUPER_ADMIN/ADMIN trong phạm vi, STAFF phải là người phụ trách lead. Không cho TEACHER/STUDENT.

| Phương thức | Đường dẫn | Tác dụng |
|---|---|---|
| GET | `/leads/{id}/preferences` | Đọc nhu cầu và lịch sử lựa chọn website |
| PUT | `/leads/{id}/preferences` | Xác nhận/đổi/xóa nhu cầu |
| POST | `/leads/{id}/intake/preview` | Xem trước, không tạo hồ sơ/đăng ký |
| POST | `/leads/{id}/intake/confirm` | Xác nhận nguyên tử, chống lặp |

Body xem trước (`Selection`):

```json
{
  "leadVersion": 2,
  "mode": "ENROLL",
  "existingStudentId": null,
  "studentVersion": null,
  "profile": null,
  "classId": 17,
  "approvalId": null,
  "acknowledgeSimilarProfiles": false
}
```

ID/phiên bản trên chỉ minh họa; lấy từ lead và lớp thực tế. `classId` phải xác nhận rõ, không tự suy ra đăng ký từ lớp mong muốn.

- `PROFILE_ONLY`: không gửi classId/approvalId, chỉ tạo hoặc liên kết hồ sơ.
- `ENROLL`: chọn classId; kiểm tra lại phân lớp khi xác nhận.
- Tạo mới: `profile=null` lấy họ tên/điện thoại/email từ lead; ngày sinh còn trống. Có thể gửi `StudentDto.Request` để bổ sung thông tin, gồm người giám hộ khi dưới 18 tuổi. Không nhận `currentLevel` khác rỗng trong bước tạo mới: trình độ mong muốn không được coi là đầu vào đã xác minh.
- Dùng hồ sơ hiện có: gửi `existingStudentId`, `studentVersion`, không gửi profile. Không ghi đè hồ sơ đã có bằng dữ liệu lead.
- Hồ sơ tương tự được tìm trong cùng chi nhánh theo liên hệ/họ tên, trả tối đa 21 kết quả. Số điện thoại là gợi ý, không tự gộp. Nếu tạo mới khi có hồ sơ tương tự, phải kiểm tra và đặt acknowledgeSimilarProfiles=true.

Preview trả lead, selection, hồ sơ sẽ tạo, similarProfiles, thông tin lớp/học phí/sĩ số, kết quả bộ kiểm tra Placement, blockers/notices và fingerprint. Frontend dùng API lớp/lịch hiện có để hiển thị thêm lịch chi tiết. Fingerprint ràng buộc nhu cầu, phiên bản lead/học viên, dữ liệu hồ sơ tương tự, học phí, sức chứa và điều kiện đầu vào.

Xác nhận bằng **cùng selection** đã xem trước:

```json
{
  "requestId": "UUID-do-client-tao",
  "selection": {"leadVersion": 2, "mode": "ENROLL", "classId": 17},
  "fingerprint": "64-ky-tu-hex-tu-preview",
  "reason": "Khách đã xác nhận hồ sơ và lớp học"
}
```

Body minh họa phải thay requestId/fingerprint bằng giá trị hợp lệ; không thay selection sau preview. Kết quả: `{leadId,studentId,enrollmentId,mode,createdStudent}`.

Nếu mất phản hồi, gửi lại nguyên body và requestId: trả cùng kết quả, không tạo trùng. Cùng requestId nhưng khác ý định trả 409. Lead đã chuyển đổi không được tiếp nhận lại bằng requestId khác; đăng ký thêm qua Enrollment hiện có.

## Kiểm tra và giao dịch

- Chỉ tiếp nhận lead CONTACTED/CONSULTING. Chưa liên hệ, đã mất hoặc đã chuyển đổi phải xử lý trạng thái phù hợp trước.
- Dùng chung Placement/Enrollment: chi nhánh, lớp/khóa đang nhận, sĩ số thực, đăng ký trùng, lịch trùng và chính sách đầu vào.
- Khách mới được xem trước bằng hồ sơ trong bộ nhớ, không chèn student/đánh giá giả. Context âm trong `placement.context.studentId` chỉ là định danh xem trước; không dùng để đăng ký hoặc xin ngoại lệ.
- Nếu lớp yêu cầu đánh giá đầu vào mà khách mới chưa có: chọn PROFILE_ONLY rồi đánh giá và đăng ký theo quy trình hiện có. Không tự bỏ điều kiện để ép một lần tiếp nhận.
- Ngoại lệ chỉ áp dụng cho hồ sơ đã tồn tại; approvalId được Placement kiểm tra lại khi xác nhận, không miễn sức chứa, lịch trùng hoặc quyền chi nhánh. Preview nêu rõ việc còn cần xác minh ngoại lệ, không coi việc nhập ID là đã được duyệt.
- Khóa theo chi nhánh, lead, học viên và lớp; kiểm tra lại fingerprint và nguồn ngay trước ghi. Tạo hồ sơ → đăng ký → cập nhật lead → receipt cùng giao dịch. Mọi lỗi rollback toàn bộ.
- Không tạo tài khoản đăng nhập và không thu tiền tại API tiếp nhận. Nhánh ENROLL sử dụng cơ chế giao học phí của Enrollment hiện có sau commit; sự cố Finance được xử lý theo trạng thái/retry hiện có, không xóa đăng ký đã thành công.
- Cho phép nhiều lead tư vấn liên kết cùng hồ sơ học viên; bỏ unique `leads.converted_student_id`, vẫn giữ khóa ngoại. Một lead chỉ có một receipt tiếp nhận mới.
- API chuyển đổi cũ tiếp tục hoạt động để tương thích giao diện hiện hành.

## Migration và local

- V033: `docs/migrations/V033__lead_intake.sql`; cài Docker mới: `deployment/docker/mysql/007-lead-intake.sql`.
- Docker hiện có: `node scripts/docker/apply-lead-intake-migration.cjs` sau backup. Script kiểm tra cột/index/constraint để chạy lại an toàn và so sánh hash dữ liệu cũ.
- Nếu dùng MySQL Windows/Maven riêng, phải áp dụng V033 trên database tương ứng. Tên unique index trong SQL theo schema dự án; schema tự tạo khác tên cần kiểm tra `SHOW INDEX FROM leads` và thay đúng unique index chỉ gồm converted_student_id, giữ index không unique cho khóa ngoại.
- Backup trước migration: `ops-1795660574406-d8c543` (99 bảng); schema mới 100 bảng, thêm `lead_intake_receipts`. Không chuyển lead hoặc thêm học viên thật tự động.
- Kiểm thử: `LeadIntakeTest`, hồi quy Academic và WebsiteIntakeWorkflowTest; nghiệm thu Docker/MySQL tạm bằng `scripts/docker/verify-lead-intake-live.cjs`. Bản clone chặn Finance/Teaching để không ghi vào hệ thống thật; được dọn sau thử nghiệm.
- Báo cáo nghiệm thu: [tn12-completion.json](tn12-completion.json) và [tn12-live.json](tn12-live.json). `node scripts/docker/publish-lead-intake-audit.cjs` đối chiếu dữ liệu nghiệp vụ với backup trước migration và kiểm tra trạng thái 9 container.

## Bàn giao TN3

Thay modal hiện tại bằng Hồ sơ → Đăng ký → Xác nhận. Điền sẵn lớp mong muốn nhưng yêu cầu xác nhận; hiển thị similarProfiles, blockers và điều kiện đầu vào bằng tiếng Việt. Lưu requestId cho tới khi xác định kết quả. Sau thành công mở hồ sơ/đăng ký và nút thu phí, thay vì buộc nhân viên tìm lại học viên ở màn hình phân lớp.
