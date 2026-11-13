# Khắc phục trợ lý chưa mở được

## Gemini chờ lâu rồi báo chưa sẵn sàng — 13/11/2026

Log thực tế cho thấy request dừng tại MODEL_TIMEOUT sau khoảng 20 giây, trước khi gọi công cụ nghiệp vụ. Phép thử độc lập trên cùng model/key nhận HTTP 503 từ cả generateContent và Interactions, với thông báo Google đang quá tải. Đây không phải bằng chứng API key sai hoặc database chậm.

Theo lựa chọn người dùng, đã đổi `.env.local` và mẫu mặc định sang:

```dotenv
GEMINI_MODEL=gemini-3.1-flash-lite
GEMINI_TIMEOUT_SECONDS=10
```

Không đổi key, provider hoặc billing. API model cũ vẫn được Google hỗ trợ; không chuyển giao thức chỉ để che lỗi quá tải. [Tài liệu model Flash-Lite](https://ai.google.dev/gemini-api/docs/models/gemini-3.1-flash-lite) xác nhận khả năng function calling.

Kiểm thử **Gemini thật + MCP thật**, Auth/C2 tổng hợp:

| Luồng | Kết quả | Thời gian quan sát |
|---|---|---|
| Khóa giao tiếp dưới 5 triệu | Đạt; maxTuition=5000000, maxInclusive=false | 2,94 giây |
| Lịch dạy tuần của teacher | Đạt; thứ Hai–Chủ nhật UTC+7 | 1,45 giây |
| Tổng hồ sơ học viên | Đạt; giữ số 0 từ nguồn | 1,28 giây |
| Học viên trùng tên → chọn người thứ hai → xem lớp | Đạt; đúng studentId=12 | 4,76 giây cho cả chuỗi |

Bằng chứng: `storage/gemini-flash-lite-verification.json`; chạy lại bằng `scripts/assistant/gemini-live-smoke.ps1` khi cần, có tiêu thụ quota API. Đây là số đo một lượt kiểm tra, không phải cam kết độ trễ mọi lúc. Chưa nghiệm thu toàn bộ ma trận C7 với API nghiệp vụ/MySQL thật.

Đã giảm timeout mỗi lượt model mặc định 20 xuống 10 giây. Sau lỗi provider 503/5xx hoặc timeout/mạng, backend tạm ngừng gọi model 30 giây; yêu cầu tiếp nhận MODEL_COOLDOWN ngay thay vì tiếp tục chờ. UI có thông báo AI quá tải/timeout riêng, đọc Retry-After để khóa gửi trong thời gian chờ, không tự retry hoặc đổi provider/model.

Hồi quy: assistant 124 test đạt, 1 live opt-in skip trong bộ mặc định; frontend 121 test và build đạt. Live smoke nói trên chạy riêng và đạt. Cần restart assistant-service rồi Ctrl+F5 web để nạp source/env mới; log khởi động sẽ có `assistant_provider model=gemini-3.1-flash-lite timeoutSeconds=10 configured=true` (không chứa key).

## UI báo hết hạn hội thoại ngay khi mở — sửa sau C6, 13/11/2026

Đã sửa frontend phân biệt lỗi GET `/assistant/config` với lỗi POST `/assistant/chat`. Trước đó mọi HTTP 404 đều bị diễn giải là hội thoại hết hạn, kể cả lúc chưa tạo hội thoại hoặc route chưa truy cập được.

- Config 404/409: thông báo chưa kết nối được dịch vụ, cho kiểm tra lại; không báo hết hạn hội thoại.
- Chat chỉ reset khi backend trả đúng `CONVERSATION_NOT_FOUND`, `CONTEXT_CHANGED` hoặc `SESSION_CHANGED`.
- `NOT_FOUND` là thiếu dữ liệu trong phạm vi, không phải mất hội thoại. `REQUEST_CONFLICT` yêu cầu gửi câu hỏi mới, không thử lại vô hạn cùng requestId.

Kiểm tra máy tại lượt sửa: assistant health trả 200; config không bearer trả 401 đúng yêu cầu, qua gateway và trực tiếp assistant. Log service có các request cấu hình 200 của phiên đã đăng nhập. Không gọi Gemini hoặc truy vấn dữ liệu nghiệp vụ để chẩn đoán.

`.env.local` backend khi kiểm tra chưa có hai cờ bật tính năng. Đã bổ sung theo yêu cầu khắc phục UI:

```dotenv
ASSISTANT_MCP_ENABLED=true
ASSISTANT_CHAT_ENABLED=true
```

Key Gemini đã có; không thay key, model, DB hoặc cấu hình khác. Cờ trong `.env.example` vẫn false để giữ mặc định an toàn cho bản cài mới. Bật cờ không phải bằng chứng model thật đã hoạt động; C7 vẫn cần nghiệm thu.

**Cần làm sau sửa:**

1. Trong terminal đang chạy assistant-service, Ctrl+C rồi chạy lại `mvn spring-boot:run` từ thư mục assistant-service. Spring đọc `.env.local` khi khởi động; sửa file không tự cập nhật bean đang chạy.
2. Tải lại web bằng Ctrl+F5, mở Trợ lý và bấm Kiểm tra lại kết nối nếu còn màn hình cũ.
3. Nếu dùng web production/preview, dùng bản `dist` mới đã build. Gateway hiện đã có route assistant, không cần đổi đường dẫn FE hoặc mở cổng MCP.

Hồi quy frontend sau sửa: 120 test đạt, build/lint phần assistant đạt. Browser fixture: 21 luồng đạt, thêm config 404 và phục hồi bằng nút thử lại. Không dùng kết quả fixture để tuyên bố Gemini thật đã PASS.
