# C1 — Phạm vi và chính sách nghiệp vụ

[Mục lục và tiến độ](README.md). Những mục dưới đây là quyết định thiết kế cho bản đầu; không có nghĩa source đã thực thi chatbot.

## 1. Phạm vi được hỗ trợ

Trợ lý tra cứu dữ liệu trong Language Center: khóa/chương trình, lớp, học viên, lịch học/dạy, điểm danh, kết quả học tập, học phí/phiếu thu và lead ưu tiên chăm sóc. Hướng dẫn thao tác chỉ dựa vào mục hướng dẫn đã duyệt theo role; chưa có hướng dẫn thì trả UNSUPPORTED_CAPABILITY và link trang phù hợp, không tự suy đoán quy trình.

Chỉ trả lời yêu cầu nằm trong danh sách ý định hữu hạn, có công cụ hoặc nội dung hướng dẫn đã duyệt. Không có nhánh hỏi đáp kiến thức chung sau bộ lọc. Không hỏi đáp học thuật dù có từ khóa trung tâm/tiếng Anh: viết bài IELTS, dịch, chữa bài, giải bài tập và kiến thức giảng dạy đều ngoài bản đầu. Viết code, tin tức, giải trí, tư vấn đời sống và trung tâm khác không được hỗ trợ. Không duyệt web ngoài.

Các hành động tạo/sửa/xóa, thu tiền, tạo đơn, đối soát, xuất toàn bộ dữ liệu, tải file, reset password và phân công tài khoản đều không nằm trong công cụ C1–C7, kể cả SUPER_ADMIN yêu cầu. Trợ lý có thể mở trang thao tác khi có hướng dẫn đã duyệt; người dùng thao tác bằng UI hiện hành.

Phân biệt: OUT_OF_SCOPE = ngoài chủ đề; UNSUPPORTED_CAPABILITY = đúng chủ đề nhưng chưa có công cụ; FORBIDDEN = không có quyền; NO_RESULTS = truy vấn thành công nhưng rỗng; DEPENDENCY_UNAVAILABLE = chưa lấy được dữ liệu. Không đánh đồng những trạng thái này.

## 2. Ma trận quyền bản đầu

SA = SUPER_ADMIN; A = ADMIN; S = STAFF; T = TEACHER. Dấu ✓ luôn kèm kiểm chi nhánh và quyền tài nguyên tại backend. Đây là giới hạn tối đa; trợ lý không mở rộng quyền ứng dụng.

| Nhóm công cụ | SA | A | S | T |
|---|---|---|---|---|
| Khóa học, tìm lớp | ✓ | ✓ | ✓ | Khóa trong phạm vi; lớp chính mình |
| Tìm hồ sơ học viên toàn chi nhánh | ✓ | ✓ | ✓ | Không; tìm trong roster lớp chính mình qua công cụ riêng |
| Các lớp của một học viên | ✓ | ✓ | ✓ | Chỉ lớp chính mình có học viên đó; không liệt kê lớp khác |
| Thống kê toàn bộ học viên | ✓ | ✓ | ✓ tại chi nhánh đang chọn | Không; tổng lớp mình là công cụ khác, chưa gộp vào thống kê trung tâm |
| Buổi học/lịch dạy thực tế | ✓ | ✓ | ✓ | List theo actual teacher; đọc chi tiết buổi theo quyền actual/primary hiện hành |
| Điểm danh | ✓ | ✓ | ✓ | Buổi actual/primary cho phép đọc; dạy thay không có lịch sử cả lớp |
| Điểm | Bảng điểm hiện hành hoặc công bố, ghi rõ loại | Như SA trong phạm vi | Chỉ điểm công bố theo học viên/đăng ký | Bảng điểm bài thi lớp chính mình; không có quyền chỉ nhờ dạy thay |
| Học phí, công nợ, phiếu thu | ✓ | ✓ | ✓ | Không |
| Lead ưu tiên chăm sóc | ✓ | ✓ | Bản đầu chỉ lead được giao cho mình | Không |
| Hướng dẫn sử dụng | Theo quyền | Theo quyền | Theo quyền | Theo quyền |

Source hiện cho STAFF đọc lead trong chi nhánh, còn ghi yêu cầu là người phụ trách. Bản đầu **chủ động thu hẹp** công cụ ưu tiên chăm sóc của STAFF xuống assignedTo hiện hành; không thay quyền API/UI cũ. Source: LeadService.list/get/owner. Trợ lý không cấp mới quyền tìm hồ sơ học viên cho TEACHER; dùng roster lớp có phép và chỉ trả các trường tối thiểu.

## 3. Phạm vi chi nhánh

- Mặc định mọi role dùng chi nhánh đang chọn trên web; SA cũng không tự chuyển sang toàn hệ thống chỉ vì thiếu branchId. UI phải hiển thị phạm vi thực tế.
- SA có thể yêu cầu WHOLE_SYSTEM; A có thể yêu cầu ALL_ASSIGNED nếu công cụ hỗ trợ. S/T chỉ ACTIVE_BRANCH. Chưa hỗ trợ chọn nhiều chi nhánh tùy ý cùng lúc.
- WHOLE_SYSTEM/ALL_ASSIGNED phải là lựa chọn rõ hoặc yêu cầu rõ bằng lời, được server kiểm và phản hồi bằng nhãn. Câu “của trung tâm” mặc định phạm vi đang chọn và nói rõ giới hạn; không tự suy rộng.
- Không trộn branchId với allAssigned=true. A tập phân công rỗng bị chặn ở lớp phiên như hệ thống hiện tại; adapter không được biến rỗng thành toàn hệ thống. Nếu đã có scope rỗng hợp lệ ở API thống kê, giữ kết quả rỗng, không fallback.
- Mỗi lần gọi kiểm phiên, trạng thái tài khoản, mustChangePassword, role, profileId và phân công hiện hành. Không tin role/branch trong prompt, bộ nhớ hay tool arguments do model tự đặt.
- Đổi tài khoản/chi nhánh xóa ngữ cảnh dữ liệu cũ, hủy request đang chạy; kết quả về trễ không hiện vào phiên mới. Thu hồi phân công phải có hiệu lực ở lần gọi kế tiếp. Không dùng câu trả lời cache vượt người dùng/scope.

## 4. Định nghĩa số liệu

| Khái niệm | Quy tắc đã chốt cho bản đầu |
|---|---|
| Tổng học viên | Số hồ sơ Student duy nhất trong phạm vi, gồm STUDYING/PAUSED/GRADUATED; không đếm Lead hoặc số Enrollment. Cộng map studentsByStatus của API thống kê, không cộng một trang danh sách |
| Hồ sơ trạng thái đang học | Student.status=STUDYING; chỉ mô tả trạng thái hồ sơ, không khẳng định đã có lớp |
| Học viên đang có lớp | COUNT DISTINCT studentId có Enrollment STUDYING và khoảng EnrollmentPeriod hiệu lực ở ngày hỏi; lớp không FINISHED, ngày nằm trong thời gian thực tế của lớp. Phải đối chiếu fallback legacy với roster trước khi viết C2; không tự chuyển Student.status thành số này |
| Đang học lớp nào | Các đăng ký/khoảng học có hiệu lực ở ngày hỏi, mặc định hôm nay Asia/Ho_Chi_Minh. RESERVED/TRANSFERRED/COMPLETED/DROPPED không được gọi là đang học; lớp chưa bắt đầu hiển thị riêng “đã đăng ký, chưa khai giảng” |
| Học phí dưới 5 triệu | tuitionFee < 5000000 VND, không phải <=. “Tối đa/không quá” dùng <=; “từ…đến…” bao gồm hai đầu. Dùng BigDecimal, không suy ra giảm giá hoặc học phí từng buổi |
| Khóa giao tiếp | Tìm code/name và type theo dữ liệu hiện có; không đoán từ tên IELTS/TOEIC. Mặc định OPEN; yêu cầu lịch sử mới lấy CLOSED. Khóa chung branchId=null được tính một lần trong tổng hợp |
| Thời lượng khóa | expectedMinutes nếu có; duration legacy không suy diễn là giờ/buổi. Thiếu đơn vị trả chưa xác minh |
| Lớp còn tuyển | Chưa FINISHED, còn chỉ tiêu và đủ điều kiện ngày/trạng thái hiện hành; không lấy riêng currentSize < maxSize làm bảo đảm có thể ghi danh. Nói “còn chỗ theo dữ liệu hiện tại”, link để kiểm tra ghi danh |
| Lịch học/lịch dạy | Buổi ClassSession thực tế, không dùng lịch tuần mẫu để khẳng định buổi đã tồn tại. Học bù/hủy/dạy thay có nhãn; tuần từ thứ Hai đến Chủ nhật theo UTC+7 |
| Điểm danh | Chỉ dòng recorded=true là đã ghi nhận. recorded=false phải là “chưa điểm danh” kể cả status mặc định PRESENT. Buổi tương lai/hủy không có sheet hợp lệ thì trả trạng thái buổi, không tự dựng danh sách có mặt |
| Điểm số | 0 là điểm hợp lệ; null/NOT_GRADED là chưa chấm; published/current là hai loại riêng. STAFF chỉ nhận bản công bố mới nhất còn hiệu lực. Chưa công bố/đã thu hồi không thành điểm 0 |
| Nợ quá hạn | Học phí chưa hủy, dueDate trước ngày nghiệp vụ, amount > paidAmount; dựa finance hiện hành. Không đếm enrollment chuyển lớp thành khoản nợ thứ hai; giữ billing root |
| Phiếu thu | Trả trạng thái có hiệu lực, hủy/hoàn rõ ràng; không coi đơn online PENDING hoặc trang return thành đã thu. Không tái tính doanh thu từ một trang phiếu thu |

Doanh thu/báo cáo tài chính tự do chưa nằm trong chín nhóm công cụ đầu. Nếu bổ sung, chỉ SA/A và tái sử dụng định nghĩa finance (gross/void/refund/net), cần thêm bộ nghiệm thu; không âm thầm mở cho STAFF vì có quyền xem phiếu thu.

## 5. Lead ưu tiên chăm sóc

Không dự đoán “khả năng mua cao nhất”. Khi hỏi “top khách hàng tiềm năng”, hỏi người dùng có muốn xem danh sách ưu tiên chăm sóc hay không; chỉ đổi ý định sau khi họ đồng ý hoặc đã nói rõ.

Thuật toán v1, chạy tại server trước LIMIT:

1. Chỉ NEW/CONTACTED/CONSULTING; loại CONVERTED/LOST; kiểm scope và assignee theo bảng quyền.
2. Nhóm quá giờ hẹn (nextContactAt < now), sau đó nhóm còn lại có hẹn hôm nay, sau đó chưa có lịch, rồi có hẹn sau hôm nay. Nhóm phân hoạch này không dùng trực tiếp hai filter OVERDUE/TODAY đang có vì chúng có thể chồng lấn.
3. Trong nhóm có hẹn: nextContactAt tăng dần. Trong nhóm chưa có hẹn: lastContactedAt null trước, sau đó cũ trước; createdAt cũ trước; id tăng dần để ổn định. Nhóm có hẹn trùng giờ cũng dùng id tăng dần.
4. Mặc định top 5, tối đa 20. Trả priorityReason, nextContactAt, lastContactedAt, status, assignee và link; không trả xác suất mua hoặc score giả.

Ngày giờ lấy Asia/Ho_Chi_Minh và một now cố định cho cả truy vấn. Quy tắc là thứ tự chăm sóc, không phải bằng chứng khách sắp đăng ký.

## 6. Trả lời và dữ liệu tối thiểu

Trả lời ngắn, có nguồn nội bộ, scope, thời điểm quan sát và link đã được server cho phép. Tên trùng: trả tối đa 5 lựa chọn gồm mã/tên/chi nhánh; không tự chọn, không lộ thông tin người ngoài quyền. Trong bản đầu không gửi địa chỉ, ngày sinh đầy đủ, phụ huynh, email, số điện thoại nguyên vẹn hay ghi chú tự do tới model. Dữ liệu liên hệ cần tra trực tiếp trên UI có quyền; danh sách lựa chọn thường đủ mã học viên.

Không tìm thấy trong scope không có nghĩa người đó không tồn tại toàn hệ thống. Mã sai và tài nguyên ngoài quyền dùng thông báo chung, không tiết lộ tên/chi nhánh của bản ghi bị chặn. Chỉ dùng số liệu đã truy vấn thành công; thiếu/lỗi không thành 0. Câu hỏi hỗn hợp ngoài chủ đề: chỉ xử lý phần nghiệp vụ nếu tách rõ; phần còn lại trả giới hạn, không gọi công cụ ngoài danh sách.
