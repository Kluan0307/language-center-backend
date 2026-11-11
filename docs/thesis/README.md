# Bộ tài liệu hoàn thiện khóa luận

**Hiện hành11/11:** [Tổng quan cập nhật](../current-state.md), [giai đoạn5](../reviews/2026-11-11-phase-5-completion.md). Các mốc ngày10/11 bên dưới là lịch sử;96FE là tổng mới.

**Hiện trạng10/11/2026: G15–G16 đã sửa theo code, G17–G18 đạt86 kiểm tra thật; G19 hoàn thành bản thảo kỹ thuật, G20 đã nghiệm thu release local/restore/demo; bản nộp và production còn checklist.** [G13](../thesis-progress/G13.md) · [G14](../thesis-progress/G14.md) · [G15](../thesis-progress/G15.md) · [G16](../thesis-progress/G16.md) · [G17](../thesis-progress/G17.md) · [G18](../thesis-progress/G18.md) · [G19](../thesis-progress/G19.md) · [G20](../thesis-progress/G20.md). Các mục ghi ngày phía dưới giữ lịch sử triển khai. Chính sách là giả định G01-v1; dữ liệu khảo sát thực tế vẫn CẦN BỔ SUNG, không phải kết quả phỏng vấn đã có.

| Tài liệu | Mục đích |
|---|---|
| [Bản thảo khóa luận](report/index.md) | Trọn bộ bản thảo 7 chương, kịch bản demo và câu hỏi bảo vệ (G19) |
| [Yêu cầu và hiện trạng](requirements.md) | RQ01–RQ12, bằng chứng code, phần thiếu và tiêu chí nghiệm thu |
| [Quy tắc nghiệp vụ](business-rules.md) | Quyết định dùng để triển khai G02–G12, trạng thái, quyền và dữ liệu cũ |
| [Ma trận truy vết](traceability.md) | Yêu cầu → use case → màn hình/API/dữ liệu → test/bằng chứng đã chạy và phần còn thiếu |
| [Khảo sát](survey.md) | Bảng câu hỏi, mẫu ghi nhận và các thông tin thực tế còn thiếu |
| [Dàn ý báo cáo](report-outline.md) | Cấu trúc chương, nguồn bằng chứng và dữ liệu cần bổ sung |
| [Bàn giao G01](../thesis-progress/G01.md) | Phạm vi kiểm tra, baseline, kết quả và hướng dẫn G02 |
| [Kế hoạch 20 prompt](../../../../KE_HOACH_HOAN_THIEN.md) | Thứ tự giao việc, prompt và tiến độ G01–G20 |

Đọc hiện trạng và quyết định mục tiêu tách biệt. Khi code được thay đổi ở giai đoạn sau, cập nhật tài liệu và bằng chứng tương ứng; không chỉ sửa trạng thái trong kế hoạch.

## Trạng thái hiện tại G13–G19 — 10/11/2026

**G19 là bản thảo kỹ thuật đã đồng bộ; bản nộp còn mẫu khoa/khảo sát/thông tin cá nhân. G20 đã chốt release local theo qualification; cần diễn tập máy bảo vệ và hoàn thiện bản nộp.** G13 hoàn thiện tài liệu Use Case/khảo sát; G14 hoàn thiện ERD, Class Diagram và từ điển dữ liệu 53 bảng/52 thực thể; G15 hoàn thiện 10 sơ đồ Activity/Sequence cùng đặc tả thiết kế động; G16 kiểm kê 173 API chính, đồng bộ hợp đồng/README 3 cấp và ma trận truy vết RQ01–RQ12; G17 chuẩn hóa bộ fixtures demo độc lập và nghiệm thu 5 kịch bản tích hợp cốt lõi; G18 nghiệm thu an ninh, phân quyền, lỗi và đồng thời; G19 hoàn thành bản thảo báo cáo khóa luận 7 chương, kịch bản demo 15–20 phút, đề cương 18 slides và 20 câu hỏi bảo vệ. Tiếp tục **G20 — Backup/restore schema cuối và chốt bản bàn giao**.

| Bộ tài liệu thiết kế & động | Dùng để |
|---|---|
| [Use Case](use-cases.md) / [quyền](role-permissions.md) | 18 nhóm, bốn role, tiền/hậu điều kiện và ngoại lệ (G13) |
| [Quy trình](processes.md) / [khảo sát](survey.md) | As-built tách khảo sát thật chưa có; Q01–26 và mẫu biên bản (G13) |
| [Truy vết hiện tại](traceability.md) | RQ01–RQ12 → UC/implementation/giai đoạn (G13–G19) |
| [Thiết kế tĩnh](design/README.md) | 53 bảng/625 cột, 52 entity classes, FK/ID/HTTP và schema verification (G14) |
| [Thiết kế động & luồng lỗi](design/dynamic-workflows.md) | 5 luồng: Ghi danh & tạo học phí, thu tiền retry, ngoại lệ phân lớp, bàn giao giáo viên, chốt kết quả (G15) |
| [Nguồn diagram/SVG](diagrams/README.md) | 32 sơ đồ canonical (5 Use Case, 9 ERD, 8 Class, 5 Activity, 5 Sequence) (G13–G15) |
| [Bàn giao G16](../thesis-progress/G16.md) | Báo cáo hoàn thành G16, đối chiếu API/README thực tế và bàn giao G17 |
| [Bàn giao G17](../thesis-progress/G17.md) | Báo cáo hoàn thành G17, nghiệm thu 5 kịch bản thành công và đối soát học phí |
| [Bàn giao G18](../thesis-progress/G18.md) | Báo cáo hoàn thành G18, nghiệm thu phân quyền, lỗi, rào cản và concurrency |
| [Bàn giao G19](../thesis-progress/G19.md) | Báo cáo hoàn thành G19, bản thảo báo cáo khóa luận, demo và câu hỏi bảo vệ |
| [Bàn giao G20](../thesis-progress/G20.md) | Release local đã nghiệm thu; qualification/restore/demo và phần còn lại |
