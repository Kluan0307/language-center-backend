# Giai đoạn5 — Nghiệm thu hệ thống hiện tại và phục hồi

Ngày: **11/11/2026**. **Hoàn thành trong phạm vi kiểm thử local với source FE hiện tại, năm JAR, gateway, MySQL và Edge thật.** Chưa là qualification bản ZIP mới hoặc production.

## Kết quả và bằng chứng

Lượt chính `20261111-064b2f08`, schema `lc_rehearsal_1794387348219_e050cb`; bản phục hồi `20261111-064b2f08-restore`, schema `lc_rehearsal_1794387921369_856f44`.

[Báo cáo tổng hợp](../../storage/acceptance/20261111-064b2f08/phase5-summary.json), [baseline86](../../storage/acceptance/20261111-064b2f08/acceptance-summary.json).

| Nhóm | Expected | Actual / bằng chứng |
|---|---|---|
| Backend Maven verify | Không failure/error/skipped, package thành công | **335/335**: Auth54, Academic159, Finance40, Teaching74, Gateway8; [Surefire](../../storage/phase5-backend-tests.json), [log](../../storage/phase5-backend.log) |
| Frontend | Lint/build/tests đạt | **96/96**, lint/build đạt; [log](../../storage/phase5-frontend.log) |
| Ops/integrity | Công cụ backup và integrity hoạt động | **12/12**; [log](../../storage/phase5-operations-tests.log) |
| Runtime/API/MySQL baseline | Luồng chính, CRUD, quyền, race/outage/UI/đối soát | **86/86**:20luồng chính+38ca rủi ro+8CRUD+12UI+8SQL; [summary](../../storage/acceptance/20261111-064b2f08/acceptance-summary.json) |
| UI cập nhật trước restore | Đăng nhập4role, menu/trang mới, lịch/filter/URL/audit/mobile | **12/12**; [report](../../storage/acceptance/20261111-064b2f08/phase5-live-ui.json) |
| Smoke trước restore | Health/readiness/protected routes/request ID | **19/19**; [report](../../storage/acceptance/20261111-064b2f08/smoke.json) |
| Backup/restore bản sao đã dừng writer | Restore đủ bảng/dữ liệu/file và replay migration không làm lệch dữ liệu | **Đạt**:53bảng, fingerprint/số dòng khớp,2file được tham chiếu; [proof](../../storage/acceptance/20261111-064b2f08/phase5-recovery.json) |
| UI sau restore | Cùng12ca trên bản phục hồi | **12/12**, có đăng nhập thật4role; [report](../../storage/acceptance/20261111-064b2f08-restore/phase5-live-ui.json) |
| Smoke sau restore | Runtime khởi động và bảo vệ route đúng | **19/19**; [report](../../storage/acceptance/20261111-064b2f08-restore/smoke.json) |
| F13/source schema | V011 sau backup kiểm chứng, không đổi dữ liệu nghiệp vụ | **Đạt**:duration NO→YES, fingerprint/số dòng giữ nguyên; [proof](../../storage/acceptance/20261111-064b2f08/source-v011-migration.json) |

Không cộng các nhóm trên thành số lượng “chức năng đã kiểm thử”: ca có thể chạm cùng nghiệp vụ ở nhiều mức.86 đã chứa12UI baseline;24UI bổ sung là nhóm riêng trước/sau restore.

## Phạm vi đã xác minh

- JWT sai/hết hạn, phiên bị thu hồi, role/chi nhánh/đối tượng không có quyền và Admin chỉ có một phần phạm vi Teacher nhiều chi nhánh.
- Đánh giá đầu vào, ngoại lệ sai/cũ/đồng thời, ghi danh lớp đầy và hai yêu cầu tranh chỗ cuối.
- Thu học phí nhiều đợt, request lặp/đồng thời, chênh lệch số dư, outage Finance và retry sau commit; đối soát doanh thu/công nợ.
- Điểm0 khác chưa chấm, bài bắt buộc, xác nhận/mở lại/chốt lại kết quả, race sửa nguồn với chốt, refs hoàn thành và closure không bị bypass.
- Hồ sơ Teacher, đổi trạng thái, bàn giao/ràng buộc còn việc; CRUD học viên/Teacher/khóa/lớp theo API hiện có.
- Tài liệu/revision/duyệt/tải và snapshot hiện hành; giữ hồ sơ học sau restart service/reload.
- UI thật tạo và reload học viên, giữ phản hồi scope cũ không ghi đè scope mới, mất phiên giữa lúc nhập không lưu được dữ liệu.
- Menu Chương trình & trình độ/Quản trị viên chỉ SA; Admin/Staff bị chặn trực tiếp. Lịch dùng teacher.profileId, room/class filter thực, không có truy vấn danh sách size10 thừa trong calendar; reload giữ URL; lịch sử hiển thị bảng trước/sau; Teacher mobile không tràn trang.
- UI login đi qua form/CAPTCHA của ứng dụng. Automation quan sát ký tự canvas để nhập CAPTCHA, không tuyên bố CAPTCHA an toàn trước bot.

## F13 — đã sửa schema nguồn có kiểm soát

Nguồn `language_center_db` còn `courses.duration NOT NULL`, không khớp V011. Sau khi bản sao migration và ca tạo khóa expectedMinutes/CRUD đạt, đã:

1. Tạo backup nguồn mới, verify checksum và kiểm dữ liệu ổn định. Backup nguồn dùng snapshot online, được ghi nhãn **online-rehearsal-unqualified**, không giả là backup production có dừng toàn bộ writer.
2. Áp dụng đúng `V011__verified_duration_plans.sql`, với lock timeout10giây. Cột legacy duration cho phép NULL; không nhét duration giả hay tự quy đổi đơn vị.
3. Đối soát dữ liệu và số dòng mọi bảng trước/sau: khớp, tài khoản/mật khẩu/hồ sơ/nghiệp vụ không đổi. Không xóa hoặc reset database gốc, không đổi `.env.local`.

F13 được đóng trong phạm vi lệch nullability đã phát hiện. Không suy rằng mọi môi trường khác đã được migrate; triển khai máy khác vẫn phải backup/preflight theo runbook.

## Backup/restore

Dừng JVM writer của **runtime thử riêng** trước khi backup, verify manifest và phục hồi sang schema mới; không restore đè database gốc. Đối chiếu fingerprint toàn dữ liệu sau replay13migration, số dòng53bảng, và file references/checksum. Bản phục hồi khởi động prod/validate, chạy19smoke và12UI bổ sung thành công. Cả runtime gốc thử và runtime phục hồi đã dừng; dữ liệu/evidence/backup giữ riêng.

## Lỗi đã gặp khi chạy và cách xử lý

- Kịch bản browser cũ chờ nút Lịch sử trực tiếp và nhãn đồng bộ cũ. Cập nhật selector mở menu `⋯` và nhãn hiện tại; giữ các assertion về nguồn kết quả, quyền và dữ liệu. Report lỗi được lưu riêng, không sửa ứng dụng để qua test.
- PowerShell Add-Content lỗi `Stream was not readable` khi cleanup runtime. Thay bằng append UTF-8 có retry cho log lifecycle; các lượt start/stop sau đó và restore hoàn tất. Chỉ khớp đường dẫn JAR runtime riêng khi dừng tiến trình.
- Sau restart cần đợi đủ readiness trước browser; bổ sung bước chờ, không coi lỗi kết nối lúc khởi động là PASS.
- Tổng hợp log PowerShell phải đọc đúng UTF-16LE/BOM; bộ tổng hợp đã sửa cách đọc, không đổi log hay expected để ép PASS.

Không phát hiện thêm lỗi nghiệp vụ chặn nghiệm thu trong bộ ca đã chạy. Đây không phải cam kết không còn lỗi ở mọi tình huống.

## Công cụ và bước tiếp theo

[Hướng dẫn/scripts giai đoạn5](../../scripts/phase5/README.md). `run.ps1` mới tập hợp những bước đã chạy riêng và đã kiểm cú pháp; chưa gán bằng chứng một lượt chạy nguyên file orchestration mới. Source của UI/API đã được kiểm ở các lệnh/runtime thật nêu trên.

Tiếp theo **giai đoạn6**: đồng bộ README/API/ma trận/sơ đồ/ảnh/báo cáo với các thay đổi11/11; bổ sung khảo sát thực tế do người dùng cung cấp; đóng gói release mới và qualification đúng artifact đó; diễn tập trên máy bảo vệ. Không chỉnh gói ZIP bất biến10/11 để gắn kết quả mới. TLS/Internet/load, usability đầy đủ/accessibility và hồ sơ nộp theo mẫu khoa vẫn cần công việc riêng.
