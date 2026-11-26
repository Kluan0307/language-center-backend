# L8–L9 — Quản trị nội dung và nghiệm thu tích hợp

Ngày cập nhật: 16/11/2026.

## L8

Web quản lý có mục **Nội dung website**, đường dẫn `/website-content`, dùng Ant Design và styled-components theo cấu trúc hiện có.

- Superadmin: chọn bản ghi nghiệp vụ, biên tập chương trình/chi nhánh/khóa/lớp/giáo viên, xem trước nội dung, lưu, xuất bản, ẩn và đọc lịch sử.
- Admin: chỉ xem/biên tập bản nháp BRANCH/COURSE/CLASS thuộc các chi nhánh được phân công. Không sửa nội dung đang công khai, không tự xuất bản/ẩn, không chỉnh chương trình toàn hệ thống hoặc giáo viên công khai.
- Staff/Teacher/Student: không có màn hình hoặc quyền API quản trị website.
- Giá trị tên, giá, địa chỉ, lịch tiếp tục lấy từ bản ghi gốc. Màn hình xem trước chỉ mô phỏng nội dung biên tập; xuất bản kiểm tra lại điều kiện nghiệp vụ ở backend.
- Mỗi lần lưu/xuất bản/ẩn ghi một event gồm người sửa, thời điểm, trạng thái và bản chụp thuộc tính. Lịch sử được đọc qua API riêng, tối đa 100 sự kiện mới nhất/bản ghi; không có API sửa/xóa lịch sử.
- Cập nhật dùng version; xung đột trả 409 và cần tải lại bản ghi. Lỗi lưu được hiển thị trong cửa sổ biên tập.

## Cách sử dụng

1. Khởi động lại academic-service để nạp code và schema. `DDL_AUTO=update` tự thêm bảng `website_edit_events`; validate/none cần áp dụng V025 sau V023/V024.
2. Chạy web quản lý, đăng nhập Superadmin và mở **Nội dung website**.
3. Chọn loại → Thêm nội dung → chọn bản ghi nghiệp vụ → nhập slug và mô tả → Lưu bản nháp.
4. Xem trước và kiểm tra thông tin/ảnh được phép công khai.
5. Xuất bản theo thứ tự chương trình → chi nhánh → khóa → lớp. Hồ sơ giáo viên cần chi nhánh đã công khai và nhân sự đang hoạt động.
6. Mở website công khai và tải lại để kiểm tra. Cần ẩn nội dung trước khi đổi slug đang công khai.

Không tự công khai hoặc thay đổi dữ liệu hiện có. Các script kiểm thử không chèn lead mẫu vào database vận hành.

## API bổ sung

- GET `/api/v1/academic/website/sources/{kind}?page=0`: chọn nguồn; Superadmin/Admin, lọc theo quyền; 100/trang.
- GET `/api/v1/academic/website/history/{kind}/{id}`: lịch sử, lọc theo quyền nguồn.
- GET `/api/v1/academic/website/teacher-sources?page=0`: nguồn giáo viên; Superadmin.
- GET `/api/v1/academic/website/teacher-history/{id}`: lịch sử hồ sơ công khai; Superadmin.
- GET/PUT publications mở cho Admin nhưng service kiểm soát phạm vi chi nhánh và chỉ cho lưu nháp chưa công khai. Không dựa riêng vào việc ẩn nút trên frontend.

## L9 — kết quả tự động

**106 test backend trong 13 suite đã chọn đạt**, gồm:

- Biên tập, lịch sử, phân quyền nhánh, không cho Admin ẩn/sửa nội dung đã xuất bản.
- Nháp không công khai, nguồn tắt/đủ chỗ/không còn tuyển sinh bị ẩn.
- Website inquiry → lead được phân Staff → tương tác tư vấn → chuyển học viên đúng chi nhánh; chuyển lại không tạo thêm học viên.
- Replay, chuẩn hóa điện thoại, đăng ký ở các nhánh khác nhau, không có nhân sự nhận, giới hạn gửi và quyền API.
- Hồi quy lead, nhân sự, đăng ký học, xếp lớp, quá trình học và quyền student.

**35 kiểm tra Chrome website công khai đạt**, gồm giữ ngữ cảnh, đồng ý liên hệ, mất phản hồi sau khi nhận, retry cùng mã, bộ lọc, 404, sitemap, responsive 320/390px và không lộ key vào client bundle. Production build web quản lý đạt; còn cảnh báo bundle chia sẻ >500 KiB như trước.

Kiểm thử sử dụng **H2 và HTTP fixture riêng**. Không coi kết quả này là nghiệm thu trên MySQL vận hành, APK hoặc kiểm thử trực quan tương tác đầy đủ của màn hình CMS mới; phần CMS đã kiểm tra build và quyền backend.

## Các bước nghiệm thu có người vận hành

| Tình huống | Kết quả cần xác nhận |
|---|---|
| Superadmin tạo nháp → xem trước → xuất bản | Nội dung chỉ xuất hiện sau xuất bản, tên/giá/lịch đúng bản ghi gốc |
| Admin nhánh A sửa nháp A và thử truy cập B | A cho phép, B bị từ chối; không có quyền xuất bản |
| Khách gửi tư vấn từ lớp đã công khai | Đúng chi nhánh, Staff/Admin hợp lệ nhận lead; không tạo tài khoản tự động |
| Nhân viên chăm sóc → chuyển học viên | Một hồ sơ học viên, mã học viên hợp lệ, lịch sử giữ đầy đủ |
| Đăng ký lớp và xem trên APK bằng tài khoản học viên thật | Đúng lớp/lịch/điểm/điểm danh/học phí trong phạm vi cá nhân |
| Ẩn nội dung hoặc khóa chi nhánh | Website không tiếp tục cung cấp lớp tuyển sinh không hợp lệ |

Các kiểm tra trên MySQL/thiết bị thật còn cần thực hiện trước khi đóng nghiệm thu vận hành L9. L10 (hosting/domain/giám sát) chưa triển khai.
