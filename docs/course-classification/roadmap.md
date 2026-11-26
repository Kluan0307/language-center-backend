# Lộ trình phân loại khóa học thống nhất

| Giai đoạn | Phạm vi | Trạng thái |
|---|---|---|
| G1 | Quy chuẩn đối tượng, chương trình, quan hệ và quyền | Hoàn thành — g1-rules.md |
| G2 | Schema bổ sung, backup, rehearsal MySQL, ánh xạ chắc chắn và báo cáo | Hoàn thành local — g2-handoff.md và g2-report.json |
| G3 | API chuẩn, bảo vệ chương trình/policy, ngừng nhận type tự do | Đã triển khai — g3-g4-handoff.md |
| G4 | Web quản lý chọn chương trình/đối tượng; xác nhận phân loại khóa cũ | Code hoàn thành; 16 khóa cũ sau đó đã được thay ở D4 bằng bộ mới có phân loại đầy đủ |
| G5 | Website/CMS và các lối tìm theo đối tượng/chương trình | Hoàn thành code; 38 kiểm tra Chrome đạt |
| G6 | Mobile tương thích bản APK cũ, hiển thị dữ liệu đã chuẩn hóa | API/mã app hoàn thành; chưa build APK hoặc thử thiết bị thật |
| G7 | Chatbot và thống kê không đếm trùng khi join nhiều đối tượng | Hoàn thành code; 397 kiểm thử H2/offline đạt — g7-handoff.md; chưa thử model thật |
| G8 | Nghiệm thu toàn luồng, chỉ bỏ type khi hết phụ thuộc | 362 test, 49 Chrome đạt; giữ type vì còn phụ thuộc. Thiếu đối tượng của bộ cũ đã được xử lý bằng thay dữ liệu D4; thử APK/model thật còn lại — g8-handoff.md |

G1–G8 chuẩn hóa phân loại. Sau đó D1–D8 đã làm mới dữ liệu, xuất bản và cập nhật website theo nhóm; 20 khóa mới có đối tượng được thiết kế rõ trong ma trận seed. Xem [bàn giao D5–D8](d5-d8-handoff.md); các bước thử APK/model/thanh toán thật được ghi riêng, không suy từ tên khóa cũ.
