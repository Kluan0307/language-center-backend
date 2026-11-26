# Bộ dữ liệu mẫu bốn chi nhánh

**Tài liệu bộ cũ:** dữ liệu nghiệp vụ này đã được thay ở D4 ngày 16/11/2026. Tài khoản và phân quyền giữ nguyên; ID/mã học viên và lớp đã thay. Dùng [bàn giao D1–D4](../course-classification/d1-d4-handoff.md) cho bộ hiện tại.

Đã nạp ngày 14/11/2026 vào `language_center_db`, mở rộng từ bộ mẫu G4–G6. Giữ nguyên ID, mã và lịch sử hiện có; không xóa bảng hoặc xóa dữ liệu cũ.

**Mật khẩu chung của các tài khoản mẫu: `123456`.** Mật khẩu được lưu bằng BCrypt; tất cả phiên đăng nhập cũ đã bị thu hồi sau khi cập nhật mật khẩu.

| Chi nhánh | Admin | Staff | Học viên | Khách tiềm năng | Khóa học | Lớp |
|---|---|---|---:|---:|---:|---:|
| CN01 – Cơ sở 1 | admin1 | staff1, staff2 | 100 | 20 | 4 | 4 |
| CN02 – Cơ sở 2 | admin1 | staff3, staff4 | 100 | 20 | 4 | 4 |
| CN03 – Cơ sở 3 | admin2 | staff5, staff6 | 100 | 20 | 4 | 4 |
| CN04 – Cơ sở 4 | admin2 | staff7, staff8 | 100 | 20 | 4 | 4 |

Tài khoản:

- `superadmin`: SUPER_ADMIN.
- `admin1`, `admin2`: ADMIN, hai nhóm chi nhánh không chồng nhau.
- `staff1` đến `staff8`: STAFF; mỗi người một chi nhánh như bảng.
- `teacher1` đến `teacher8`: TEACHER; giữ phân công nhiều chi nhánh đã có của teacher1/teacher4. teacher5/teacher6 thuộc CN03, teacher7/teacher8 thuộc CN04.
- `hv0001` đến `hv0400`: STUDENT, liên kết đúng hồ sơ HV0001–HV0400.

Có 419 tài khoản: 1 superadmin, 2 admin, 8 staff, 8 teacher và 400 học viên.

## Dữ liệu đào tạo và nghiệp vụ

- Ba chương trình GE, IELTS, TOEIC; dùng chương trình/khóa học theo mô hình hiện có, không thêm bảng môn học riêng.
- Mỗi chi nhánh có khóa GE-A1, GE-A2, IELTS-F, TOEIC-F; 12 phòng, sức chứa 30; lớp tối đa 25.
- Tổng 16 lớp, 128 buổi học, 400 đăng ký và 400 khoản học phí. Mỗi lớp có 25 đăng ký đang học/bảo lưu. Giữ một hồ sơ bảo lưu HV0013 từ bộ mẫu trước; lớp đó có 24 người đang học và một người bảo lưu.
- Học viên mới vào các lớp cũ có thời gian hiệu lực từ lần nhập học mới; không tự xuất hiện trong điểm danh hoặc danh sách thi trước ngày vào học. Hai chi nhánh mới có dữ liệu lịch sử mẫu theo kế hoạch lớp.
- 269 phiếu thu, có đóng đủ/đóng một phần/chưa đóng/quá hạn; tổng tiền đã đóng khớp sổ thu và điều chỉnh.
- 888 bản ghi điểm danh hợp lệ; 16 đánh giá đầu vào; 4 bài thi, 56 điểm và 4 đợt công bố điểm; 8 yêu cầu hỗ trợ.
- Không tạo giao dịch VNPAY giả hoặc tài liệu không có file thật.

## Kiểm tra và chạy lại

Đã kiểm tra trên bản sao MySQL trước khi áp dụng; 15 điều kiện kiểm tra nằm trong giao dịch, sai điều kiện sẽ không commit. Sau khi nạp có thêm 10 kiểm tra độc lập, bao gồm phân công, 25 đăng ký/lớp, lịch trùng, người giám hộ, điểm công bố, người thu tiền và BCrypt khớp `123456`.

```powershell
# Chỉ kiểm tra bộ dữ liệu đang có:
node scripts/identity/verify-four-branches.cjs

# Sinh kế hoạch và thử trên bản sao, chưa cập nhật database dự án:
node scripts/identity/expand-four-branches.cjs

# Đối chiếu hiện trạng, thử trước rồi áp dụng vào database dự án:
node scripts/identity/expand-four-branches.cjs --apply
```

Lần chạy tiếp sẽ kiểm tra số lượng hiện có; không dùng để ghi đè một chi nhánh đã vượt 100 học viên/20 lead. Script đặt lại mật khẩu mẫu và ghi thêm sự kiện bảo mật khi chạy `--apply`. Không chạy trực tiếp SQL đã sinh lần trước để nạp lặp vì các ID đã tồn tại.

[Manifest hiện tại](four-branches-manifest.json) · [SQL đã áp dụng](seed-four-branches.sql).

Backup trước khi nạp và kết quả kiểm tra nằm trong `storage/four-branches/`; `latest.json` ghi đúng đường dẫn backup của lần áp dụng. Bộ này thay số lượng và mật khẩu của bộ mẫu G4–G6; không dùng `load-demo.cjs --verify-local` của bộ 30 học viên để kiểm tra bộ 400 học viên.

Tải lại web và đăng nhập lại. Nếu trình duyệt còn giữ danh sách chi nhánh cũ, đăng xuất rồi đăng nhập bằng tài khoản tương ứng.
