# C5 — Kiểm soát trợ lý trước khi triển khai UI

Cập nhật 13/11/2026. Đã triển khai các kiểm soát C5 trong assistant-service; không thay database, quyền nghiệp vụ C2, provider/model, `.env.local`, web hoặc mobile. Chat/MCP vẫn **tắt mặc định**. C6 là giao diện chat web; C7 nghiệm thu model và hệ thống thật.

## Kiểm soát đã có

| Ranh giới | Hành vi hiện tại |
|---|---|
| Chủ đề | Chỉ có công cụ nghiệp vụ đã khai báo; không có nhánh trả lời kiến thức chung, SQL tự do, đọc file, truy cập URL hoặc ghi dữ liệu |
| Câu hỏi hỗn hợp | Ví dụ “Tìm khóa giao tiếp dưới 5 triệu rồi dự báo thời tiết” chỉ gửi phần khóa học cho model, kèm cảnh báo đã bỏ phần ngoài phạm vi |
| Chỉ dẫn vượt quyền | Chặn các mẫu override/giả role, đọc key/env, URL tùy ý, SQL và credential phổ biến trước model. Chuẩn hóa Unicode, dấu tiếng Việt và ký tự ẩn trước kiểm tra |
| Mutation | Yêu cầu xóa/tạo/cập nhật/thu học phí/xác nhận thanh toán trả UNSUPPORTED_ACTION; câu hỏi hướng dẫn chỉ dùng catalog help cố định |
| Quyền và scope | Không đổi role/scope theo lời chat. Nhắc chi nhánh không được giao bị 403; chi nhánh được giao nhưng khác scope hiện tại phải chọn scope rõ ràng và mở hội thoại mới |
| Teacher/staff | Teacher không mở rộng tìm kiếm ra toàn trung tâm, không suy quyền bảng điểm cả lớp từ buổi dạy thay; giữ chặn tài chính/lead/thống kê. Staff không xem điểm nháp |
| Thu hồi trong lúc xử lý | Kiểm lại Auth sau downstream read và trước trả response/replay; không trả dữ liệu đã lấy nếu phiên hoặc phân công đã thay đổi |
| Dữ liệu công cụ | Giữ projection tối thiểu; tên chứa mẫu injection/secret bị thay bằng thông báo trung tính trước khi đưa vào card, choices hoặc vòng gọi model tiếp theo. Loại HTML/điều khiển trong chuỗi hiển thị |
| Điều hướng | Chỉ route relative đã whitelist theo role; từng route có danh sách query key và kiểu giá trị. Chặn redirect, duplicate key, path encoded, scheme/host/fragment và ID sai định dạng |
| Tham số công cụ | Schema kín, kiểm ID đã được xác định; chặn instruction/URL trong tham số chuỗi. Tiền min > max, khoảng lịch đảo hoặc dài hơn 31 ngày bị chặn trước API nghiệp vụ |
| Đúng nghĩa số liệu | Thống kê hiện tại không thay số lịch sử; lịch sử chưa có snapshot trả UNSUPPORTED_CAPABILITY. “Đang có lớp” dùng ACTIVE_IN_CLASS, không dùng trạng thái hồ sơ. Khoảng tiền đảo trong câu hỏi được yêu cầu xác nhận lại |
| HTTP input | Tối đa 16 KiB byte body thực đọc, gồm chunked/không có hoặc sai Content-Length; không giải nén body gửi vào. JSON trùng trường/trailing object, trường lạ và ID phân số bị từ chối |
| HTTP output | `Cache-Control: no-store`, `Pragma: no-cache`, request trace UUID. HTTP 429 có `Retry-After: 60`; lỗi không kèm nội dung provider hoặc stacktrace |

Không dựa vào keyword làm lớp bảo vệ duy nhất: dù model hiểu sai hoặc trả function ngoài danh sách, host vẫn kiểm family/tool/schema/ID; MCP và API đích tiếp tục kiểm quyền. Message/card dùng template và dữ liệu nguồn; không xuất văn bản tự do của model. Các mẫu ngôn ngữ là lớp nhận biết có giới hạn, chưa chứng minh bao phủ mọi biến thể tiếng Việt/Anh hoặc mọi prompt tấn công.

Khi không thể tách an toàn phần nghiệp vụ khỏi câu ngoài phạm vi, trợ lý có thể từ chối toàn câu hoặc yêu cầu hỏi lại. Không bảo đảm tự hiểu mọi cách diễn đạt. Các trường tên hợp lệ nhưng trùng mẫu chặn cũng có thể bị ẩn; cần theo dõi false positive trong C7.

## Hạn mức sử dụng

| Hạn mức | Mặc định | Cấu hình backend |
|---|---|---|
| Request đã xác thực, gồm config/chat/delete | 30/phút/account | ASSISTANT_REQUESTS_PER_MINUTE |
| Lượt gọi model, tính cả lần provider lỗi | 12/phút/account | ASSISTANT_MODEL_CALLS_PER_MINUTE |
| Lượt model toàn instance | 120/phút | ASSISTANT_GLOBAL_MODEL_CALLS_PER_MINUTE |
| Chat đang xử lý/account | 2 | ASSISTANT_CONCURRENT_PER_ACCOUNT |
| Chat đồng thời toàn instance | 8 | Giới hạn host trong code |

Các biến đã thêm vào `.env.example`, không cần điền lại key. Khoảng cấu hình hợp lệ lần lượt: 1–600, 1–120, 1–2.000, 1–8; cấu hình ngoài khoảng khiến service từ chối khởi tạo. Tắt tính năng bằng ASSISTANT_CHAT_ENABLED=false và ASSISTANT_MCP_ENABLED=false, rồi restart assistant.

Rate limit dùng cửa sổ cố định 60 giây theo **accountId đã xác minh**, không theo token/IP client khai báo. Đổi token không reset quota; replay không gọi model thêm nhưng vẫn tính request HTTP. Khi model đang chạy, worker giữ suất đồng thời đến lúc thực sự kết thúc, kể cả caller đã timeout. Không tự retry, đổi provider/model hoặc bật thanh toán API.

Map rate limit giới hạn 2.000 tài khoản đang có bucket; hết cửa sổ và không còn worker thì dọn. Các giới hạn C4 còn giữ: 200 hội thoại, TTL 15 phút, 10 lượt lịch sử, 8 snapshot replay, 3 lượt model/4 tool mỗi request, deadline 30 giây và response model tối đa 512 KiB.

Đây là hạn mức **mỗi instance, trong RAM**: restart reset rate window; chưa phải ngân sách tiền/ngày hoặc bộ chống DDoS. Khi chạy nhiều instance phải dùng rate store chung và cấu hình giới hạn mạng/gateway tương ứng. Chưa benchmark lưu lượng production hoặc slow-upload; giới hạn byte không thay timeout/proxy ở deployment.

## Nhật ký và vận hành

- Audit ghi request UUID, role, scope mode, outcome, số lần model/tool và duration; HTTP ghi trace/status/duration, từ chối ghi code an toàn. Không ghi prompt, kết quả, tên/điện thoại/email, tool arguments, bearer hoặc API key.
- Chưa ghi định danh account hoặc thống kê token từ provider; chưa có dashboard/định mức chi phí. Không suy số token từ số lượt gọi model.
- Mặc định log file `../storage/assistant-service.log` theo working directory assistant-service; có thể đổi bằng `ASSISTANT_LOG_FILE` trong môi trường vận hành. Cấu hình rotation tối đa 10 MB/file, giữ archive 7 ngày, tổng archive cap 70 MB; file hiện tại chưa tính vào archive cap.
- Chỉ tài khoản vận hành nên đọc log. Cấu hình rotation không thay ACL filesystem. Không bật SDK/HTTP body debug trên dữ liệu thật; request/response DTO đã có toString ẩn nội dung.
- 401: đăng nhập lại; 403: kiểm phân công; 409: mở hội thoại mới khi đổi scope/phiên; 429: chờ Retry-After; 503/504: kiểm Auth/C2/provider bằng mã an toàn. Không sửa key/quota chỉ dựa vào timeout.

## Kiểm thử và bằng chứng

`mvn verify` assistant-service: **124 ca, 123 đạt, 1 Gemini live opt-in skip**, gồm 62 ca mới C5 và hồi quy C3/C4. Không gọi Gemini hoặc MySQL thật trong lượt C5. Log: `storage/c5-verify.log`; báo cáo JUnit: `assistant-service/target/surefire-reports`.

| Nhóm | Bằng chứng tự động |
|---|---|
| Chủ đề/Unicode/mutation/hỗn hợp/lịch sử/JSON | C5PolicyTest; input đối kháng không được chuyển thành query chung |
| Quyền/scope/thu hồi phiên/DB injection/metric/link | C5SecurityTest với SDK MCP thật, Auth/C2 tổng hợp; kiểm cả response và endpoint thực sự đã gọi |
| Quota/window/account/concurrency/map cap | UsageLimiterTest; có kiểm tranh chấp song song |
| Đổi token cùng account, 429 headers, account riêng | C5FilterTest; Auth HTTP fixture thật |
| Chunked thực, ID phân số, body encoding | C5HttpTest dùng Tomcat cổng ngẫu nhiên và HTTP client thật, model mock; body bị chặn trước model/API nghiệp vụ |
| Audit, timeout và worker còn chạy | C5SecurityTest: bắt audit log kiểm không có marker prompt/PII/credential; worker không hợp tác vẫn giữ lease đến khi dừng |

Smoke JAR dùng `.\scripts\assistant\smoke-local.ps1`: health UP, POST /mcp không token bị chặn 401; script kiểm cổng thử đã đóng sau cleanup. Chỉ dừng JVM do lượt smoke tạo, không dừng dịch vụ người dùng. Kết quả local `storage/c3-smoke/result.json` giữ đường dẫn cũ để tương thích, có processId và cleanup.

Không tuyên bố 50 ca [nghiệm thu C1](acceptance-cases.json) đã chạy đầy đủ hoặc Gemini thật đã PASS. Bộ C1 vẫn là input dự kiến cho nghiệm thu tích hợp C7; test C5 là kiểm soát độc lập trên fixture. Lần thử model thật gần nhất ở C4 còn timeout/HTTP 503; C5 không thay đổi kết luận đó.

## Chạy và bước tiếp theo

Build/restart assistant-service để nhận kiểm soát mới. Không cần migration hoặc restart các service nghiệp vụ cho thay đổi C5. [Cách cấu hình/chạy API C4](c4-completion.md), [MCP C3](c3-completion.md).

Tiếp theo **C6**: drawer chat Ant Design + styled-components; chỉ hiện cho 4 role web, render dữ liệu dạng text/card, xử lý choices/429/409, khóa scope và hủy kết quả về trễ khi đổi account/branch. Giữ các cờ tắt cho đến khi chuẩn bị chạy thử tích hợp. **C7** xác minh Gemini thật trên dữ liệu tổng hợp, bộ nghiệm thu nghiệp vụ, latency/quota, false positive và hồi quy hệ thống; quyết định chính sách dữ liệu trước khi gửi dữ liệu vận hành thật.
