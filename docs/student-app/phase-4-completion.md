# Student App — giai đoạn 4: lớp và lịch cá nhân

Hoàn thành code và kiểm chứng local ngày11/11/2026. Xem [ảnh lịch tuần](figures/phase4-week-grid.png), [lịch sử lớp](figures/phase4-class-history.png), [Trang chủ](figures/phase4-home-next.png). Ảnh lấy từ browser ở390x844 dùng API/MySQL thật, không phải thiết bị Android.

## Đã triển khai

- Học tập: nhóm Đang học/Bảo lưu và Lịch sử; phân trang; chi tiết từng đăng ký, thời kỳ theo học, sự kiện công khai và liên kết lịch của đăng ký.
- Lịch: mặc định danh sách ngày, thêm danh sách tuần và time-grid native; chọn ngày trước/sau/hôm nay, lọc theo đăng ký lớp; trạng thái ngày/view/bộ lọc lưu trên URL.
- Time-grid: cột giờ cố định khi vuốt ngang, cuộn đến ngày đang chọn; chia cột các buổi trùng giờ thay vì che nhau; buổi không đủ giờ nằm ngoài lưới, không gán giờ giả. Chỉ dùng React Native, không nhúng FullCalendar/Ant Design DOM.
- Chi tiết buổi: ngày giờ GMT+7, trạng thái hủy/đã diễn ra/theo lịch, phòng và giáo viên thực tế, học bù/dạy thay, địa chỉ chi nhánh. Không trả ghi chú/lý do nội bộ hoặc danh sách học viên.
- Trang chủ: lấy buổi SCHEDULED gần nhất còn chưa kết thúc; loại buổi hủy, buổi thiếu giờ hoặc không thuộc quyền. Có thể trả buổi đang diễn ra; không suy từ mẫu lịch tuần.
- Deep link lớp/buổi/lịch giữ đích sau đăng nhập và đổi mật khẩu bắt buộc. Sửa race điều hướng lúc xóa phiên sau đổi mật khẩu khiến đích cũ bị thay bằng Trang chủ. Redirect chỉ nhận đường dẫn nội bộ đã kiểm tra.

## API mới — 5 GET

Base `/api/v1/academic/student/me`, chỉ STUDENT đã đổi mật khẩu:

| Path | Hợp đồng |
|---|---|
| /enrollments | page0-based,size1..100; group ALL/CURRENT/HISTORY, status tùy chọn; PageResponse |
| /enrollments/{id} | Chỉ đăng ký thuộc principal; thông tin lớp/khóa/chi nhánh, periods, coverage, publicEvents |
| /schedule | from,to ISO bắt buộc, inclusive tối đa92ngày; enrollmentId tùy chọn, page,size; PageResponse |
| /sessions/{id} | Buổi học thỏa quyền và thời kỳ/ghi nhận của chính học viên; ngoài quyền404 |
| /next-session | {session:null hoặc SessionView,asOf,timezone}; không cần lấy một trang rồi đoán buổi tiếp theo |

Tổng API chính hiện tại184: Auth24, Academic120, Teaching28, Finance12; alias/diagnostics/health giữ cách đếm cũ. Đây là API thực hiện, không phải toàn bộ draft Student App trong G1.

SessionView gồm sessionId,enrollmentId,enrollmentIds,classId,classCode,className,courseName,branch,date,startTime,endTime,status,kind,isMakeup,teacherChanged,teacherName,roomName,legacyEstimated,version,updatedAt. Nếu nhiều đăng ký cùng sở hữu một buổi, trả một buổi duy nhất; enrollmentIds liệt kê các liên kết hợp lệ, enrollmentId là liên kết nhỏ nhất hoặc đăng ký đang lọc.

## Quy tắc dữ liệu thực tế

1. Danh tính lấy từ accountId/profileId đang đăng nhập, không lấy studentId hay X-Branch-Id của client. Các role vận hành bị chặn ở API student; STUDENT vẫn bị chặn ở API quản trị cũ, POST và route mới chưa mở.
2. EnrollmentPeriod dùng khoảng [activeFrom,activeUntil): ngày kết thúc không còn hiệu lực. Giữ lịch sử trước chuyển lớp/bảo lưu; không lấy tất cả buổi chỉ vì học viên từng thuộc lớp.
3. Học bù xét ngày diễn ra thực tế theo quy tắc roster hiện hành. Không tự cấp quyền học bù ngoài thời kỳ chỉ vì buổi gốc trước đó được tham dự. Nếu trung tâm cần học bù vượt thời kỳ, phải bổ sung nghiệp vụ cấp quyền riêng, không nới quyền đọc bằng suy đoán.
4. Legacy STUDYING chưa có periods: chỉ suy từ ngày đăng ký và ngày bắt đầu lớp, gắn legacyEstimated/cảnh báo. Legacy terminal thiếu periods: không suy cả lịch; chỉ cho xem buổi quá khứ có Attendance.classSession liên kết chính xác với đăng ký của mình. Không ghép Attendance chỉ bằng ngày vì có thể có nhiều buổi/ngày.
5. Buổi hủy vẫn hiển thị để học viên biết; next-session không chọn buổi hủy. Thiếu giờ không tự đặt08:00 hoặc tạo duration. COMPLETED của buổi không xác nhận học viên có mặt; chuyên cần thuộc G5. Thời gian học hiển thị theo Asia/Ho_Chi_Minh kể cả điện thoại ở múi giờ khác.
6. PublicEvents chỉ gồm sự kiện đăng ký công khai, trạng thái/ngày/dự kiến quay lại; không trả reason, actor, note nội bộ. COMPLETED ở đăng ký không đồng nghĩa PASS; kết quả học tập thuộc G5.

## Phân trang và truy vấn

Count/page/detail/next dùng cùng predicate sở hữu và eligibility. Truy vấn exists tránh nhân đôi session; fetch class/course/room phù hợp và nạp chi nhánh theo lô, không gọi danh sách giáo viên quản trị bằng JWT STUDENT.

App lịch và danh sách lọc lấy đủ trang100dòng; phát hiện tổng thay đổi, trùng ID hoặc metadata sai thì báo tải lại, không hiển thị lịch thiếu như đã đầy đủ. Lớp học phân trang20dòng trên màn hình. Request hủy khi đổi view/filter/rời trang; kết quả gắn account/path/revision để tránh response cũ ghi đè phạm vi mới.

## Kiểm chứng

| Nhóm | Kết quả |
|---|---|
| Backend toàn bộ5module | 369 tests,0fail/error: Auth61,Academic180,Teaching77,Finance43,Gateway8 |
| Mobile | 34tests; TypeScript và Hermes Android bundle export đạt |
| API/MySQL riêng | 63checks gồm setup/cấp tài khoản và assertions lớp/lịch/ownership/phân trang/validation |
| Browser UI390x844 | 15luồng,0runtime exception; lịch ngày/tuần/grid/filter, detail, history, next, private link404 và giữ đích qua đổi mật khẩu |
| Thiết bị | adb không có thiết bị kết nối; chưa nghiệm thu native back, gesture, SecureStore hoặc iOS |

Bằng chứng: storage/student-phase4-latest.txt -> run riêng; phase4-api.json,phase4-ui.json,summary.json và hash JAR/source. Fixtures đăng ký/buổi là read-model fixtures tạo bằng SQL trong database riêng; cấp tài khoản/đổi mật khẩu qua API thật. Không coi đây là kiểm thử lại toàn bộ workflow ghi danh/thu phí. Unit tests có thêm các tình huống đối nghịch về trùng liên kết và thời kỳ. Các con số là checks đạt, không phải coverage100%.

Web không thay code nghiệp vụ ở G4;96tests web là kết quả G3 được giữ làm lịch sử, không ghi là đã chạy lại ở G4.

## Chạy và chuyển giai đoạn

G4 không có migration mới và không thay schema/dữ liệu gốc. Nếu database chưa cập nhật G3, áp dụng V014 theo [hướng dẫn tài khoản](account-user-guide.md), rồi restart Academic từ source mới và các service theo cấu hình hiện có. App cần EXPO_PUBLIC_API_BASE_URL trỏ Gateway và tài khoản STUDENT thật.

Bộ helper G3 prepare/runtime hỗ trợ thêm --phase=4/-Phase4, con trỏ và tag JVM riêng; không ghi đè bằng chứng G3, không dừng service người dùng. Bản ZIP release trước mobile vẫn giữ nguyên, không chứa phần mở rộng G4.

G5 tiếp theo: công bố điểm, kết quả cuối khóa và chuyên cần cá nhân. Tài liệu/ticket, học phí, thanh toán và thông báo vẫn theo G6–G9. Gate thiết bị thật/bản cài cần hoàn tất trước bàn giao G10.

