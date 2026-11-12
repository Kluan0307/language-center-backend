# Nghiệp vụ, dữ liệu và quyền Student App

Thiết kế dự kiến, 11/11/2026. Tên bảng/cột cuối cùng phải đối chiếu migrations trước G3; không tạo schema ở G1.

## Sở hữu dữ liệu

| Service | Bổ sung dự kiến | Ràng buộc |
|---|---|---|
| Auth | Role STUDENT; cấp tài khoản, kích hoạt, reset; sự kiện bảo mật | Account không bị xóa cùng hồ sơ; không dùng branchAssignments nhân sự để cấp quyền học viên |
| Academic | students.account_id nullable unique; liên kết có audit; yêu cầu hỗ trợ, phản hồi công khai; hồ sơ/lịch/chuyên cần cá nhân | FK logic tới accounts; không cascade; liên kết chỉ tới STUDENT; truy vấn ownership mọi lần |
| Teaching | grade_publications + publication_items snapshot, revision/audit; quyền chia sẻ tài liệu | Công bố theo bài kiểm tra; mỗi snapshot chỉ điểm hợp lệ; không xuất roster toàn lớp |
| Finance | payment_orders, provider_events/inbox, settlement/reconciliation, outbox | unique provider transaction, idempotency intent, tiền chính xác, nguồn ledger hiện có duy nhất |
| Auth (thiết bị) / Academic (hộp thư) | push_devices; notification/inbox + delivery/outbox | Push token thuộc account/installation; inbox học viên do Academic sở hữu; không thêm service mới ở bản đầu |

Các service phát sự kiện tích hợp qua outbox vào giao diện internal có xác thực máy–máy, dedup eventId; không cấp endpoint internal cho student JWT. Chưa có broker thì worker HTTP retry, không transaction phân tán. Mất push không làm mất thông báo trong inbox. Home tổng hợp ở app từ API từng service, không tạo BFF mới.

## Tài khoản và danh tính

Một hồ sơ tối đa một account, một STUDENT account tối đa một hồ sơ. Hồ sơ chưa có account vẫn vận hành trên web. Cấp account thực hiện Auth tạo STUDENT trạng thái hạn chế, Academic liên kết có version/lock, Auth mới cho kích hoạt; lỗi giữa bước được tra cứu/retry bằng cùng requestId. Orphan account không được truy cập dữ liệu và hiển thị hàng chờ xử lý; không rollback bằng xóa hồ sơ. Chặn tạo trùng bằng unique DB, không chỉ kiểm tra trước.

SA hoặc ADMIN có phạm vi được cấp/khóa/reset; STAFF xem tình trạng liên kết và gửi yêu cầu, không reset/cấp mặc định. ADMIN chỉ thao tác hồ sơ thuộc chi nhánh quản lý; không sửa account chung ngoài phạm vi. Chuyển role STUDENT sang nhân sự bị chặn qua API generic. Không tự động liên kết theo phone/email vì có thể trùng giữa người thân.

Mật khẩu tạm tối thiểu 8 ký tự, tối đa 72 byte UTF8 theo Auth hiện hành; không dùng mật khẩu chung demo. Đăng nhập nhận mustChangePassword; chỉ me/password, me/logout-all và thông tin tối thiểu được dùng đến khi đổi xong. Kích hoạt là lần đổi mật khẩu bắt buộc sau xác minh, không cần public activation API ở bản đầu.

Khôi phục: app hiển thị liên hệ chi nhánh và hướng dẫn, không cho tra cứu public xem username tồn tại. Nhân viên tiếp nhận rồi ADMIN/SA xác minh và reset; ghi audit, revoke mọi phiên, cấp mật khẩu tạm an toàn. Đăng xuất bản đầu thu hồi tất cả phiên (nút ghi rõ), xóa token/cache và thiết bị push. Account khóa không đọc được data kể cả JWT chưa hết hạn. Student.status PAUSED/GRADUATED không đồng nghĩa account bị khóa.

## Quyền và lịch sử

Principal -> account đang hoạt động/sessionVersion -> liên kết Student -> ownership enrollment/tuition/session/publication/document/request. Không tin studentId/accountId/branchId trong body/header. Resource ngoài quyền trả404, role không phù hợp403, hết phiên401. Thiếu liên kết403 STUDENT_PROFILE_UNAVAILABLE; dependency lỗi503, không giả như không có dữ liệu.

Hồ sơ hiện có một branchId. Bản đầu không tự gộp hai Student theo SĐT, không tự tạo khả năng học nhiều chi nhánh chưa có trong nghiệp vụ. Branch là thông tin; quyền theo chính liên kết và registration thực tế. Nếu hỗ trợ chuyển xuyên chi nhánh tương lai phải thiết kế identity/migration riêng. Không được bỏ lọc sở hữu khi branch thay đổi.

| Tài nguyên | Học viên được phép | Không được phép |
|---|---|---|
| Hồ sơ | Xem trường cá nhân; yêu cầu sửa | Tự sửa tên/ngày sinh/liên kết hoặc xem ghi chú nội bộ |
| Lớp/đăng ký | Xem đăng ký của mình, thời kỳ, trạng thái, lịch sử công khai | Danh sách bạn học, số điện thoại cá nhân giáo viên, lý do nội bộ |
| Lịch | Buổi thuộc thời kỳ đăng ký có hiệu lực; giữ buổi lịch sử liên quan | Mọi buổi của lớp chỉ vì từng học một ngày |
| Điểm | Snapshot công bố cho bản thân | Điểm nháp, before/after nội bộ, điểm người khác |
| Chuyên cần | Bản ghi của mình, thống kê có quy tắc | POST bulk hoặc tự xác nhận có mặt |
| Tài liệu | APPROVED + sharedToStudents + thuộc lớp/khóa có quyền | File PENDING, course cùng tên nhưng khác branch, storageKey tuyệt đối |
| Tiền/phiếu | Công nợ/phiếu của billing root thuộc mình | Ghi phiếu thủ công, sửa học phí, tự hoàn tiền |
| Hỗ trợ | Tạo/xem/phản hồi/đóng yêu cầu của mình | Ghi chú nội bộ, yêu cầu học viên khác |

Học xong/chuyển/bảo lưu không mất lịch sử điểm/thu tiền. Tài liệu được chia sẻ tiếp tục xem khi còn đăng ký lịch sử không bị hủy; văn bản bị thu hồi hoặc ngừng chia sẻ thì download bị chặn. Đăng ký hủy trước khi học không cấp quyền học liệu; vẫn xem giao dịch của mình nếu có.

## Công bố điểm và kết quả

Grade/Exam hiện chưa publication. G5 bổ sung publish/retract theo exam, version và reason; Teacher chính của lớp hoặc SA/ADMIN đúng phạm vi. Snapshot chứa score/maxScore/studentComment và studentId/enrollmentId từ roster đã xác minh. Chỉ publish roster frozen; thiếu điểm hiển thị NOT_GRADED, không bằng0. Điểm nhập sau publish không tự xuất ra app: phải republish revision mới; giữ snapshot trước trong audit. Retract làm app không đọc snapshot cũ, không gửi giá trị điểm qua push.

Kết quả cuối khóa chỉ hiện khi current CONFIRMED và nguồn hợp lệ; REOPENED/NEEDS_REVIEW ẩn kết luận cũ, hiện đang cập nhật. Không coi điểm từng bài là điểm cuối khóa. Nhận xét công khai cần trường studentComment riêng, mặc định null, không tự công bố comment cũ.

## Lịch và chuyên cần

Timezone Asia/Ho_Chi_Minh; date ISO yyyy-MM-dd, instant có offset/Z. Buổi thực tế là nguồn; SCHEDULED không tính vắng, CANCELLED không vào mẫu số; chỉ COMPLETED và học viên đủ điều kiện theo thời kỳ mới tính. NOT_RECORDED khác ABSENT. LATE được tính có tham dự; hasPermission của ABSENT chỉ nhãn có phép, không tự xóa khỏi mẫu số.

Tỷ lệ hiện tại = (PRESENT+LATE)/số buổi COMPLETED đủ điều kiện *100. Trả thêm số buổi chưa ghi nhận/legacy và coverageComplete. Khi thiếu bản ghi/legacy không xác minh: participationPercent=null, UI hiện số đếm và chưa đủ dữ liệu, không tính tỷ lệ chính thức. Đây là thống kê app, không sửa chính sách chốt kết quả/đủ phút backend. Xin nghỉ chỉ là request, không tự ghi hasPermission hay sửa Attendance.

## Tiền và thanh toán

Tuition/billing root và ledger hiện có là nguồn công nợ duy nhất; không cộng trùng khi chuyển lớp. Chỉ VND trong bản đầu; amount API là decimal string, không float; mỗi order trả một tuition, thanh toán một phần trong giới hạn còn nợ.

Order: CREATED -> PENDING -> SUCCEEDED | FAILED | EXPIRED; trạng thái return app không thay đổi tiền. EXPIRED có xác nhận tiền muộn -> REVIEW_REQUIRED, xử lý đối soát; không âm thầm ghi thất bại vĩnh viễn. REFUND/VOID là nghiệp vụ ledger riêng, không ghi đè lịch sử order SUCCEEDED.

Server kiểm số tiền, provider reference, chữ ký, currency, order ownership; ghi inbox event trước xử lý; khóa order + tuition, unique transaction và idempotency để ghi đúng một receipt. Cùng requestId/payload trả cùng intent; khác payload409. Khi công nợ đổi trong lúc chờ, không thu vượt dư: tiền đã thực đến nhưng không phân bổ được vào REVIEW_REQUIRED, nhân viên xử lý/hoàn có audit. Mất kết nối/chưa biết kết quả trả trạng thái chờ, app query order, không tự tạo lại sau timeout.

Bản đầu chặn nhiều order đang PENDING cùng tuition; payment thủ công vẫn theo ledger hiện hành, callback phải xử lý race. Không cung cấp nút học viên tự đánh dấu đã đóng. Sinh biên nhận sau settlement thật; demo sandbox có nhãn thử nghiệm, không lẫn tiền thật.

## Hỗ trợ và thông báo

Request: OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED; học viên được phản hồi và đóng, phản hồi RESOLVED có thể mở lại OPEN; CLOSED không sửa, tạo mới. Loại PROFILE/GRADE/ATTENDANCE/PAYMENT/LEAVE/MAKEUP/OTHER; linkedResource phải thuộc mình. Không attachment ở bản đầu để giảm rủi ro file. Nhân viên chỉ xử lý request đúng branch; giáo viên không đọc ticket cá nhân mặc định. Ghi chú nội bộ không trả trong DTO student.

Thông báo chỉ phát sau commit, dedup eventId+recipient. Inbox là nguồn, push là tùy chọn; lock account/gỡ thiết bị thì ngừng gửi. Đổi account trên một máy phải gỡ token cũ. Push không chứa điểm/tiền/họ tên nhạy cảm trên lockscreen; chỉ thông điệp chung và resource ID được kiểm quyền khi mở. readAt độc lập trạng thái giao push. Hạn nhắc phí không gửi lại vô hạn, cần key theo tuition/dueDate/kỳ nhắc.
