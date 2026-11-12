# Student App — Giai đoạn 1: thiết kế và hợp đồng

Ngày: 11/11/2026. Trạng thái: HOÀN THÀNH THIẾT KẾ ĐỀ XUẤT; chưa triển khai endpoint/migration. Không thay đổi release web đã nghiệm thu. Đây là nguồn kế hoạch cho 10 giai đoạn mobile; không thay thế API đang chạy.

## Tài liệu

- [Kế hoạch tối ưu Học phí và Thanh toán — rà soát 12/11/2026](payment-flow-optimization-plan.md)
- [Kết quả tối ưu Học phí và Thanh toán — giai đoạn 1–4](payment-flow-phases-1-4-completion.md)
- [Nghiệp vụ, dữ liệu và phân quyền](domain-and-access.md)
- [Hợp đồng API dự kiến](api-contract.md)
- [Màn hình, lộ trình và nghiệm thu](delivery-plan.md)

## Đối chiếu source

| Bằng chứng | Hiện trạng | Quyết định |
|---|---|---|
| auth Account, Role; Academic/Teaching/Finance Role | Bốn role, Account.sessionVersion, mustChangePassword; không STUDENT | Thêm STUDENT tại mọi bộ giải mã/quyền, deny mặc định; giữ phiên hiện có |
| academic Student | Hồ sơ có studentCode, branchId, version, guardian; chưa accountId | Academic sở hữu liên kết accountId nullable unique; không JPA cascade sang Auth |
| Gateway SecurityConfig | Chuyển tiếp, anyExchange permitAll | Mỗi service tự xác thực và kiểm ownership; Gateway không phải ranh giới quyền |
| teaching Grade, Exam | Có điểm, sửa điểm, rosterFrozenAt; chưa có trạng thái công bố | Công bố snapshot điểm riêng; không suy roster frozen là đã công bố |
| teaching Document | PENDING/APPROVED/REJECTED, classId/courseId | APPROVED là điều kiện cần; phải thêm chia sẻ cho học viên và kiểm đăng ký |
| academic Attendance | PRESENT/ABSENT/LATE, hasPermission, classSession optional | NOT_RECORDED/LEGACY_UNVERIFIED là trạng thái trình bày, không tự ghi ABSENT |
| finance PaymentService | CASH/BANK_TRANSFER, requestKey, khóa học phí, ledger | Không mở POST payments cho STUDENT; xây payment order và adapter xác nhận riêng |
| docs/api-list.md | 173 API chính, chưa Student Portal/online payment/refresh | API mới được đánh dấu đề xuất; không sửa số API hiện hành |
| language-center-student-app/package.json | Expo58, React Native0.88, TS6, template blank, chưa Router | G2 thiết lập điều hướng, design tokens và API client |

## Phạm vi bắt buộc

Tài khoản/kích hoạt/khôi phục; hồ sơ; lớp và lịch sử; lịch cá nhân; điểm công bố/kết quả; chuyên cần; tài liệu; công nợ/phiếu thu; thanh toán sandbox; thông báo trong app/push; yêu cầu hỗ trợ và nơi xử lý trên web.

Ngoài bản cơ bản: QR tự điểm danh, phụ huynh nhiều con, thi online, LMS/video, chat realtime, tự chuyển/bảo lưu/ghi danh. Xin nghỉ/học bù là yêu cầu hỗ trợ, chưa tự động thay đổi nghiệp vụ.

## Quyết định mặc định để triển khai không chờ

- Trung tâm cấp username + mật khẩu tạm qua quy trình xác minh trực tiếp; lần đầu buộc đổi mật khẩu. Không đăng ký công khai bằng mã học viên.
- Khôi phục qua nhân viên trung tâm là kênh cơ bản có thể chạy ngay. Email/OTP tự động là tùy chọn sau khi có dịch vụ và địa chỉ đã xác minh; không dựng OTP giả.
- Bản đầu không refresh token: token hết hạn đăng nhập lại; giữ sessionVersion thu hồi phía server. Khi đổi mật khẩu/khôi phục, đăng xuất toàn bộ thiết bị và gỡ liên kết push cũ.
- Android là thiết bị nghiệm thu đầu; mã nguồn giữ khả năng iOS nhưng không tuyên bố đã kiểm chứng iOS.
- Thanh toán: adapter nhà cung cấp, sandbox trước. Chưa chọn nhà cung cấp; không bịa callback/chữ ký. G8 cần chọn từ tài liệu chính thức và thông tin sandbox. Chưa có thì không đánh dấu thanh toán hoàn thành.
- Giao diện giữ nhận diện web, dùng thành phần native; không đưa Ant Design web vào app.

## Hoàn thành giai đoạn 1 nghĩa là gì?

Đã đối chiếu source, xác định các khoảng trống, chốt defaults, phân service sở hữu, draft API/DTO, quyền, state machine, màn hình và kiểm thử. Không có backend/app mới được nghiệm thu trong giai đoạn này. Các chi tiết provider/push và dịch vụ email được tách thành phụ thuộc triển khai, không là tính năng đã có.

## Tiến độ giai đoạn 2

[Đã triển khai nền tảng app và kiểm chứng local](phase-2-completion.md). Chưa nghiệm thu thiết bị Android thật; API STUDENT thuộc giai đoạn 3.


## Tiến độ giai đoạn 3

[Đã triển khai tài khoản và hồ sơ](phase-3-completion.md) · [Migration và hướng dẫn sử dụng](account-user-guide.md). Thiết kế G1 giữ như đề xuất ban đầu; G3 ghi rõ coordinator DB và API thực tế.


## Tiến độ giai đoạn 4

[Đã triển khai lớp và lịch cá nhân](phase-4-completion.md): 5API đọc mới, lịch ngày/danh sách tuần/time-grid native và buổi tiếp theo. Không có migration mới. Bằng chứng local tách khỏi yêu cầu thiết bị thật.



## Tiến độ giai đoạn 5

[Điểm công bố và chuyên cần cá nhân](phase-5-completion.md). Có V015 và giao diện công bố/thu hồi trên web. Bước tiếp theo G6: tài liệu/hỗ trợ.


## Tiến độ giai đoạn 6

[Đã triển khai tài liệu và hỗ trợ](phase-6-completion.md). Chia sẻ/thu hồi trên web, tải qua bearer header; ticket có phân quyền, ghi chú nội bộ riêng, version/UUID. V016 chưa áp dụng database nguồn.


## Tiến độ giai đoạn 7

[Đã triển khai tài chính cá nhân](phase-7-completion.md): khoản thu theo billing root, tổng toàn tài khoản, phiếu và lịch sử đảo/hoàn, hỗ trợ gắn tài nguyên tài chính. Không tự sửa ledger khi GET; dữ liệu chưa xác minh không ra tổng0 giả. Không migration mới.


## Tiến độ giai đoạn 8

[Code thanh toán online và cấu hình sandbox](phase-8-completion.md). Đã có orders/IPN/querydr/ledger/outbox, app theo dõi và web đối soát. Mặc định tắt checkout; môi trường local ngày 12/11 đã cấu hình merchant/HTTPS, bật sandbox và mở được trang chọn phương thức VNPAY. Chưa nghiệm thu toàn bộ giao dịch thật qua IPN. [Đợt tối ưu giai đoạn 1–4](payment-flow-phases-1-4-completion.md) cập nhật luồng học phí/thanh toán.


## Tiến độ giai đoạn 9

[Inbox, home, thiết bị và hướng dẫn cấu hình](phase-9-completion.md). Chạy bằng Expo Go khi chưa có EAS/FCM; push thật chờ credentials và thiết bị.

## Tiến độ giai đoạn 10

[Dọn runtime, cấu hình bàn giao và kết quả kiểm thử](phase-10-completion.md). Đã gỡ preview/UI thử nghiệm, bổ sung profile APK/AAB và kiểm tra release; 503 test backend, 80 test app, 96 test web đạt. Android release bundle đã export và scan. APK đối chiếu manifest build; nghiệm thu Android/push thật vẫn chờ thiết bị theo lựa chọn của người dùng.
