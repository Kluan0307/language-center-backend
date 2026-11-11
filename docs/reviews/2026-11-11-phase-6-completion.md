# Giai đoạn 6 — Đồng bộ tài liệu và bàn giao release

Hoàn thành phần kỹ thuật ngày 11/11/2026. Snapshot được đóng trước nghiệm thu; hồ sơ qualification độc lập gắn đúng SHA256 manifest, không sửa bản release sau khi kiểm thử.

## Sản phẩm

- [ZIP bàn giao](../../storage/releases/language-center-20261111.zip): 278.996.542 byte, 990 entry đã đối chiếu SHA256 từng file với nguồn đóng gói.
- ZIP SHA256: `416069b90f4cfe9dc4a1e2ea3535c9229cf521c5506eb0c3047d1be389d54128`.
- [Hướng dẫn bắt đầu](../../storage/releases/language-center-20261111-qualification/START_HERE.md), [báo cáo nghiệm thu](../../storage/releases/language-center-20261111-qualification/REPORT.md), [bằng chứng máy đọc](../../storage/releases/language-center-20261111-qualification/qualification.json).
- Release có 965 artifact theo manifest: 5 JAR, FE static, source, tests, tài liệu và migration. Giữ nguyên ZIP G20 cũ. Không đóng gói cấu hình mật, token, dump hay backup database.

## Công việc đã hoàn thành

Đồng bộ README, 23 route sidebar, danh mục 173 API chính, ma trận yêu cầu, quyền, báo cáo và ảnh thực tế. Giữ bằng chứng lịch sử có ngày; không coi kết quả cũ là kiểm thử mới. Bổ sung schema-v013-clean.sql cho database trống: thử tạo 53 bảng, 625 cột, không tài khoản, replay đủ 13 migration. Database hiện hữu phải đối chiếu migration, không chạy clean schema đè dữ liệu.

Đóng gói thêm tests/docs FE và cấu hình mẫu. Đối chiếu byte source BE/FE với snapshot, quét cấu hình mật và xác minh manifest sau chạy. Kiểm tra tài liệu đạt 3.606 checks; kiểm tra đồng bộ bổ sung đạt 162 checks; operations integrity 12/12.

## Nghiệm thu đúng gói

Chạy 5 JAR bên trong release với prod/validate và FE static, không dùng Vite hoặc JAR target thay thế. Đạt 86 ca API/SQL/browser; thêm 12 ca UI đăng nhập thật 4 role và 19 smoke trước restore. Sau restore đạt tiếp 12 UI và 19 smoke.

Backup/verify/restore dùng chính operations.cjs trong gói: 53 bảng và 4 file tham chiếu khớp fingerprint/số dòng. Database nguồn giữ nguyên dữ liệu. Các runtime riêng của lượt nghiệm thu đã được dừng. Run chính: `20261111-g20-d50c1df7`; restore cuối: `20261111-g20-d50c1df7-restore-package`.

Nền source đã đạt 335 tests BE và 96 FE ở giai đoạn 5; đây không phải tuyên bố chạy lại toàn bộ unit tests trên ZIP. Manifest SHA256: `ec148063f694328f7d5b1800a397c64d4a0dd271d7812ab04ecd85084bf615aa`.

## Giới hạn và việc còn lại

Gói chạy cần Java 21, Node 22, MySQL 8, PowerShell và cấu hình riêng theo START_HERE. ZIP không phải ảnh database chứa tài khoản sẵn cho máy khác. Bootstrap superadmin một lần trước khi chạy production theo hướng dẫn.

Cần bổ sung khảo sát thực tế, thông tin sinh viên/GVHD, mẫu khoa và dàn trang bản nộp; PPTX/video và diễn tập trên máy bảo vệ chưa được xác nhận. Nghiệm thu local không thay thế kiểm tra TLS, tải và vận hành Internet. Không tự tạo thông tin khảo sát hoặc xác nhận production.
