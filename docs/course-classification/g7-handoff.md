# G7 — Chatbot và thống kê theo phân loại khóa học

G7 cập nhật công cụ tra cứu theo chương trình và đối tượng chuẩn. Không thay đổi dữ liệu MySQL, model AI, khóa API hoặc quyền của tài khoản.

## Hành vi

- `search_courses` nhận `programCode` và `audienceCode`, kết hợp học phí, trạng thái, từ khóa, trình độ và phân trang. MCP không còn cung cấp tham số `type`; HTTP giữ trường cũ để tương thích trong thời gian chuyển đổi.
- Kết quả khóa học bổ sung `programCode`, `audiences`, `classificationComplete`. Không suy đối tượng từ tên khóa, trường `type`, tuổi học viên hoặc trình độ đầu vào.
- `search_course_admissions` nhận cùng bộ lọc chuẩn, vẫn dùng revision chính sách mới nhất và danh sách trình độ được nhận. Chính sách lệch chương trình hiện tại không được đưa ra như một điều kiện tuyển sinh hợp lệ.
- `get_center_statistics` hỗ trợ hai bộ lọc trên cho `COURSES_COUNT`. Bộ lọc không được dùng với thống kê chi nhánh, phòng hoặc lớp; backend từ chối thay vì bỏ qua điều kiện.
- Các mã danh mục ngoài quy chuẩn bị từ chối. Mã chuẩn chưa được tạo trong database cho kết quả rỗng, không bỏ lọc.
- Lọc đối tượng bằng `EXISTS`/truy vấn con, không join mở rộng dòng kết quả. Mỗi khóa được tính một lần dù gắn nhiều đối tượng.
- Câu nối tiếp “chỉ xem người lớn”, “còn IELTS” giữ điều kiện tra cứu trước đó, thay bộ lọc liên quan và quay về trang đầu.
- Câu yêu cầu nhiều chương trình hoặc nhiều nhóm đối tượng được hỏi lại trước khi gọi model/truy vấn, vì các công cụ hiện nhận một mã mỗi loại. Không cộng số khóa của các nhóm có thể giao nhau.

## Quyền và ý nghĩa số liệu

Giữ nguyên phạm vi `ACTIVE_BRANCH`, `ALL_ASSIGNED`, `WHOLE_SYSTEM`. Quyền tra danh mục khóa không đồng nghĩa với quyền xem học viên, điểm hoặc lớp ngoài phân công.

Thống kê trung tâm vẫn chỉ dành cho SUPER_ADMIN/ADMIN. STAFF/TEACHER tiếp tục có công cụ tra cứu khóa trong phạm vi được phép; không mở quyền `get_center_statistics` cho họ trong G7.

`search_courses` mặc định khóa `OPEN`. `COURSES_COUNT` mặc định mọi trạng thái, chỉ lọc `OPEN` khi được yêu cầu. Tổng kết quả tra cứu lấy `totalElements`, không lấy độ dài trang đang xem. Đối tượng học không phải bằng chứng học viên đạt điều kiện ghi danh.

## Câu hỏi để thử sau khi khởi động lại dịch vụ

| Câu hỏi | Bộ lọc/hành vi mong đợi |
|---|---|
| Khóa IELTS cho học sinh THCS dưới 5 triệu? | IELTS + SECONDARY + maxTuition=5000000, maxInclusive=false |
| Khóa giao tiếp cho người đi làm? | COMMUNICATION + ADULT |
| Khóa tiếng Anh tổng quát cho tiểu học? | GE + PRIMARY |
| Khóa tiếng Anh cho mẫu giáo? | PRESCHOOL; không tự gán chương trình nếu chưa xác định |
| Khóa IELTS cho THPT? | IELTS + HIGH_SCHOOL |
| Tổng số khóa học IELTS cho THCS đang mở? | COURSES_COUNT, IELTS + SECONDARY + OPEN; theo quyền quản lý |
| Chỉ xem người lớn | Đổi audienceCode=ADULT, giữ chương trình/học phí/phạm vi trước đó |
| Có bao nhiêu khóa IELTS cho THCS và người lớn? | Hỏi chọn một nhóm, không cộng hai số đếm |

Các câu ngôn ngữ tự nhiên vẫn đi qua model hiện tại và công cụ được cấp quyền. Kiểm thử G7 sử dụng model giả lập, không xác nhận độ chính xác của Gemini thật.

## Chạy local

Khởi động lại `academic-service` và `assistant-service` để nạp code/công cụ mới. API Gateway và web không cần thay cấu hình cho G7. Mở hội thoại mới để tránh giữ bằng chứng truy vấn theo schema cũ.

Theo bàn giao G2–G6, 16 khóa cũ vẫn cần quản lý xác nhận đối tượng. Nếu chưa gán đối tượng, tìm theo THCS/người lớn có thể trả về rỗng dù tìm theo chương trình có kết quả; đây không phải lý do để chatbot tự phân nhóm.

## Kiểm thử

Backend H2 kiểm tra khóa nhiều đối tượng đếm một lần, khóa khác chi nhánh bị loại, `type` cũ không được dùng để đoán, HTTP bind bộ lọc, mã sai trả 400 và quyền giáo viên không được nâng lên thống kê quản lý.

Assistant offline kiểm tra năm nhóm đối tượng, bốn chương trình, ranh giới từ, ánh xạ học sinh THCS, MCP truyền bộ lọc chuẩn/từ chối `type`, giữ bộ lọc khi hỏi nối tiếp, nhận câu đếm và hỏi lại câu nhiều nhóm trước khi gọi model/API.

Lệnh kiểm thử và kết quả tổng hợp nằm trong `g7-validation.json`. G8 nghiệm thu toàn luồng vẫn là giai đoạn tiếp theo; chưa bỏ cột `type` khỏi database.
