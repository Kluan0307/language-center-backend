# Giai đoạn5 — nghiệm thu tích hợp và phục hồi

Đã chạy các bước trên source ngày11/11/2026; xem [kết quả](../../docs/reviews/2026-11-11-phase-5-completion.md). Bộ orchestration `run.ps1` được bổ sung từ những lệnh đã chạy riêng; đã kiểm cú pháp, chưa tuyên bố đã chạy lại trọn bộ orchestration mới trong một lần sau khi viết file.

Từ backend root, dependency frontend đã cài, JDK/Maven/MySQL/Edge có sẵn:

```powershell
.\scripts\phase5\run.ps1 -Documents storage/improvement6-documents
```

Phải cung cấp đủ các thư mục tài liệu vật lý nếu database nguồn có references ở nhiều nơi. Script tạo bản sao riêng, chạy unit/build,86 ca baseline, UI mới và smoke, backup/restore, rồi kiểm tra lại UI/smoke trên bản phục hồi. Không đổi .env.local hoặc xóa dữ liệu/schema cũ. Mật khẩu Super Admin của bản sao theo harness acceptance (mặc định123456 hoặc ACCEPTANCE_BOOTSTRAP_USERNAME/PASSWORD khi được cấu hình); không in token/mật khẩu trong báo cáo.

**Có bước ghi schema nguồn có kiểm soát:** `apply-source-v011.cjs` chỉ chạy sau baseline và UI thật đạt. Nếu duration đã nullable thì không ghi DDL; nếu còn NOT NULL, nó tạo/verify backup nguồn mới, kiểm dữ liệu ổn định rồi áp dụng V011 đã thử trên bản sao. Có lock timeout và đối soát fingerprint/số dòng sau migration. Bước này không thêm/sửa/xóa dữ liệu nghiệp vụ; backup online được ghi nhãn unqualified thay vì giả là backup production đã dừng writer.

Runtime thử dùng28080–28084/25173/29388; chỉ dừng JVM/browser/Vite theo đường dẫn runtime riêng. `runtime.ps1` dùng append log có retry để tránh lỗi Add-Content khi ghi lifecycle trên Windows.

Các helper:

- `live-ui.mjs`:12 ca UI thật, gồm đăng nhập4role; dùng canvas để đọc CAPTCHA cho automation, không chứng nhận bảo mật CAPTCHA.
- `recover.cjs`: chỉ chấp nhận schema rehearsal; yêu cầu JVM writer riêng đã dừng; backup/verify/restore và so fingerprint toàn dữ liệu sau replay migration.
- `summarize.cjs`: yêu cầu335tests Java,96FE,12ops,86baseline,12UI trước và12UI sau restore,19smoke mỗi lượt, recovery/source migration proof đạt. Khi bổ sung test sau này, cập nhật mốc nghiệm thu thay vì bỏ assertion.

Không dùng báo cáo này để tuyên bố production/TLS/Internet/load hoặc bản ZIP cuối đã nghiệm thu. Không publish private-session/env/dump/backup.
