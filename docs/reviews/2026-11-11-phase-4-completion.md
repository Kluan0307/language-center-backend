# Giai đoạn 4 — Bộ demo qua nghiệp vụ thật

Ngày: **11/11/2026**. Hoàn thành F06/F07/F08 trong phạm vi bộ demo trên schema riêng. Không thay thế nghiệm thu hồi quy toàn hệ thống ở giai đoạn5.

## Kết quả cuối

Lượt tái tạo sạch: `demo-g4-20261111-9c94df`; database `lc_rehearsal_demo_g4_1794386814337_04ad`.

- **40 kiểm tra API** qua gateway/services thật, **8 đối soát SQL/source**, **4 lượt đọc UI** với API/MySQL thật đều đạt.
- [Summary](../../storage/acceptance/demo-g4-20261111-9c94df/phase4-summary.json), [core20](../../storage/acceptance/demo-g4-20261111-9c94df/g4-core.json), [details20](../../storage/acceptance/demo-g4-20261111-9c94df/g4-details.json), [SQL](../../storage/acceptance/demo-g4-20261111-9c94df/database-proof.json), [UI](../../storage/acceptance/demo-g4-20261111-9c94df/browser-demo.json).
- Chạy lại bằng runner từ schema sạch, exit0: [log tái tạo](../../storage/demo-phase4-reproducibility.log). Runtime chỉ dùng các cổng riêng28080–28084/25173/29388; đã dừng sau kiểm tra.
- Backend được package từ source hiện tại với skipTests; không tuyên bố đã chạy lại335 Java tests. FE dùng source hiện tại trên Vite; không phải bản release ZIP cũ.
- Tạo schema chỉ từ cấu trúc, chạy prepare-existing-schema +13migration. Provision Super Admin bằng application bootstrap, hai chi nhánh bằng BranchSeed hiện có. Toàn bộ dữ liệu nghiệp vụ và các tài khoản khác được tạo qua API; không chèn enrollment/snapshot/history thủ công bằng SQL.
- Dấu vân tay **dữ liệu** `language_center_db` trước/sau trùng nhau. Không reset tài khoản, không sửa .env.local, không migrate nguồn. Các schema/lượt lỗi trước đó giữ riêng, không DROP tự động.

## Bộ dữ liệu

| Dữ liệu | Số lượng |
|---|---:|
| Tài khoản / chi nhánh |6 /2|
| Chương trình / trình độ |3 /8|
| Khóa / lớp / phòng |4 /4 /3|
| Học viên / đăng ký / học phí |10 /8 /8|
| Buổi học |25|
| Đánh giá đầu vào / ngoại lệ |5 /3|
| Phiếu thu |4|
| Bài kiểm tra / điểm |1 /2|
| Bản kết quả chính thức |4:2 đã mở lại,2 hiện hành|

ID bắt đầu nhỏ từ schema mới. Tên và mã ngắn: GE, ON-B1, GT-A2, IELTS-CB, TOEIC-CB, P1/P2; không tái đánh số dữ liệu nguồn.

## Các tình huống có thể trình bày

1. Lead → liên hệ → chuyển Minh Anh thành học viên; đánh giá B1 và ghi danh lớp Ôn tập B1.
2. Quốc Bảo A2 bị chặn trực tiếp, Staff đề nghị, Admin duyệt, ngoại lệ được dùng đúng đăng ký. Staff tự duyệt bị403.
3. Giao tiếp A2 yêu cầu A1: có học viên đủ mức, thiếu đánh giá, sai mức và khác chương trình. Có yêu cầu bị từ chối và yêu cầu để lại chờ duyệt.
4. Thu học phí hai đợt, retry không tạo phiếu trùng; có đã đóng đủ/một phần/chưa đóng. Tổng phải thu25triệu, thực thu7triệu, còn18triệu.
5. Điểm danh → hoàn tất180phút; thiếu bài bắt buộc chưa thể đánh giá đủ; điểm85/40 tạo PASS/FAIL theo ngưỡng60%.
6. Chốt kết quả → hoàn tất cả hai đăng ký → đóng lớp; nợ3triệu của Quốc Bảo vẫn giữ, không bị xóa hoặc tự chặn học thuật.
7. Mở lại kết quả trước khi mở lớp, sửa85→86 kèm lý do; bản kết quả cũ không đổi. Xác nhận lại, cập nhật tham chiếu hoàn thành, đóng lại lớp.
8. Ba lớp tương lai có24buổi trong4tuần để xem Time-grid; kiểm không trùng giáo viên/phòng. Tuần bắt đầu **16/11/2026** trong lượt cuối; Teacher chỉ thấy4buổi/tuần của mình, SA thấy6buổi/tuần toàn hệ thống.

Workshop ba giờ dùng để kiểm tra vòng đời; không chứng nhận tiến cấp CEFR sau ba giờ. Hai khóa cơ bản OPEN_ENTRY là chủ ý; khóa yêu cầu đánh giá dùng tập mức được chấp nhận rõ ràng, không tự quy đổi điểm.

## Cách mở và tạo lại

[Hướng dẫn, lệnh và thông tin tài khoản](../../scripts/demo-phase4/README.md).

```powershell
# Mở bộ dữ liệu đã nghiệm thu, không tạo lại
.\scripts\demo-phase4\open.ps1

# Dừng runtime demo riêng
.\scripts\demo-phase4\open.ps1 -Action stop

# Tái tạo một lượt/schema mới, giữ các lượt cũ
.\scripts\demo-phase4\run.ps1
```

URL demo `http://127.0.0.1:25173`. Tài khoản superadmin/admin/staff/teacher/teacher2/staff2 dùng mật khẩu được sinh riêng trong file private tại run directory; không đưa mật khẩu/token vào tài liệu công khai. Runner workshop cần chạy sau03:00 giờ Việt Nam vì dùng buổi00:00–03:00 ngày tạo. Browser dùng phiên đăng nhập qua API để đọc UI; chưa phải kiểm tra CAPTCHA/đăng nhập UI.

## Phát hiện mới: F13 — schema nguồn chưa khớp migration

**P1, xác nhận bằng runtime và metadata.** Lượt đầu sao chép cấu trúc nguồn rồi tạo khóa chỉ có expectedMinutes bị409 do `courses.duration` còn NOT NULL. [Preflight nguồn cuối](../../storage/acceptance/demo-g4-20261111-9c94df/source-schema-preflight.json) vẫn ghi `legacyDurationNullable=NO`. V011 quy định chuyển cột legacy này sang nullable để không bịa đơn vị duration.

Đã sửa quy trình prepare của demo để áp dụng migration đầy đủ. **Chưa sửa nguồn.** Đây là vấn đề triển khai schema nguồn, không phải sửa fixture bằng cách nhét một duration giả để qua lỗi.

Tiêu chí xử lý tiếp: backup nguồn và tài liệu/file liên quan; kiểm đủ các migration trên bản sao; xác minh tạo khóa chỉ expectedMinutes và bảo toàn dữ liệu; sau đó mới cập nhật schema đang dùng theo quy trình có kiểm chứng. Không gọi database chính sẵn sàng chỉ vì bộ demo riêng đã chạy.

## Các lần thử không đạt và giới hạn

Lượt đầu không chạy đủ migration; lượt xây script chi tiết phát hiện thiếu teacherName, đọc sai cấp policy trong response và thứ tự mở lại. Đã sửa kịch bản theo contract thực, không nới guard backend. Lỗi so sánh object/order và newline của dump được sửa ở verifier; không thay fingerprint đầu vào để ép PASS. Bằng chứng lỗi ở các run/log riêng được giữ. Lượt cuối tái tạo sạch thành công không dựa vào các lần sửa giữa chừng.

Không thay Java/API/FE trong giai đoạn4, không tuyên bố đã thử toàn bộ race/outage/bảo lưu/chuyển lớp/tài liệu hoặc production. Các phần đó thuộc giai đoạn5. Bản fixture SQL xem UI cũ vẫn tồn tại như tài liệu lịch sử; không dùng làm bằng chứng nghiệp vụ mới.
