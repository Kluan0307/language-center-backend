# TN3–TN4 — Giao diện tiếp nhận và nghiệm thu

Ngày cập nhật: 26/11/2026. Đã nối API TN1–TN2 vào web quản lý, dùng Ant Design và styled-components theo cấu trúc hiện có. Không cần migration mới ngoài V033; schema vẫn 100 bảng.

## Cách sử dụng

1. Mở **Khách hàng tiềm năng**. Lead cần ở trạng thái **Đã liên hệ / Đang tư vấn**, đúng phạm vi được quản lý/phụ trách.
2. Chọn **Tiếp nhận học viên** trong thao tác, hoặc mở chi tiết và bấm nút tiếp nhận trực tiếp.
3. **Hồ sơ**: kiểm tra dữ liệu điền sẵn; tạo mới hoặc tìm hồ sơ hiện có. Nhập thêm ngày sinh/liên hệ/giám hộ khi cần. Không dùng trình độ mong muốn thay đánh giá đầu vào.
4. **Đăng ký**: chọn đăng ký ngay hoặc chỉ tạo/liên kết hồ sơ. Lớp mong muốn được chọn sẵn; có thể lọc khóa học, đổi lớp và xem giáo viên, phòng, thời gian, lịch tuần. Lựa chọn chưa giữ chỗ.
5. **Xác nhận**: xem hồ sơ, lớp, học phí, sĩ số và điều kiện. Đối chiếu hồ sơ tương tự, nhập lý do, tích xác nhận rồi tiếp nhận.

Nếu thiếu điều kiện, nút xác nhận bị khóa và hiển thị lý do. Có thể quay lại đổi lớp hoặc chọn chỉ tạo hồ sơ. Học viên mới cần đánh giá đầu vào thì tạo hồ sơ trước, bổ sung đánh giá rồi đăng ký theo luồng phân lớp hiện có; không tự bỏ điều kiện. Với hồ sơ đã tồn tại, giao diện dùng lại thành phần Placement để xử lý ngoại lệ đúng quy trình.

Sau thành công có lối vào hồ sơ, đăng ký, học phí và lớp. Không yêu cầu tìm lại học viên để xếp lớp lần nữa khi đã chọn nhánh đăng ký ngay. Nếu học phí chưa đồng bộ, hiển thị thông báo và hướng người dùng mở đăng ký để kiểm tra/thử lại; không khẳng định đã thu tiền hoặc nhận lại học viên.

**Chỉ liên kết hồ sơ hiện có** không đồng nghĩa người đó chưa có lớp: giao diện chỉ xác nhận không tạo đăng ký mới trong lần tiếp nhận này.

## Ghi nhận và sửa nhu cầu

- Tạo Lead thủ công có thể chọn lớp và khung giờ mong muốn ngay từ đầu; chọn lớp sẽ chọn đúng khóa tương ứng.
- Với Lead đã có, dùng **Nhu cầu lớp** để đổi/xóa khóa, lớp, khung giờ và ghi lý do. Lịch sử trước/sau được giữ lại.
- Màn sửa thông tin Lead giữ khóa học ở chế độ xem, tránh đổi khóa ở một nơi trong khi lớp mong muốn vẫn thuộc khóa khác. Các thay đổi khóa/lớp được thực hiện ở Nhu cầu lớp.
- Chi tiết Lead hiển thị lớp, khung giờ mong muốn và nhãn lịch sử tiếng Việt.
- Các yêu cầu website gần đây được hiển thị để đối chiếu, không tự ghi đè lớp đã xác nhận.

## Mất kết nối và khôi phục

- Trước khi gửi xác nhận, client tạo requestId và giữ nguyên nội dung cho tới khi xác định kết quả.
- Lỗi mạng/5xx không bị coi ngay là tiếp nhận thất bại. Giao diện chuyển sang **Đang xác minh lần tiếp nhận**, cho kiểm tra kết quả hoặc gửi lại đúng yêu cầu.
- Session storage chỉ lưu UUID theo tài khoản và lead, không lưu hồ sơ/ngày sinh/giám hộ trong dấu khôi phục.
- Có thể đóng để kiểm tra sau nếu dấu khôi phục được lưu. Mở lại/refresh trang sẽ có thao tác **Kiểm tra tiếp nhận**, kể cả lead đã chuyển đổi; kết quả được lấy từ receipt.
- Nếu không tìm thấy receipt sau khi kiểm tra, tải lại dữ liệu và xem trước lại. Lead đã chuyển đổi vẫn bị chặn tạo thêm lần tiếp nhận khác ở backend.
- Trường hợp trình duyệt không cho lưu session storage, yêu cầu đang gửi còn được giữ trong bộ nhớ của modal để thử lại; không mở nút đóng kiểm tra sau khi không có dấu lưu.

API bổ sung: **GET `/api/v1/academic/leads/{id}/intake/requests/{requestId}`**. Trả cùng `IntakeResult` của lần xác nhận đã commit; dùng `Cache-Control: no-store`, kiểm tra quyền/lead khớp receipt. Khóa theo lead/chi nhánh để không đọc kết quả dở dang khi xác nhận còn đang xử lý. 404 khi không có receipt tương ứng.

## TN4: kiểm thử và sửa lỗi

- Kiểm thử đồng thời đã phát hiện yêu cầu đọc lead trước khi chờ khóa có thể giữ phiên bản cũ trong persistence context. `LeadService.lock` bỏ bản đọc cũ trước locking read hiện hành; gửi đồng thời cùng UUID trả cùng kết quả thay vì lỗi phiên bản.
- Hai khách tranh chỗ cuối: chỉ một đăng ký thành công, người còn lại nhận 409; không để lại hồ sơ mới dở dang.
- Kiểm thử rollback sau khi đã tạo hồ sơ, hồ sơ tương tự/dùng chung số điện thoại, dùng lại học viên, stale version, quyền nhân viên phụ trách và tương thích website.
- Truy vấn danh sách lead tải kèm lớp mong muốn để tránh thêm truy vấn theo từng dòng.
- Kiểm thử Chrome thật: tạo lead có nhu cầu ngay từ đầu; ba bước tiếp nhận; chỉ tạo hồ sơ; chặn thiếu đánh giá; cố ý làm mất phản hồi sau khi server commit rồi khôi phục qua receipt.
- Kiểm tra bố cục tại 1440 và 390 px. Ảnh nằm trong `docs/intake/screenshots/`.

## Bằng chứng và local

- [Báo cáo TN3–TN4](tn34-completion.json), [nghiệm thu HTTP/MySQL/Chrome](tn34-live.json).
- Script: `scripts/docker/verify-intake-ui-live.cjs`, `scripts/docker/publish-intake-ui-audit.cjs`.
- Nghiệm thu dùng database clone và Academic riêng; Finance/Teaching và push của bản thử bị vô hiệu để không ghi/gửi ra hệ thống thật. Container, database và user tạm được dọn sau thử nghiệm.
- Không tự chuyển lead thật hoặc thêm học viên thật. Đối chiếu checksum các bảng nghiệp vụ trước/sau với backup `ops-1795662452510-ccafaa`.
- Đã cập nhật Academic và web-admin trên Docker. Mở `http://localhost:15173`, tải lại trang nếu tab còn bản cũ. Lệnh khởi động vẫn là `./scripts/docker/compose.ps1 -Action up -Web -Assistant` ở thư mục backend.
- Với Maven/MySQL Windows riêng, cần V033 như hướng dẫn TN1–TN2; không có migration mới ở TN3–TN4.

Các kiểm thử tạm không xác nhận kết nối Finance/VNPAY thật. Luồng thu tiền vẫn sử dụng các trang nghiệp vụ hiện có sau khi đăng ký và đồng bộ học phí thành công.
