# OPT1–OPT3 — Rà soát và tối ưu (25/11/2026)

## Kết quả đã thực hiện

- 62 kiểm tra nhất quán dữ liệu đạt: khóa ngoại, phân loại khóa, chi nhánh, ghi danh, điểm danh, điểm và tài chính. Bằng chứng: `opt1-opt2-audit.json`.
- 23 kiểm tra HTTP đăng nhập/phân quyền, chi nhánh, lịch dạy, ghi danh, học phí/phiếu thu và website đạt: `runtime-functional-validation.json`.
- Thêm index `students(branch_id,id)` và `tuition(student_id,due_date,id)`, đồng bộ entity và migration V028. Hash toàn bộ bản ghi không đổi.
- EXPLAIN danh sách học viên theo chi nhánh: chi phí ước tính 41,50 → 10,32. Truy vấn học phí theo học viên/ngày đến hạn: dùng index mới, chi phí 0,35, không filesort. Đây không phải thời gian ms hoặc load test.
- 60 kiểm thử Finance đạt: OnlinePaymentTest, PaymentFlowContractTest, PaymentProviderConfigTest, StudentFinanceTest.
- 62 kiểm thử Academic đạt cho lead, ghi danh, lịch học, điểm danh, giới hạn số truy vấn, tiếp nhận tư vấn và quyền chi nhánh. 8 kiểm thử GeminiGatewayTest đạt cho adapter model. Tổng 130 kiểm thử backend trong đợt này, không gọi provider thật.
- Đã nhập các key còn thiếu vào env Docker, giữ flag provider tắt; không in key. Báo cáo cấu hình: `opt3-integrations.json`.

## Dùng lại công cụ

```powershell
node scripts/docker/audit-optimization.cjs
node scripts/docker/apply-lookup-indexes.cjs
node scripts/docker/configure-integrations.cjs --import-local
```

Script index chỉ nhắm MySQL Docker, bỏ qua index đã tồn tại và đối chiếu hash mọi bảng. Database Windows chưa áp dụng DDL. Migration một lần và rollback: `docs/migrations/V028__student_lookup_indexes.sql`. Khi tạo Docker DB từ snapshot schema cũ, chạy script index sau khi MySQL sẵn sàng.

## VNPAY trong Docker

Giữ terminal ngrok chạy:

```powershell
ngrok http 18080
```

Trong terminal backend khác:

```powershell
.\scripts\docker\set-vnpay-return-url.ps1
```

Wrapper mới chọn cổng 18080, ghi vào `language-center/.env.docker.local` sau khi xác minh endpoint HTTPS. Script gốc vẫn mặc định cổng 8080 cho Maven. Đăng ký IPN URL mà script in ra với VNPAY, đặt `VNPAY_ENABLED=true` trong env Docker rồi chạy lại stack. Không trỏ đơn Docker về callback của database Maven.

## Gemini

Key và model đã có trong env Docker. Khi muốn dùng, đặt `ASSISTANT_CHAT_ENABLED=true` và `ASSISTANT_MCP_ENABLED=true`, chạy lại `lc.ps1 -Action up`. Kiểm tra hội thoại bằng tài khoản và chi nhánh phù hợp. Đợt này chưa bật flag hoặc gọi model thật.

## Push

Còn thiếu Expo Project ID, cấu hình EAS/Firebase và thiết bị/build hợp lệ. Điền `EXPO_PROJECT_ID` vào env Docker; access token nếu dùng enhanced push security. Bật `NOTIFICATIONS_PUSH_ENABLED=true`, cấu hình job theo nhu cầu rồi chạy lại stack. Cần thiết bị đăng ký token thành công mới kiểm chứng được gửi push thật.

## Công việc còn mở

Theo lựa chọn của người dùng, hoàn thiện code/hướng dẫn trước khi có thông tin tích hợp. Provider vẫn tắt; chưa thực hiện giao dịch sandbox, model hoặc push thật trong Docker.

OPT1 còn nghiệm thu lại toàn bộ thao tác qua giao diện web/mobile trên bản sao. Luồng ghi lead, ghi danh, lịch học và điểm danh được kiểm tra thêm bằng các workflow test cô lập. OPT2 còn đo thời gian API/tải đồng thời trước khi chọn cache. EXPLAIN không thay thế đo tải. OPT3 bàn giao code và hướng dẫn theo lựa chọn người dùng; nghiệm thu provider thật chờ cấu hình bên ngoài.
