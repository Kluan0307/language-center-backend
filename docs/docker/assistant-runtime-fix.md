# Trợ lý AI trong Docker — 25/11/2026

Đã khắc phục hai nguyên nhân khiến trợ lý chưa dùng được:

1. Backend `.env.local` bật trợ lý, nhưng `.env.docker.local` tại thư mục ngoài `language-center` tắt `ASSISTANT_CHAT_ENABLED` và `ASSISTANT_MCP_ENABLED`. Compose lấy cấu hình từ file Docker; chạy container khỏe không đồng nghĩa chức năng chat đang bật.
2. Gemini trả lời được với key/model đang có, nhưng một yêu cầu ngắn mất 17,6 giây. Mức chờ 10 giây khiến assistant trả `MODEL_TIMEOUT`, sau đó tạm ngắt gọi model trong 30 giây (`MODEL_COOLDOWN`).

Cấu hình Docker local hiện tại:

```dotenv
ASSISTANT_CHAT_ENABLED=true
ASSISTANT_MCP_ENABLED=true
GEMINI_MODEL=gemini-3.1-flash-lite
GEMINI_TIMEOUT_SECONDS=25
```

Giữ nguyên key và nhà cung cấp, không bật billing. Đã recreate assistant-service và restart các dịch vụ gọi nó để làm mới DNS. Giới hạn tổng mỗi lượt chat vẫn là 30 giây; Gateway chờ tối đa 40 giây, frontend 35 giây. Không cần rebuild image cho việc đổi env.

Kiểm chứng thực tế qua Gateway Docker:

- `GET /api/v1/assistant/config`: HTTP 200, `enabled=true`, `readOnly=true`.
- Câu hỏi “Toàn trung tâm có bao nhiêu học viên?” bằng superadmin, phạm vi toàn hệ thống: HTTP 200, `ANSWER`, kết quả **400 hồ sơ**, có nguồn công cụ `get_student_statistics`, mất khoảng 20,5 giây.
- Bản ghi kết quả tại `storage/assistant-docker-live.json`; không chứa key/token.

Mở http://localhost:15173, tải lại trang và bắt đầu hội thoại mới. Hội thoại trước khi khởi động lại assistant không được giữ trong bộ nhớ. Một số lượt vẫn có thể chậm hoặc hết thời gian khi Google phản hồi chậm; lần kiểm chứng này không chứng minh độ ổn định dài hạn.

Script kiểm tra nhanh, dùng tài khoản mẫu superadmin (mật khẩu đọc từ `LC_SEED_PASSWORD`, mặc định bộ demo):

```powershell
node scripts/docker/check-assistant-live.cjs
```

Script gửi một câu hỏi thống kê chỉ đọc. Kết quả cần có `ANSWER` và nguồn thống kê mới được coi là đạt. `scripts/docker/probe-gemini.cjs` chỉ gửi yêu cầu tổng hợp ngắn tới Google để chẩn đoán kết nối, không gửi thông tin học viên.
