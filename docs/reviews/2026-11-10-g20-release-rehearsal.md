# G20 và diễn tập bốn role — kết quả bàn giao local

Ngày10/11/2026. **Đạt trong phạm vi release và diễn tập local.** Gói [g20-release-20261110](../../storage/releases/g20-release-20261110/HANDOFF.md) có900artifact; SHA-256 release.json: 1fddd2e130db136b4831ec02a27e3fb414604d22b77a9f110854ee7fb6148865. Manifest/file là snapshot bất biến trước kiểm thử; hồ sơ qualification này ghi kết quả sau chạy, không sửa nội dung gói đã chạy.

## Kết quả

| Kiểm tra | Expected | Actual |
|---|---|---|
| Build/backend tests |Native package0;335tests không lỗi/bỏ qua|Đạt; backend-tests.json và package-exit.json|
| FE |82Node tests, lint/build production|Đạt; VITE_API_BASE_URL=/api/v1; chunk size warning ghi nhận, không đổi ngưỡng để che|
| Công cụ |Ops9; integrity3|Đạt, log kèm|
| Nghiệp vụ trên artifact |86ca thật|86/86; g17/g18/browser/database báo cáo riêng|
| UI đăng nhập4role |12ca trước và sau restore|12/12 mỗi lượt, không gọi24ca độc lập|
| Smoke5service |19ca trước/sau restore|19/19 mỗi lượt|
| Phục hồi cuối |Dừng writer riêng, backup SQL+file, restore schema mới|Fingerprint/số dòng khớp sau13migration;8file references;9file trong backup gồm SQL|
| Bổ sung kịch bản |4bước UI chỉ đọc|FAIL nhưngCOMPLETED, lịchTeacher, snapshot60%, báo cáo thu/nợ3M đều đạt|
| Toàn vẹn sau chạy |Manifest không thay đổi|900/900hash hợp lệ; source snapshot dirtyWorkingTree=true được ghi rõ|

## Môi trường và bằng chứng

Run chính20261110-g20-3d704261, schemalc_rehearsal_1794287045077_21e166; restore20261110-g20-3d704261-restore, schemalc_rehearsal_1794287228593_cb5afa. JAR chạy qua start-release.ps1 trong gói, prod/validate; frontend tĩnh trong gói, cùng origin25173→Gateway28080. Không dùng Vite dev để thay nghiệm thu release. Nguồn language_center_db không đổi theo fingerprint/số dòng trước-sau; backup nhất quán là của schema thử đã dừng JVM, không tự gọi đó là điểm phục hồi production của người dùng.

Expected/actual ở g17.json, g18.json, g17-crud.json, browser.json, database.json và các báo cáo demo. Ảnh demo-*.png là Edge thật trên bản phục hồi; actor đăng nhập qua UI, CAPTCHA local được harness quan sát ký tự canvas để điền đúng, không phải đánh giá chống bot. Bốn bước bổ sung dùng actor đăng nhập API để đọc UI; mô tả riêng trong presentation-readthrough.json.

## Lỗi đã phát hiện và xử lý

Gói thử g20-verified-20261110 không đủ điều kiện vì log sinh trong thư mục release làm strict verifier từ chối file ngoài manifest. Đã sửa launcher buộc log tuyệt đối bên ngoài; bản cuối được chạy lại từ đầu và giữ toàn bộ gói/log lỗi. Bổ sung diễn tập ban đầu dùng nhầm /schedule cho Teacher nên guard từ chối đúng; đã sửa kịch bản sang /my-schedule, giữ report lỗi và chạy lại4bước đạt, không nới quyền ứng dụng. Wrapper PowerShell coi cảnh báo Mockito stderr là NativeCommandError;335kết quả Surefire đạt, package native exit0 được ghi riêng, không giả mã thoát của wrapper.

## Giới hạn và cách dùng

Đọc HANDOFF.md trong gói, source/README và bản thảo trong source/language-center. Gói public không có .env.local, private-session hoặc SQL dump. Metadata backup đính kèm chỉ là bằng chứng, không đủ để restore; SQL/documents đầy đủ và tài khoản thử giữ riêng local ở thư mục lượt nghiệm thu. Không chia sẻ file private.

Diễn tập này là thao tác UI tự động trên máy hiện tại, không phải một bài nói15–20phút hoặc kiểm tra máy bảo vệ của người dùng. Cần diễn tập người trình bày/máy đích, mẫu khoa/thông tin cá nhân/khảo sát và dàn trang. Nginx/TLS/Internet/load, RPO/RTO chưa nghiệm thu. Không kết luận đồ án/bản nộp/production hoàn tất100%.


Hồ sơ local: [G20](../thesis-progress/G20.md), [checklist diễn tập](../../deployment/defense-rehearsal-checklist.md), [qualification](../../storage/releases/g20-release-20261110-qualification/qualification.json).
