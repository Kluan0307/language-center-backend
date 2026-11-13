# Assistant service

C4: backend hội thoại Gemini, MCP nội bộ chỉ đọc, Java 21 / Spring Boot 4.1.1 / MCP Java SDK 2.0.0 với Jackson 3.

C5 đã bổ sung policy, quota theo account, giới hạn body thực, kiểm lại quyền trước response và audit. [Cấu hình hạn mức, log và nghiệm thu C5](../docs/assistant/c5-completion.md). Hiện 123 test đạt, 1 Gemini live opt-in skip; tại mốc C5 chưa có web drawer.

C6 đã có [drawer chat web cho bốn role](../docs/assistant/c6-completion.md); kèm sửa whitelist link khóa học giữ branchId. Restart gateway để trình duyệt nhận Retry-After. Không tự bật assistant; Gemini thật còn cần nghiệm thu C7.

**Sửa model chậm:** đã chuyển theo lựa chọn người dùng sang gemini-3.1-flash-lite, timeout 10 giây, cooldown 30 giây sau lỗi provider. Bốn luồng Gemini thật + MCP với Auth/C2 tổng hợp đã đạt; [chi tiết và cách restart](../docs/assistant/troubleshooting.md). Chưa nghiệm thu toàn bộ C7 trên dữ liệu nghiệp vụ thật. Bộ mặc định hiện 124 test đạt, 1 live opt-in skip; live smoke chạy riêng.

[Bàn giao, kiến trúc, cấu hình và giới hạn C3](../docs/assistant/c3-completion.md).

[API, cấu hình, kiểm thử và giới hạn C4](../docs/assistant/c4-completion.md). Source/test C4 đã hoàn tất; Gemini thật chưa nghiệm thu do timeout/503.

```powershell
cd assistant-service
mvn spring-boot:run
```

Mặc định `127.0.0.1:8086`, MCP/chat tắt. GET `/api/v1/assistant/config`, POST `/api/v1/assistant/chat`, DELETE `/api/v1/assistant/conversations/{id}` yêu cầu bearer SA/A/S/T và xác minh Auth. Config trả enabled=false khi thiếu cờ/key; chat trả 503 khi tắt. Không có URL MCP công khai.

Để kiểm thử tích hợp, đặt `ASSISTANT_MCP_ENABLED=true`, `ASSISTANT_CHAT_ENABLED=true`, `GEMINI_API_KEY`, `GEMINI_MODEL` trong `.env.local` backend gốc. API key chỉ dùng ở backend. Không cần DB_PASSWORD/JWT_SECRET cho assistant; Auth và các service C2 phải chạy khi tra cứu thật. Restart gateway để nạp route assistant. Chưa bật dữ liệu vận hành thật trước C5/C7.

```powershell
mvn verify
```

Test mặc định dùng server Auth/C2 giả lập trên loopback riêng và SDK MCP thật: 123 ca đạt, 1 smoke Gemini thật bỏ qua. Không gọi MySQL thật. Smoke opt-in từ backend gốc: `.\scripts\assistant\gemini-live-smoke.ps1`, chỉ dùng dữ liệu tổng hợp và có thể tính quota API. Lần thử Gemini thật tại C4 còn timeout/503.
