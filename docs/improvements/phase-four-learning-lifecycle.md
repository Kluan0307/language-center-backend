# Cải thiện 4 — Vòng đời học viên và danh sách dự thi

Triển khai ngày 05/11/2026 trên Academic, Teaching và frontend Ant Design/styled-components, theo cấu trúc trang/service/hook hiện có. Đây là giai đoạn cải thiện sau MVP.

## Phạm vi và cách dùng

- **Học viên → Xem → Lịch sử vòng đời**: xem thay đổi hồ sơ và các đăng ký, lý do, người thực hiện, thời gian và đăng ký chuyển đến/đi.
- **Đăng ký & xếp lớp → Trạng thái**: bảo lưu phải nhập lý do và ngày dự kiến học lại sau hôm nay. Bảo lưu giải phóng sĩ số; không hoàn/hủy học phí.
- Với đăng ký bảo lưu, chọn **Học lại** để trở về lớp cũ, **Cập nhật ngày dự kiến** để điều chỉnh kế hoạch, hoặc **Chuyển lớp** để học tại lớp khác cùng khóa/chi nhánh. Lớp cũ đã kết thúc thì dùng chuyển lớp. Học lại/chuyển lớp vẫn kiểm tra hồ sơ đang học, sức chứa, trùng lịch thực tế và đăng ký trùng.
- **Xem đăng ký**: hiển thị các khoảng học và lịch sử sự kiện. Ngày dự kiến quá hạn có nhãn nhắc xử lý, không tự chuyển trạng thái.
- **Bảng điểm → Chọn lớp/bài thi**: lấy học viên theo ngày thi. Có thể nhập điểm cho người hiện bảo lưu/chuyển lớp/ngừng học nếu họ thuộc danh sách tại ngày thi. Giáo viên phụ trách lớp nhập điểm ban đầu; quản lý chỉ sửa điểm đã có với lý do.
- Lần giáo viên lưu điểm đầu tiên sẽ chốt toàn bộ danh sách dự thi, gồm cả học viên chưa có điểm. Sau đó thay đổi đăng ký không làm thay đổi danh sách. Kỳ thi tương lai chỉ xem trước, chưa được nhập điểm.

## Trạng thái và thời điểm hiệu lực

| Đối tượng | Chuyển trạng thái | Quy tắc |
|---|---|---|
| Hồ sơ | STUDYING ↔ PAUSED/GRADUATED | Không tự đổi đăng ký; PAUSED/GRADUATED chặn khi còn STUDYING; GRADUATED còn chặn RESERVED |
| Đăng ký | STUDYING → RESERVED/DROPPED/COMPLETED | Lý do + version; không hoàn thành trước ngày bắt đầu lớp |
| Bảo lưu | RESERVED → STUDYING/DROPPED | Học lại thủ công; hoặc chuyển cùng khóa/chi nhánh sang một đăng ký STUDYING mới |
| Bảo lưu | RESERVED → RESERVED | Chỉ cập nhật ngày dự kiến, có audit, không mở thêm khoảng học |
| Kết thúc | COMPLETED/DROPPED/TRANSFERRED | Không mở lại; sai sót quá khứ không được sửa bằng chuyển trạng thái tùy tiện |

Ngày nghiệp vụ theo Asia/Ho_Chi_Minh, độ chính xác một ngày. Khoảng học là **[activeFrom, activeUntil)**: tính ngày bắt đầu, không tính ngày kết thúc.

- Ghi danh/học lại mở từ ngày lớn hơn giữa hôm nay và ngày bắt đầu lớp.
- Bảo lưu/ngừng/chuyển lớp ngừng tính ở lớp cũ từ hôm nay; chuyển đến bắt đầu hôm nay hoặc ngày bắt đầu lớp đích.
- Hoàn thành vẫn tính ngày học cuối hôm nay; đóng khoảng học vào ngày mai.
- Chưa hỗ trợ chọn ngày hiệu lực tùy ý, tác vụ hẹn giờ hoặc sửa ngược quá khứ. Nếu bảo lưu/chuyển lớp sau giờ học trong cùng ngày, danh sách điểm danh đã lưu được giữ; danh sách thi đã chốt không đổi. Kỳ thi chưa chốt phản ánh khoảng học hiện tại theo quy ước ngày này.
- reservedSince là ngày bắt đầu bảo lưu hiện tại; expectedReturnDate chỉ kế hoạch. Học lại/chuyển/ngừng xóa thông tin bảo lưu hiện hành nhưng lịch sử còn đầy đủ. Bảo lưu cũ chưa biết ngày giữ null; có thể bổ sung ngày dự kiến mà không bịa ngày bắt đầu.

## Tính nhất quán và phân quyền

SA/Admin/Staff quản lý vòng đời trong chi nhánh được giao. Teacher không được gọi API quản lý hồ sơ/ghi danh/lịch sử; roster thi chỉ giáo viên chính của lớp, không cấp quyền cho người dạy thay một buổi.

Các mutation trạng thái/chuyển lớp bắt buộc **reason dài 3–500 ký tự và version mới nhất**. HTTP 409 nếu dữ liệu đã đổi; frontend không tự gửi lại bằng phiên bản mới. Khóa chi nhánh/học viên/lớp và optimistic version bảo vệ thay đổi đồng thời. Giao diện tải bản ghi mới nhất trước mở form ghi danh.

Chuyển lớp giữ billingEnrollmentId và tuitionFee gốc; không tạo lần thu/học phí thứ hai. Chưa xử lý chuyển khác khóa/chi nhánh, hoàn tiền hay bù trừ giá; thuộc giai đoạn tài chính riêng.

Đề thi đã có điểm hoặc roster đã chốt không được đổi ngày/thang/trọng số. Không có API tự bỏ chốt danh sách; tránh nhập thêm học viên không có bằng chứng. Mọi thay đổi điểm vẫn giữ người nhập gốc và lịch sử sửa.

## API và migration

Thêm 4 API: GET /academic/students/{id}/history, /academic/enrollments/{id}/history, /academic/classes/{id}/roster?date=yyyy-MM-dd và /teaching/exams/{id}/roster. Xem [danh sách](../api-list.md) và [hợp đồng](../api-contracts.md). Tại mốc hoàn thành giai đoạn 4: **104 API chính**: Auth 14, Academic 67, Finance 8, Teaching 15.

[V004](../migrations/V004__learning_lifecycle.sql) thêm version, thông tin bảo lưu, learning_events, trường chốt roster và exam_candidates. Sao lưu, dừng bản cũ rồi áp dụng sau V003 trước khi chạy bản mới. Local đã sao lưu/áp dụng; không tạo lại database và không thay mật khẩu.

Không dựng lại lý do/tác giả cho thay đổi cũ. Các khoảng học sẵn có giữ nguyên. Khi không có khoảng học cũ, chỉ fallback đăng ký hiện STUDYING từ ngày ghi danh/ngày bắt đầu lớp; không suy diễn lịch sử của đăng ký đã ngừng. Điểm cũ luôn được giữ: roster xem trước hợp nhất người đã có điểm với nguồn SAVED_GRADE khi không xác minh được khoảng học; chốt ở lần Teacher lưu tiếp theo. Không coi SAVED_GRADE là chứng cứ về điểm danh hay tham gia cả khóa.

DDL MySQL không rollback như một giao dịch dữ liệu. Muốn quay lại bản cũ phải dừng service và phục hồi từ bản sao lưu đã kiểm tra. Migration chạy lại không reset version, sự kiện, expectedReturnDate hoặc danh sách đã chốt.

## Nghiệm thu

Kiểm thử tích hợp bao phủ bảo lưu, ngày dự kiến, học lại, chuyển từ lớp kết thúc, giữ học phí, chặn hồ sơ tạm ngừng, kiểm tra sĩ số/version, roster theo ngày, ngày hoàn thành, chốt toàn danh sách, người đã có điểm cũ và khóa ngày thi. Kiểm tra MySQL/gateway và UI Staff/Teacher có dữ liệu thử tách biệt, được dọn sau nghiệm thu.

Giai đoạn tiếp theo: **cải thiện 5 — lịch sử điều chỉnh tài chính, sửa sai/hoàn tiền và đối soát**.
