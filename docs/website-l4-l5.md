# L4–L5 — Đồng bộ nội dung website và hệ thống quản lý

## Kết quả

- L4: bảng `website_publications` bổ sung metadata xuất bản, không sao chép bảng nghiệp vụ.
- L5: API chỉ đọc, phân trang; website Next.js lấy dữ liệu qua gateway bằng server components.
- Tên, mã, địa chỉ, số liên hệ, học phí, thời lượng phút, ngày học và lịch tuần đọc từ bản ghi nguồn tại thời điểm truy vấn.
- Mô tả, đối tượng, mục tiêu đầu ra, danh sách nội dung, ảnh, giờ hoạt động và link bản đồ là nội dung biên tập riêng.
- Không tự xuất bản dữ liệu demo hoặc dữ liệu đang có. Chưa xuất bản thì website hiển thị trạng thái đang cập nhật.

## Quy tắc

1. PROGRAM trỏ đến `programs`, COURSE trỏ đến `courses`, BRANCH trỏ đến `branches`, CLASS trỏ đến `school_classes`.
2. Nháp không công khai; slug duy nhất theo loại. Đổi slug đang công khai bị chặn; cần ẩn trước khi đổi. Chưa có cơ chế chuyển hướng URL cũ.
3. Chương trình phải active. Chi nhánh phải ACTIVE và có địa chỉ khi xuất bản.
4. Khóa học phải OPEN, chương trình đã xuất bản, chi nhánh (nếu có) đã xuất bản/ACTIVE. `programId` phải khớp chính sách đầu vào hiện hành nếu có. Khóa dùng chung có branchId null.
5. Lớp chỉ công khai khi UPCOMING, ngày bắt đầu còn ở tương lai, còn chỗ, khóa và chi nhánh đủ điều kiện. Lớp đang học/đã kết thúc/đủ chỗ tự biến mất khỏi danh sách tuyển sinh.
6. Học phí mặc định ẩn (`showTuition=false`); bật mới trả về giá gốc hiện hành của khóa. Không tự suy diễn đơn vị của duration cũ; chỉ hiển thị expectedMinutes đã xác định.
7. API công khai không trả accountId, profile học viên, công nợ, phiếu thu, teacherId, teacherName, phòng nội bộ hoặc người xuất bản. Lịch tuần chỉ gồm thứ/giờ bắt đầu/kết thúc.
8. Chỉ SUPER_ADMIN quản lý xuất bản trong giai đoạn này. Giao diện quản trị nội dung/phân quyền biên tập chi nhánh thuộc L8.

## Chạy local

1. Khởi động lại academic-service để nạp code/schema mới, rồi chạy api-gateway.
2. Development đang dùng `DDL_AUTO=update`: Hibernate thêm bảng mới. Môi trường validate/none: chạy migration `docs/migrations/V023__website_publications.sql` trước. Không chạy reset/seed database.
3. Tại `language-center-website/.env.local`:

```dotenv
WEBSITE_API_URL=http://localhost:8080/api/v1/academic/public/catalog/
```

Đây là biến chỉ dùng trên Next server, không có prefix NEXT_PUBLIC. Khi deploy phải dùng URL backend mà server hosting truy cập được, không dùng localhost của máy cá nhân.

4. Chạy `npm run dev` trong website và mở http://localhost:3000.
5. API này không dùng JWT; không đưa JWT/DB password vào môi trường website. Browser không gọi gateway trực tiếp nên không cần mở CORS thêm cho website. Gateway đã có route `/api/v1/academic/**`.

## API công khai

```text
GET /api/v1/academic/public/catalog/{kind}?page=0&size=24
GET /api/v1/academic/public/catalog/{kind}/{slug}
```

`kind`: PROGRAM, COURSE, BRANCH, CLASS (viết hoa). Trả danh sách `{items,page,size,hasNext}`. Page bắt đầu 0, tối đa 10000; size 1–100. Thứ tự ổn định theo sourceId.

- COURSE nhận `program=<slug>` và `branch=<slug>`.
- CLASS nhận `course=<slug>` và `branch=<slug>`.
- Bộ lọc không phù hợp loại trả 400; nội dung nháp/ẩn/không còn hợp lệ trả 404 khi xem chi tiết.
- Kết quả lọc điều kiện công khai trước khi phân trang, không làm rỗng trang vì lọc sau.

Ví dụ kiểm tra:

```powershell
Invoke-RestMethod 'http://localhost:8080/api/v1/academic/public/catalog/PROGRAM'
Invoke-RestMethod 'http://localhost:8080/api/v1/academic/public/catalog/COURSE?program=tieng-anh'
```

## Xuất bản nội dung (SUPER_ADMIN)

API nội bộ:

```text
GET /api/v1/academic/website/publications/{kind}?page=0
PUT /api/v1/academic/website/publications/{kind}
```

GET trả metadata hiện có (tối đa 50/trang); chọn sourceId thật từ API nghiệp vụ. Tạo mới dùng version 0; cập nhật dùng version mới nhất trong response. Sai version trả 409. Dữ liệu nhập sai trả 400; slug trùng trả 409. URL ảnh/bản đồ phải HTTPS.

Ví dụ PowerShell (điền ID thật, không dùng ví dụ này để tự xác nhận dữ liệu demo):

```powershell
$token = Read-Host 'JWT của superadmin'
$headers = @{ Authorization = "Bearer $token" }
$body = @{
  sourceId = 1
  version = 0
  slug = 'tieng-anh'
  state = 'DRAFT'
  summary = 'Nội dung đã được trung tâm xác nhận'
  audience = 'Đối tượng phù hợp'
  outcomes = 'Mục tiêu học tập đã được xác nhận'
  topics = "Nghe`nNói`nĐọc`nViết"
  showTuition = $false
} | ConvertTo-Json
$saved = Invoke-RestMethod -Method Put -Uri 'http://localhost:8080/api/v1/academic/website/publications/PROGRAM' -Headers $headers -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($body))
$saved
```

Đổi state thành PUBLISHED và dùng version response để xuất bản. Để ẩn, gửi cùng sourceId/slug/version mới nhất, state DRAFT và metadata cần giữ.

Thứ tự: PROGRAM → BRANCH → COURSE → CLASS. COURSE cần programId (ID chương trình gốc), showTuition do người quản trị quyết định. Các loại khác không nhận programId/showTuition=true. BRANCH có thể thêm openingHours/mapUrl; ảnh giới thiệu dùng imageUrl. L7 sẽ hoàn thiện hiển thị ảnh/nội dung tuyển sinh; L8 cung cấp màn hình thao tác thay cho API.

## Đồng bộ và lỗi

- Next fetch dùng `cache:no-store`, timeout 5 giây. API trả `Cache-Control:no-store`. Mỗi lần tải/navigate từ server đọc nguồn mới; không cần rebuild website sau khi xuất bản/sửa/ẩn. Trang đã mở cần tải lại để nhận thay đổi.
- Không dùng dữ liệu mẫu dự phòng khi backend lỗi. Trang chủ hiển thị thông báo; các trang dữ liệu có error boundary và nút thử lại. Có loading, empty state, 404 nội dung không công khai.
- Sitemap chỉ có URL nội dung đang công khai, lấy từ cùng API. Backend lỗi thì không trả sitemap rỗng giả thành công.
- Form tư vấn và tiếp nhận lead là L6, chưa thuộc thay đổi này.

## Kiểm chứng

Backend: `mvn -Dtest=WebsiteCatalogTest,WebsiteSecurityTest test` trong academic-service; H2 riêng, không dùng database dự án. Bao gồm private-by-default, giá ẩn/giá hiện hành, điều kiện tuyển sinh, trạng thái cha, lọc/phân trang, sửa xung đột và quyền API.

Website: `npm run typecheck`, `npm run build`; kiểm thử tích hợp bằng backend giả lập chỉ nằm trong script kiểm thử. Không thêm fixture vào website hoặc MySQL.

Kết quả 15/11/2026: 33 test backend (12 test mới và 21 test hồi quy/khởi động) đạt; TypeScript và production build đạt; 16 kiểm tra Chrome với API giả lập đạt, gồm desktop/mobile 320px, timetable, 404, ẩn giá, cập nhật không rebuild và mất kết nối. Chưa chạy nghiệm thu trên nội dung MySQL thực tế vì chưa duyệt/xuất bản dữ liệu. Báo cáo: `docs/website-l4-l5-validation.json` trong backend và `docs/validation/l4-l5/` trong website.
