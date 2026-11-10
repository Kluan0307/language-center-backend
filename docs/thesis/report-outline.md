# Dàn ý báo cáo và kế hoạch bằng chứng

Lập ở G01 ngày **07/11/2026**. Đây là dàn ý làm dần từ đầu, không phải báo cáo khóa luận hoàn chỉnh. G19 tổng hợp bản thảo; bản nộp cần theo mẫu khoa thực tế.

## 1. Thông tin cần bổ sung trước khi nộp

- [CẦN BỔ SUNG: tên trường/khoa, sinh viên/mã số, người hướng dẫn, niên khóa, hạn nộp và mẫu định dạng].
- [CẦN BỔ SUNG: nguồn khảo sát, đối tượng/biên bản, quy trình và biểu mẫu được phép sử dụng].
- [CẦN BỔ SUNG: yêu cầu số chương, trích dẫn, slide và thời gian bảo vệ của khoa].

Không tự điền bằng thông tin suy đoán từ đường dẫn máy hoặc lịch sử chat. Không tạo danh mục tham khảo giả; khi viết cơ sở lý thuyết phải xác minh nguồn và ghi URL/phiên bản/ngày truy cập phù hợp.

## 2. Cấu trúc chương dự kiến

| Chương | Nội dung cần viết | Nguồn đầu vào | Mốc hoàn thiện |
|---|---|---|---|
| 1. Tổng quan đề tài và khảo sát | Lý do, mục tiêu, phạm vi, vấn đề quản lý, quy trình hiện tại nếu có chứng cứ, quy trình mục tiêu, phương pháp | requirements/survey, đề cương, biên bản thật | Khởi tạo G01; bổ sung G13/G19 |
| 2. Cơ sở lý thuyết và công nghệ | Web/REST, dữ liệu quan hệ, phân quyền, transaction/version, stack thực dùng; giải thích lựa chọn trong phạm vi đồ án | POM/package, code; tài liệu chính thức đã xác minh khi trích dẫn | G19; tránh lý thuyết dài không dùng |
| 3. Phân tích yêu cầu | Actor, RQ01–RQ12, giả định, BR, Use Case, luồng lỗi và phi chức năng | requirements/business-rules/traceability; Use Case G13 | G13/G19 |
| 4. Thiết kế | Kiến trúc 4 service+gateway, shared schema/ownership, ERD/từ điển, class, UI/navigation, activity/sequence | Entity/migration/DTO/FE; G14–G15 | G14–G15/G19 |
| 5. Cài đặt và minh họa | Luồng học viên/giáo viên/khóa/lớp/đầu vào/thu phí/điểm; migration/legacy; ảnh màn hình có thật | Bàn giao G02–G12, API G16, ảnh UI G17 | G16–G17/G19 |
| 6. Kiểm thử và đánh giá | Môi trường/dữ liệu/expected-actual, quyền/concurrency/failure, đối soát tiền/kết quả, restore, hạn chế | Test report G17–G18, bằng chứng cũ có nhãn lịch sử, G20 | G18/G20; G19 viết phần đã có |
| 7. Kết luận và hướng phát triển | Mức đáp ứng RQ, giới hạn chưa kiểm chứng, khảo sát còn thiếu, e-learning/QR/portal hướng sau | Truy vết cuối và evidence thực tế | G19, cập nhật sau G20 |

Phụ lục dự kiến: đặc tả Use Case, API tiêu biểu, từ điển dữ liệu, ma trận test, hướng dẫn chạy/demo, migration/backup; không đưa secret/PII hoặc toàn bộ source dài vào báo cáo.

## 3. Danh sách sơ đồ và ảnh cần có

- G13: Use Case tổng quan và theo bốn role, luồng nghiệp vụ mục tiêu; quy trình hiện tại chỉ khi có khảo sát thật.
- G14: ERD và Class Diagram có nguồn chỉnh sửa, phân biệt tham chiếu ID với FK/JPA.
- G15: Activity/Sequence ghi danh–học phí, thu tiền/retry, ngoại lệ đầu vào, bàn giao/ngừng giáo viên, chốt/mở lại kết quả.
- G17: ảnh UI thật theo role và trường hợp dữ liệu đã lưu/đối soát. Không dùng ảnh mockup để chứng minh đã nghiệm thu.
- G18/G20: bảng expected/actual, phạm vi thử nghiệm, kết quả restore/build; không lấy test count lịch sử làm thành tích code mới.

## 4. Ghi nhận trong từng giai đoạn

Mỗi G02–G12 cập nhật BR/traceability/API ảnh hưởng và bàn giao Gxx; ghi lý do quyết định, migration và ca thử. G13–G16 tổng hợp sơ đồ/hợp đồng đúng bản cuối. G17–G18 điền kết quả thực tế. G19 viết bản thảo dựa trên các nguồn đó. G20 bổ sung bằng chứng bàn giao/restore cuối cùng.

Các giới hạn phải giữ rõ khi chưa kiểm chứng: chưa khảo sát trung tâm thật; chưa chạy usability với người dùng thật; chưa load test production; chưa kiểm tra Nginx/TLS trên máy chủ đích; dữ liệu cũ chưa xác minh không được tự thành kết quả hợp lệ.

## Nguồn chương phân tích/thiết kế sau G13/G14

- Chương phân tích: [Use Case](use-cases.md), [quyền](role-permissions.md), [quy trình](processes.md), [khảo sát](survey.md) và [truy vết](traceability.md). Không dùng sổ CẦN BỔ SUNG như biên bản đã phỏng vấn.
- Chương thiết kế: [ERD/Class nguồn và SVG](diagrams/README.md), [từ điển/đối chiếu](design/README.md). Dùng hình tổng quan trong chương, hình module và bảng cột đầy đủ ở phụ lục/khổ phù hợp.
- Caption phải nói as-built target V013 dùng chung schema, không database-per-service hoặc FK liên service tưởng tượng. Database hiện tại còn thiếu20bảng/11cột và thay đổi nullable, không mô tả đã migrate.
- Activity/Sequence chi tiết còn G15; API/test trace cuối còn G16; kết quả nghiệm thu/release/báo cáo nộp còn G17–G20.

