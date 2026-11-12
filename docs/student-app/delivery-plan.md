# Màn hình, lộ trình và tiêu chí nghiệm thu

## Mobile UI

Giữ logo/màu/chữ trạng thái web qua design tokens, thành phần native riêng. Chưa chọn UI library trước G2; không dùng Ant Design DOM hoặc nhúng web làm màn hình chính. Safe-area, bàn phím, Android back, kích thước chữ hệ thống, contrast và vùng chạm tối thiểu44pt/48dp cần kiểm tra thiết bị thật. Mọi màn hình có loading/empty/error/retry/unauthorized; không flash dữ liệu account cũ.

| Tab/stack | Màn hình và hành động |
|---|---|
| Auth ngoài tabs | Login, kích hoạt/đổi password bắt buộc, khôi phục qua trung tâm; không public signup |
| Trang chủ | Buổi tiếp theo, cảnh báo đổi lịch, số thông báo, link công nợ; thẻ module lỗi riêng, không báo toàn hệ thống trống |
| Lịch học | Ngày/tuần/danh sách; mặc định agenda; lọc lớp; session detail; tuần time-grid cuộn hợp lý, không drag/drop |
| Học tập | Lớp đang học/lịch sử -> overview/grades/final-result/attendance/documents |
| Học phí | Khoản phải đóng -> chi tiết -> nhập số tiền -> checkout -> pending/result; receipt list/detail; lịch sử orders để khôi phục |
| Cá nhân | Profile -> yêu cầu sửa; password; preferences; hỗ trợ list/create/detail/reply; liên hệ; logout-all |
| Header | Inbox -> đọc -> mở resource đúng quyền; deep link khi chưa login phải giữ đích và kiểm lại sau login |

Không sidebar desktop, bảng nhiều cột, dashboard biểu đồ dày hoặc nút quản trị trong student app. Cache chỉ để tăng tốc: badge offline/cập nhật lần cuối, không dùng cache xác nhận tiền/điểm mới. Token SecureStore, public API URL không chứa bí mật; không lưu mật khẩu, không cache PDF vào vùng chia sẻ vô hạn; xóa cache dữ liệu cá nhân khi logout.

## 10 giai đoạn

| Giai đoạn | Đầu việc / đầu ra | Gate |
|---|---|---|
| 1 Thiết kế | Bộ tài liệu này, gap/API/quyền/state/màn hình/acceptance | Đã đối chiếu source; endpoint mới chưa được tuyên bố chạy |
| 2 Nền app | Router, theme theo web, UI components, env.example, client/error/session, typed DTO skeleton | Chạy thiết bị Android, typecheck; màn hình placeholder có nhãn dev, không fake thành dữ liệu thật |
| 3 Account | Role mọi service, migration liên kết/audit, provisioning có phục hồi, Auth API, web cấp/lock/reset, login/profile/change/recovery | Unique/concurrency; lock/revoke; 2 student không đọc chéo; bốn role web regression |
| 4 Lớp/lịch | Academic student API, list/detail/history/agenda/timegrid, Trang chủ next session | Period chuyển/bảo lưu, hủy/bù, pagination/timezone đúng |
| 5 Điểm/chuyên cần | Publication snapshot + web publish/retract; student grades/results/attendance | Không lộ draft/comment nội bộ; sửa điểm không tự publish; legacy không giả vắng/đạt |
| 6 Tài liệu/hỗ trợ | Sharing + download auth, request lifecycle, web xử lý, app pages | Chặn URL file ngoài quyền, request đúng scope, tách public/internal |
| 7 Học phí/phiếu | Owned queries theo billing root, UI công nợ/receipt/corrections | Không trùng nợ chuyển lớp; tổng không cộng từ một trang; không lộ ghi chú |
| 8 Online payment | Provider sandbox, orders/inbox/settlement/reconcile +web/app | Duplicate/out-of-order/timeout/race đúng; không âm dư; provider/config còn thiếu thì ghi blocked chức năng này |
| 9 Thông báo/home | Outbox/internal delivery/inbox/push/device prefs, cập nhật web event producers | Commit-only/dedup/đúng recipient; app đóng vẫn có inbox; deep link kiểm quyền |
| 10 Bàn giao | E2E thật, thiết bị, regression web, build Android, docs/reset cấu hình | Không mock runtime, không bí mật trong APK, kiểm thử minh bạch sandbox/production |

Không bắt buộc tạo service mới hay broker chỉ để đủ kiến trúc. Migrations incremental tiếp sau V013 phải xác minh số cuối lúc thực hiện; không sửa migration cũ/clean dump bất biến. Shared schema hiện hành nhưng không sao chép dữ liệu nghiệp vụ sang DB mobile, không cascade account/student. Snapshot web release cũ giữ nguyên.

## Ma trận nghiệm thu tối thiểu

| ID | Trường hợp | Kỳ vọng |
|---|---|---|
| S01 | Cấp account hai request đồng thời cho một Student | Một liên kết duy nhất; retry tra cứu được; không orphan có quyền |
| S02 | Giả mã học viên/email người khác | Không nhận liên kết, không public enumeration |
| S03 | Mật khẩu tạm, đổi, reset, khóa | Bắt đổi trước đọc data; phiên cũ vô hiệu; profile status không khóa nhầm |
| S04 | Student A thay mọi id/query/header thành B | API/detail/file/payment/order/request trả404 hoặc403 đúng quy ước; không lộ counts |
| S05 | STUDENT gọi API quản trị cũ/4 role gọi API student | Deny; không thêm role vào allow-all hoặc điều kiện chỉ cấm TEACHER |
| S06 | Học nhiều lớp, hơn một trang buổi | Đủ và không trùng, đúng ngày giờ, không roster |
| S07 | Chuyển lớp/bảo lưu/học bù/hủy | Period áp đúng, giữ lịch sử, không thấy buổi không có quyền |
| S08 | Điểm draft, publish, sửa, republish, retract | Chỉ snapshot được phép; không tự lộ sửa mới/nhận xét cũ |
| S09 | Kết quả CONFIRMED rồi reopen/dependency lỗi | Không giữ kết luận stale; UI đang cập nhật/503 đúng |
| S10 | Chưa điểm danh, ABSENT có phép, LATE, legacy | Nhãn/phép tính đúng domain, không fabricated0% |
| S11 | Document pending/unshared/revoked/course khác branch | List và download đều chặn, kể cả URL bookmark |
| S12 | Phí nhiều đợt/chuyển lớp/VOID/REFUND | Net/còn nợ/phiếu khớp ledger, không cộng đôi |
| S13 | Bấm thanh toán2lần/mất phản hồi/đóng app | Cùng intent, khôi phục order; không ghi2phiếu |
| S14 | Callback giả/sai tiền/trùng/đảo thứ tự/đến muộn | Xác minh+dedup; REVIEW_REQUIRED khi cần; không mark paid từ return URL |
| S15 | Thu tay song song callback hoặc công nợ bị điều chỉnh | Không overallocate/âm nợ; tiền thật không bị bỏ quên |
| S16 | Ticket khác branch/internalNote/CLOSED | Scope đúng; không lộ nội bộ; không sửa học phí/điểm tự động |
| S17 | Event rollback/lặp, push denied, đổi account | Không ghost/duplicate; inbox còn; không gửi sang account mới |
| S18 | Logout/account switch/mạng yếu/expired login/deep link | Clear private cache, lỗi có retry, trở về đích sau auth kiểm quyền |
| S19 | Cài Android build thật trên máy khác | API LAN/HTTPS phù hợp, không localhost thiết bị, config không chứa secrets |
| S20 | Hồi quy web và dữ liệu nguồn | Bốn role hiện tại không bị mở/đóng quyền sai; migration có backup, không reset dữ liệu |

Không cộng các hàng trên thành số test đã PASS; G1 là kế hoạch. Mỗi giai đoạn triển khai ghi report thay đổi/API/migrations/test thật/giới hạn. Android và sandbox là phạm vi nghiệm thu đầu, iOS/Internet/production không tự suy ra.

## Phụ thuộc cần cung cấp đúng thời điểm

G2: nhận diện lấy từ FE hiện có; không cần user gửi lại. G3: danh sách người quản lý cấp tài khoản và kênh xác minh thực tế dùng mặc định domain. G8: nhà cung cấp và sandbox credentials lưu ngoài git. G9: push project/platform credentials, quyền development build thực tế. G10: thiết bị và địa chỉ triển khai. Chưa có credentials không tạo thành công giả.

### G4 đã thực hiện

[Lớp/lịch cá nhân và bằng chứng](phase-4-completion.md). Giữ gate native device cho G10; phần code/local qualification G4 đã đạt. API hiện thực theo báo cáo G4, các nhóm còn lại trong draft API vẫn là kế hoạch.


### G5 đã thực hiện

[Code, hợp đồng và kiểm chứng](phase-5-completion.md). Điểm chỉ từ snapshot công bố; kết quả cuối khóa kiểm source; chuyên cần không suy legacy thành có mặt/vắng. Gate native device giữ cho G10.


### G6 đã thực hiện

[Tài liệu/hỗ trợ và bằng chứng](phase-6-completion.md). Native share sheet trên thiết bị thật giữ gate G10; nhóm API và workflow local đã kiểm chứng. Tiếp theo G7: học phí/phiếu thu cá nhân.


### G7 đã thực hiện

[Tài chính cá nhân và bằng chứng](phase-7-completion.md). Chống cộng nợ trùng chuyển lớp, decimal string, summary toàn tài khoản, VOID/REFUND riêng và cảnh báo legacy. Thanh toán online vẫn thuộc G8; native device là gate G10.


## G8 — code/local đã thực hiện, sandbox còn chờ

[Kết quả và các gate còn lại](phase-8-completion.md). Người dùng chọn VNPAY, chưa có merchant/HTTPS. Không đánh dấu nghiệm thu sandbox thật; cần cấu hình và chạy checklist trước sử dụng. G9 hiện có code/local; xem báo cáo G9 bên dưới.


## G9 — code/local đã thực hiện, push thật còn chờ

[Kết quả và cấu hình](phase-9-completion.md). User chưa có EAS/Firebase: inbox/home đã kiểm chứng độc lập, push thật chưa nghiệm thu. Tiếp theo G10 bàn giao và các gate thiết bị/provider.

## G10 — bản nguồn bàn giao và kiểm thử tự động

[Kết quả, build và các gate còn chờ](phase-10-completion.md). Đã dọn preview, nội dung thẻ test và icon template khỏi runtime; có kiểm tra cấu hình release, profile EAS và script APK local. Người dùng chọn hoàn thiện build/hướng dẫn trước khi có Android kết nối. Nghiệm thu thiết bị, ký phát hành store và push thật chưa được đánh dấu hoàn thành.
