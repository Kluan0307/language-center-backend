# L6–L7 — Tiếp nhận tư vấn và giao diện tuyển sinh

## Đã triển khai

- Form tư vấn thật tại `/tu-van`, nhận họ tên, điện thoại, email tùy chọn, chi nhánh, chương trình/khóa/lớp tùy chọn và nhu cầu học.
- Form giữ ngữ cảnh từ khóa học, lớp và chi nhánh; tải khóa/lớp đúng lựa chọn. Không mất dữ liệu khi gửi lỗi. Chỉ thông báo thành công khi backend trả 202.
- Lưu thời điểm, phiên bản đồng ý liên hệ và mã lượt gửi. Không tự tạo tài khoản, học viên, ghi danh, giữ chỗ hoặc thu tiền.
- Tạo lead NEW, nguồn WEBSITE; phân Staff đang làm việc đúng chi nhánh, ưu tiên người ít lead active, sau đó mới Admin. Không phân Superadmin/Teacher hay nhân sự nghỉ việc/khóa tài khoản/ngoài chi nhánh.
- Yêu cầu cùng điện thoại chuẩn hóa và cùng chi nhánh ghép vào lead đang NEW/CONTACTED/CONSULTING, bổ sung lịch sử; giữ nguyên hồ sơ, khóa quan tâm cũ và trạng thái. Lượt gửi vẫn lưu đầy đủ lựa chọn mới trong receipt và lịch sử.
- Lead LOST/CONVERTED không bị mở lại tự động. Yêu cầu ở chi nhánh khác được tiếp nhận riêng.
- Web quản lý hiện có nhận lead/lịch sử này qua API đang dùng; đã thêm nhãn lịch sử website. Nhân viên tiếp tục liên hệ, tương tác, hẹn đánh giá và chuyển học viên theo luồng cũ.
- Giao diện tìm khóa theo từ khóa, chương trình, chi nhánh và mức học phí công khai; khóa dùng chung xuất hiện tại chi nhánh hợp lệ. Bộ lọc học phí không suy ra giá chưa được công khai.
- Trang chi tiết khóa có đầu vào/đầu ra, thời lượng, học phí, lớp đang tuyển sinh và lịch tuần. Điện thoại có nút tư vấn cố định, có chừa khoảng trống tránh che nội dung.
- Danh sách và trang chi nhánh có địa chỉ, giờ hoạt động, điện thoại, bản đồ và các khóa học liên quan.
- Danh sách/chi tiết giáo viên chỉ dùng hồ sơ đã duyệt, giáo viên và tài khoản đang hoạt động tại ít nhất một chi nhánh công khai. Không trả phone/email/accountId/teacherId hoặc chứng chỉ cá nhân qua API public.
- Hiển thị ảnh chương trình/khóa/chi nhánh/giáo viên từ metadata đã duyệt. Không tự tạo ảnh cơ sở hay hồ sơ người thật. Nếu chưa có ảnh giáo viên thì dùng chữ viết tắt tên, không dùng ảnh người giả.
- Cập nhật header/footer, thẻ khóa, bố cục mobile, form, thông báo lỗi/trống/thành công, chính sách thông tin và sitemap nhánh/giáo viên.

## Chạy local

1. Khởi động lại academic-service và api-gateway; auth-service cần được khởi tạo schema và có nhân sự/chi nhánh đúng dữ liệu hiện tại.
2. Development `DDL_AUTO=update` sẽ thêm `website_inquiries` và `website_teacher_profiles`. Môi trường validate/none cần chạy `docs/migrations/V024__website_consultations_teachers.sql` sau V023. Migration chỉ thêm bảng, không chèn hoặc xóa dữ liệu vận hành.
3. `WEBSITE_FORM_SECRET` đã được cấu hình cùng giá trị trong `.env.local` backend và website local, không in secret ra chat/log. Restart để nạp cấu hình. Khi đưa lên môi trường khác, đặt cùng một chuỗi ngẫu nhiên tối thiểu 32 ký tự ở hai server; không đặt NEXT_PUBLIC cho biến này.
4. Website vẫn dùng `WEBSITE_API_URL=http://localhost:8080/api/v1/academic/public/catalog/`. Next server suy ra API tư vấn và giáo viên từ cùng public root.
5. Chạy `npm run dev` tại language-center-website. Mở http://localhost:3000.
6. Phải có chi nhánh đã xuất bản và nhân sự tiếp nhận hợp lệ trước khi gửi form. Thiếu nhân sự sẽ trả lỗi, giữ form để gửi lại; không báo thành công hoặc lưu lead vô chủ.

Không tự xuất bản dữ liệu demo. Cách xuất bản chương trình/khóa/chi nhánh/lớp vẫn theo [L4–L5](website-l4-l5.md). Nội dung thật, ảnh và hotline phải được trung tâm duyệt trước khi công khai.

## Luồng kỹ thuật

```text
Browser → POST /api/consultations (Next, cùng nguồn)
        → Gateway → POST /api/v1/academic/public/consultations
        → xác minh private key → lựa chọn công khai → khóa chi nhánh
        → chống trùng → chọn nhân sự → lead + lịch sử + receipt đồng ý
```

Next kiểm tra Origin với Host thực tế hoặc origin chính thức được cấu hình. Không tin X-Forwarded-Host. Form key chỉ nằm trên server. Browser không gọi API nhân sự và không được chọn người phụ trách/role/status.

`WebsiteLeadRouting` đọc bảng auth trong database dùng chung hiện tại, giống cơ chế khóa tài khoản của LeadAssigneeDirectory. Chỉ đọc ID, tên và điều kiện làm việc; không đọc mật khẩu. Nếu sau này tách database từng service, cần thay lớp này và nguồn giáo viên bằng API nội bộ được xác thực.

## Chống trùng và giới hạn

- `requestId`: UUID của một lượt gửi. Retry cùng nội dung giữ nguyên mã. Cùng mã khác nội dung trả 409. Public response chỉ có message, không tiết lộ leadId, hồ sơ hoặc việc điện thoại đã có trong hệ thống.
- Chuẩn hóa điện thoại di động Việt Nam về 10 chữ số bắt đầu 0; chấp nhận +84 và các dấu cách/gạch/ngoặc thường dùng.
- Tối đa 3 lượt gửi khác mã/điện thoại/giờ; kiểm tra receipt trong database để giữ giới hạn sau restart. Bucket trong bộ nhớ hỗ trợ chặn các nhánh gửi đồng thời.
- Chặn honeypot và yêu cầu quá 8 KiB; có giới hạn tổng 120 request/phút cho intake hợp lệ. Chưa tích hợp CAPTCHA hoặc dịch vụ chống bot bên ngoài.
- Có thể cấu hình `WEBSITE_CLIENT_IP_HEADER` ở Next **chỉ khi proxy tin cậy ghi đè header IP đó**. Ví dụ triển khai qua proxy do mình kiểm soát. Không bật tùy tiện với X-Forwarded-For nhận trực tiếp từ Internet. Mặc định trống: không tin IP client, không gom mọi khách vào IP gateway.
- Nếu bật header tin cậy, Next gửi HMAC của IP và backend giới hạn 10 lượt/15 phút cho mã kết nối. Không lưu IP thô vào receipt.
- Transaction intake dùng READ_COMMITTED và khóa chi nhánh để lượt đợi khóa nhìn thấy receipt/lead mới nhất. Lead và receipt chỉ ghi khi toàn bộ bước tiếp nhận thành công.
- `actorId=0`, `actorName=Website` là nguồn công khai của lịch sử, không phải tài khoản hệ thống giả.

## API

| API | Quyền / mục đích |
|---|---|
| POST `/api/v1/academic/public/consultations` | Next server với X-Website-Key; không cần tài khoản người học |
| GET `/api/v1/academic/public/catalog/COURSE` | Thêm keyword, maxTuition; hỗ trợ khóa dùng chung khi lọc branch |
| GET `/api/v1/academic/public/teachers?page=0&size=12&branch=slug` | Chỉ hồ sơ giáo viên hợp lệ đã xuất bản |
| GET `/api/v1/academic/public/teachers/{slug}` | Hồ sơ công khai hoặc 404 |
| GET `/api/v1/academic/website/teachers?page=0` | SUPER_ADMIN; metadata hồ sơ để biên tập |
| PUT `/api/v1/academic/website/teachers` | SUPER_ADMIN; tạo/sửa/xuất bản, có version |

Form body:

```json
{
  "requestId": "11111111-1111-4111-8111-111111111111",
  "fullName": "Nguyễn An",
  "phone": "0901234567",
  "email": null,
  "branchSlug": "chi-nhanh-1",
  "programSlug": null,
  "courseSlug": null,
  "classSlug": null,
  "note": "Cần tư vấn lịch học buổi tối",
  "consent": true,
  "website": ""
}
```

Không dùng UUID ví dụ này cho nhiều yêu cầu. Frontend tự tạo mã. Nhánh bắt buộc; có thể chọn chưa xác định chương trình. Nếu chọn lớp thì phải có khóa, đúng chi nhánh và còn tuyển sinh. API chỉ nhận consent=true.

Mã phản hồi: 202 ghi nhận (kể cả retry hợp lệ), 400 dữ liệu/lựa chọn sai, 401 thiếu/sai private key, 409 mã gửi xung đột, 413 nội dung quá lớn, 429 quá giới hạn, 503 chưa sẵn sàng/không có người tiếp nhận. Next không chuyển nguyên lỗi nội bộ cho khách.

## Duyệt hồ sơ giáo viên

Mẫu body PUT (thay teacherId bằng ID thật trong quản lý giáo viên):

```json
{
  "teacherId": 1,
  "version": 0,
  "slug": "giao-vien-an",
  "state": "DRAFT",
  "biography": "Nội dung giới thiệu được trung tâm và giáo viên đồng ý công khai",
  "teachingFocus": "Định hướng giảng dạy đã xác nhận",
  "imageUrl": null,
  "showQualification": false
}
```

Dùng JWT Superadmin, Content-Type application/json. Tạo mới version 0; sửa dùng version response mới nhất. Chuyển PUBLISHED khi đã duyệt, có mô tả và giáo viên đủ điều kiện. Bằng cấp mặc định ẩn; showQualification=true mới lấy trường qualification hiện hành của hồ sơ giáo viên. Không công khai trường certificate/email/phone. Sửa tên/chuyên môn/trình độ thực hiện ở hồ sơ nghiệp vụ; ảnh/bài giới thiệu là nội dung biên tập riêng. Chưa có màn hình biên tập/xuất bản: phần đó thuộc L8.

## Kiểm thử

Backend dùng H2 riêng, gồm tiếp nhận không cần tài khoản, phân người ít lead, Admin dự phòng, nhân sự nghỉ việc, lỗi không có người tiếp nhận, replay không trùng, cùng điện thoại khác chi nhánh, không ghi đè lead, giới hạn điện thoại, nguồn chưa xuất bản, bảng giá ẩn, hồ sơ giáo viên và quyền API.

Website dùng Chrome với API giả lập riêng để kiểm tra thao tác thật: giữ ngữ cảnh, bắt buộc đồng ý, mất phản hồi sau khi tiếp nhận, retry cùng mã, giữ dữ liệu khi lỗi, lọc khóa học, 404, sitemap và bố cục mobile. Fixture không nằm trong mã website và không được chèn vào MySQL.

Nghiệm thu trên nội dung MySQL thực tế cần xuất bản dữ liệu đã duyệt rồi thực hiện một đăng ký tư vấn có chủ đích; chưa tự gửi thông tin cá nhân hoặc tạo lead thử trong database vận hành.

Kết quả 15/11/2026: **63 test backend**, **35 kiểm tra Chrome**, TypeScript/production build website và production build web quản lý đều đạt. Web quản lý còn cảnh báo bundle chia sẻ lớn hơn 500 KiB có sẵn, không làm build thất bại.

Lệnh kiểm thử từ thư mục backend:

```powershell
cd academic-service
mvn -Dtest=WebsiteIntakeWorkflowTest,WebsiteIntakeLimiterTest,WebsiteCatalogTest,WebsiteSecurityTest,LeadCareWorkflowTest,LeadEmploymentTest,MultiBranchSecurityTest,StudentRouteSecurityTest,CourseWriteRulesTest,AcademicServiceApplicationTests test
cd ..
node .\scripts\website\verify-l67.cjs
```

Trước khi chạy script Chrome cần build website bằng `npm run build`. Script dùng các cổng local 3010, 19082, 19227 cho riêng lần kiểm tra và tự đóng tiến trình đã tạo. Kết quả và ảnh nằm ở `docs/validation/l6-l7/` trong website. Ảnh kiểm thử có dữ liệu tổng hợp, không đại diện nội dung đã xuất bản thực tế.
