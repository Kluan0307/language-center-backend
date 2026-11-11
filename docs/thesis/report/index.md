# Bản thảo khóa luận — Xây dựng hệ thống quản lý trung tâm ngoại ngữ

Cập nhật **11/11/2026**, sau sửa G15–G16 và nghiệm thu thật G17–G18. Đây là **bản thảo kỹ thuật**, chưa phải bản nộp theo mẫu trường. Không xác nhận toàn bộ20giai đoạn hoàn tất: G20 đã nghiệm thu release local; bản nộp và production còn checklist.

## Tóm tắt

Đề tài xây dựng ứng dụng web quản lý trung tâm tiếng Anh với bốn vai trò SUPER_ADMIN, ADMIN, STAFF và TEACHER. Hệ thống quản lý học viên/giáo viên, chương trình/khóa/lớp, đánh giá đầu vào và ngoại lệ phân lớp, lịch/buổi/điểm danh, kết quả cuối khóa, học phí nhiều đợt và báo cáo cơ bản. Giao diện React/TypeScript dùng Ant Design và styled-components; backend Java/Spring gồm bốn service nghiệp vụ và Gateway trên schema MySQL chung.

Hai cơ chế được chú trọng là kiểm soát giao dịch tài chính cùng intent và xác nhận kết quả theo enrollment với snapshot/rào cản chung. Các quy tắc được phát triển theo giả định G01-v1, chưa phải kết quả khảo sát tại một đơn vị thực tế. Bằng chứng hiện có gồm335 kiểm tra backend,96 kiểm tra Node frontend và86 kiểm tra nghiệm thu qua Gateway/MySQL/Edge,12UI bổ sung trước và12sau phục hồi; không suy ra mức bao phủ100% hay năng lực vận hành production.

[Hiện trạng hiện hành](../../current-state.md) · [Nghiệm thu11/11](../../reviews/2026-11-11-phase-5-completion.md). Các nội dung khảo sát và bản nộp trong checklist dưới vẫn cần bổ sung.

## Mục lục

1. [Tổng quan và khảo sát](ch01-tong-quan-khao-sat.md).
2. [Cơ sở kỹ thuật và công nghệ](ch02-co-so-ly-thuyet.md).
3. [Phân tích yêu cầu](ch03-phan-tich-yeu-cau.md).
4. [Thiết kế hệ thống](ch04-thiet-ke-he-thong.md).
5. [Cài đặt và triển khai](ch05-cai-dat-trien-khai.md).
6. [Kiểm thử và đánh giá](ch06-kiem-thu-danh-gia.md).
7. [Kết luận và hướng phát triển](ch07-ket-luan-huong-phat-trien.md).

Phụ lục: [demo15–20 phút](kich-ban-demo.md), [18 slide/20 câu hỏi](slide-va-cau-hoi-bao-ve.md), [tài liệu tham khảo đã kiểm tra](tai-lieu-tham-khao.md), [truy vết RQ–UC–code–test](../traceability.md), [Use Case](../use-cases.md), [dữ liệu](../design/data-dictionary.md), [hợp đồng API](../../api-contracts.md).

## Checklist trước bản nộp

- [ ] [CẦN BỔ SUNG] Trường, khoa, ngành, lớp, họ tên/MSSV, GVHD, niên khóa và ngày nộp theo thông tin người thực hiện; không suy từ đường dẫn máy.
- [ ] [CẦN BỔ SUNG] Mẫu bìa/cam đoan/cảm ơn/nhận xét, số chương, font, lề, mục lục, trích dẫn và quy định dùng công cụ hỗ trợ của khoa.
- [ ] [CẦN BỔ SUNG] Biên bản khảo sát, đơn vị/đối tượng/thời điểm, biểu mẫu được phép sử dụng tại [survey](../survey.md); nếu không khảo sát thì trình bày đúng phương pháp mô hình hóa giả định.
- [ ] Chuyển bản thảo sang định dạng khoa yêu cầu, đánh số hình/bảng và kiểm tra trang, nội dung trích dẫn sau dàn trang.
- [ ] Chọn thời lượng thuyết trình/demo theo quy định thực; diễn tập liên tục trên máy bảo vệ, có phương án xem bằng chứng khi mạng/dịch vụ lỗi.
- [x] G20 đã nghiệm thu release/restore/demo local; xem [G20](../../thesis-progress/G20.md). Mẫu khoa, khảo sát và máy bảo vệ vẫn cần hoàn thiện.

Những nội dung này là đầu vào của bản nộp/vận hành, không yêu cầu đổi code để hợp thức hóa báo cáo. [Bàn giao G19](../../thesis-progress/G19.md).
