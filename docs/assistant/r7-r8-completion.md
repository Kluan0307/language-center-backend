# Bàn giao R7–R8 — trợ lý chỉ tra cứu

Cập nhật 13/11/2026. **R7 đã triển khai; R8 đã có bộ nghiệm thu tự động nhưng chưa đóng nghiệm thu Gemini thật.** Không có fine-tuning, migration hay quyền ghi dữ liệu mới.

## R7 đã thay đổi gì

- Chuẩn hóa cách hỏi có dấu/không dấu, GV/HV/NV/CN, giảng viên/giáo viên, bằng cấp/trình độ học vấn; giữ nguyên mã định danh. Không đổi “tổng công nợ” thành truy vấn đếm.
- Hiểu câu hỏi tiếp nối như “còn cô ấy?”, “người thứ hai”, “dưới 3 triệu”, “chỉ người đang hoạt động”, “tuần sau”, “khoảng ngày đó”. Giữ định danh và bộ lọc đã xác minh; hỏi lại khi thiếu ngữ cảnh.
- Câu “có bao nhiêu tất cả, không chỉ trang này?” truy vấn lại nguồn và lấy tổng kết quả phù hợp bộ lọc, không cộng số hàng đang hiển thị.
- Câu hỏi mới về người khác xóa tham chiếu cũ. Thay đổi phiên/quyền/chi nhánh không cho phép tái sử dụng hội thoại cũ. Prompt không được tự chuyển chi nhánh hay nâng quyền.
- Bổ sung nhãn tổng kết quả trên web; giữ icon bot và cấu trúc giao diện hiện tại.

## Kết quả xác minh

| Lớp kiểm tra | Kết quả | Giới hạn |
|---|---|---|
| Assistant mặc định | 342 ca: 340 đạt, 2 live opt-in bỏ qua | Có fixture tổng hợp, không phải toàn bộ prompt qua Gemini |
| Ma trận R8 | 120/120 đạt; đã chạy lại sau khi siết kiểm tra tham số và endpoint | Host + MCP SDK thật, planner và HTTP nghiệp vụ giả lập |
| Ngôn ngữ/hội thoại R7 | 49 ca đạt, gồm 36 biến thể thuộc 12 nhóm ý định | Kiểm thử chính sách và hội thoại |
| Auth/Academic/Finance/Teaching/Gateway | 85/269/127/106/10 ca đạt | Kiểm thử repository/service hiện có; không chứng minh triển khai MySQL thật |
| Web | 125 ca đạt, TypeScript/Vite build và lint đạt | Không thay thế kiểm thử người dùng trên trình duyệt |
| Gemini thật | 4 đạt, 1 bị chặn do timeout, 115 chưa chạy | Dữ liệu tổng hợp; chưa đủ để tính tỷ lệ chính xác toàn ma trận |

Gemini `gemini-3.1-flash-lite` chạy đạt SUPER_ADMIN-01 đến 04. SUPER_ADMIN-05 hết thời gian sau khoảng 10 giây (`MODEL_TIMEOUT`); bộ chạy dừng gửi thêm yêu cầu. Không đổi model, timeout, billing hay tự thử lại. Lần chạy này diễn ra trước một số chỉnh sửa cuối về refinement/assertion; không coi đó là bằng chứng live cho toàn bộ snapshot bàn giao.

Xem [tổng hợp máy đọc](acceptance-summary.json), [120 kết quả host](acceptance-host-results.json), [kết quả Gemini](acceptance-live-results.json), [oracle độc lập](acceptance-oracles.json), [audit](readonly-audit.md). Trạng thái `PASS` của host không đồng nghĩa Gemini hiểu câu hỏi đó.

## Các giới hạn còn mở

- R8 chưa đạt điều kiện nghiệm thu toàn ma trận qua Gemini thật; chưa công bố mức chính xác ≥90%.
- C7 và luồng tích hợp đầy đủ với API nghiệp vụ/MySQL thật vẫn cần nghiệm thu riêng.
- Deadline chấm điểm và thời điểm đóng lớp thực tế chưa có nguồn phù hợp. Không suy đoán từ ngày kết thúc dự kiến.
- Chưa có doanh thu dồn tích/snapshot công nợ quá khứ. Bộ lọc “chỉ người ngừng hoạt động” chưa hỗ trợ vì `activeOnly=false` có nghĩa lấy mọi trạng thái, không phải chỉ trạng thái ngừng hoạt động.

## Chạy và kiểm tra lại

Khởi động lại `assistant-service`, tải lại web và tạo hội thoại mới. R7–R8 không cần thay đổi `.env.local` hay cấu hình provider.

Từ thư mục backend gốc:

```powershell
$env:MAVEN_OPTS='-Xmx192m'
mvn "-Dmaven.repo.local=C:\Users\kluan\.m2\repository" "-DargLine=-Xmx320m" -pl assistant-service test
```

Khi provider ổn định, chạy live có chủ đích bằng key/model đã cấu hình:

```powershell
.\scripts\assistant\run-live-acceptance.ps1
node scripts/assistant/publish-acceptance.mjs
node scripts/assistant/build-readonly-roadmap.mjs
```

Script live chỉ đọc key/model cần thiết, dùng dữ liệu tổng hợp và lưu báo cáo; dừng khi gặp lỗi provider tạm thời. Không đưa dữ liệu học viên thật vào bộ chạy này. Báo cáo live mới được lưu thêm trong `storage/r8-live-runs/`.

`build-acceptance-oracles.mjs` tạo oracle từ ma trận kịch bản và các kỳ vọng được duyệt độc lập. Khi sửa oracle hoặc production code, chạy lại host test trước khi xuất báo cáo; không sửa kỳ vọng chỉ để làm test đạt. `publish-acceptance.mjs` lưu hash snapshot đóng gói và phân biệt ba lớp host/model/MySQL.
