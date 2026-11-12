# Tối ưu Học phí và Thanh toán — giai đoạn 1–4

Ngày: 12/11/2026. Đã triển khai source backend và app, kiểm tra API đang chạy và UI mobile web. Chưa thay thế nghiệm thu Android/iOS thật hoặc một giao dịch VNPAY sandbox nhận IPN, ghi phiếu hoàn chỉnh.

## Hành vi mới

Tab **Học phí** gồm **Cần đóng / Lịch sử**. Cần đóng ưu tiên tổng công nợ và các khoản chưa hoàn tất/cần kiểm tra. Lịch sử có **Phiếu thu / Giao dịch online**, không cộng số tiền đơn online với tiền phiếu thu. Tổng quan là toàn bộ học phí đang hiệu lực, kể cả khi danh sách đang lọc một lớp; tổng chưa đủ dữ liệu vẫn là “Chưa xác minh”, không chuyển thành 0.

Luồng thông thường: **Học phí → chọn khoản → xác nhận số tiền → tự mở VNPAY → quay lại app → kiểm tra tự động → xem phiếu thu**. Mặc định toàn bộ số còn thiếu, có lựa chọn đóng một phần. Trên web, cửa sổ được chuẩn bị từ lần bấm xác nhận để giảm lỗi chặn popup. Nếu không mở được, giao dịch vẫn có thể tiếp tục sau khi tải lại điều kiện từ server.

Các nội dung requestId, gửi lại UUID và khôi phục thủ công đã được thay bằng một hành động “Kiểm tra lần thanh toán trước”. Sau mất phản hồi, bộ điều phối tự tìm lại requestId; chỉ retry cùng nội dung, không sinh yêu cầu mới để né lỗi. Khi backend từ chối rõ ràng bằng mã nghiệp vụ, app cập nhật lại điều kiện và không giữ người dùng trong màn hình phục hồi sai.

Màn hình kết quả phân biệt phiên chưa hoàn tất, đang kiểm tra sau khi về từ browser, thành công, thất bại, hết hạn và cần đối soát. Chỉ kết quả server đã ghi phiếu mới hiện thành công. Có cảnh báo riêng khi một đơn đã ghi phiếu còn có giao dịch bổ sung cần kiểm tra.

## API và hợp đồng bổ sung

Tất cả API cá nhân dưới đây chỉ dành cho STUDENT đã xác thực, đúng tài khoản/hồ sơ.

| Method | Đường dẫn sau `/api/v1/finance` | Mục đích |
|---|---|---|
| GET | `/student/me/tuitions/{id}/payment-context` | Hành động phù hợp cho một khoản học phí |
| GET | `/student/me/payment-contexts?tuitionIds=13,14` | Ngữ cảnh theo batch tối đa 20 id cho các card đang hiển thị; bất kỳ id không thuộc chủ sẽ bị từ chối toàn yêu cầu |
| GET | `/student/me/tuitions?status=DUE` | Khoản chưa hủy và còn phải đóng hoặc cần xác minh |
| GET | `/student/me/payment-orders?summary=true` | Phân trang/filter tại database; không trả checkout URL trong bản tóm tắt |

Hai dòng đầu là endpoint mới; hai dòng sau mở rộng endpoint hiện có.

`PaymentContext` gồm `tuitionId, action, reason, message, outstanding, currency, order, sandbox`. `action` là PAY, RESUME, SUPPORT, NONE hoặc UNAVAILABLE. `order` là tóm tắt đơn thuộc tài khoản/hồ sơ hiện tại, không có URL ký. Số dư chưa xác minh có thể null. Ngữ cảnh là dữ liệu phục vụ hiển thị, POST tạo đơn vẫn kiểm tra lại dưới khóa.

Đơn có thêm `displayCode, className, courseName`; phiếu thu có thêm `className, courseName`. Mã GD ngắn phục vụ hiển thị; UUID đầy đủ vẫn được dùng trong API, chi tiết và xử lý hỗ trợ. Không dựa vào mã cắt ngắn để xác định duy nhất giao dịch.

API danh sách đơn mặc định vẫn giữ checkout URL theo hợp đồng cũ. Client mới chủ động dùng `summary=true` và lấy chi tiết mới ngay trước khi mở cổng. Các route cũ của app, API và link thông báo vẫn tồn tại.

Lỗi tạo đơn được phân biệt bằng `code`, `requestRejected: true` và `orderId` khi có thể cung cấp một đơn thuộc chủ. Các mã chính: PAYMENT_DISABLED, PAYMENT_INVALID_AMOUNT, PAYMENT_BALANCE_CHANGED, TUITION_NOT_PAYABLE, PAYMENT_ALREADY_PENDING, PAYMENT_REVIEW_REQUIRED, PAYMENT_INTENT_MISMATCH. Các lỗi mạng, HTTP chung và proxy không có bằng chứng từ chối không được coi là lý do xóa intent. Intent mismatch cần phục hồi/kiểm tra, không tự thay nội dung.

## Tính đúng đắn và trải nghiệm phục hồi

- API đọc ngữ cảnh không tạo đơn, phiếu hoặc ghi số dư. Batch dùng một projection quyền sở hữu và truy vấn tập hợp, không gọi Academic riêng cho từng card.
- Callback và thu trực tiếp vẫn dùng khóa/ledger hiện có. Một khoản có tiền cần đối soát vẫn bị chặn tạo đơn mới, kể cả đơn liên quan đã hết thời gian checkout.
- Khi sổ thu thay đổi trong lúc tạo đơn, backend trả mã cần kiểm tra thay vì để app retry một lỗi xung đột không rõ nghĩa.
- Tên lớp/khóa chỉ là metadata: khi Academic tạm lỗi 5xx, xem trạng thái đơn đã xác minh đúng chủ vẫn hoạt động với nhãn dự phòng. Tạo thanh toán và ngữ cảnh mới vẫn yêu cầu đủ bằng chứng học phí; không bỏ kiểm tra quyền khi thiếu dữ liệu.
- Pending intent có account/request/tuition bất biến. `clearIf` không xóa yêu cầu mới hơn hoặc của tài khoản khác. Khi đang phục hồi khoản A mà mở khoản B, app không gửi tiền của B dưới request A.
- Đổi tài khoản, mất focus và phản hồi đến muộn được chặn bằng generation/ownership. Double tap được chặn trước khi ghi intent hoặc POST.
- Kiểm tra kết quả tự động có backoff, tối đa 10 lượt trong cửa sổ khoảng 75 giây; tạm dừng sau 3 lỗi liên tiếp, không tiếp tục vô hạn. Có thể kiểm tra lại khi quay về app hoặc bấm nút. Không suy hủy/thất bại từ thao tác đóng browser.
- Khi server xác nhận thành công, các query Finance được báo làm mới; thông báo được kiểm tra lại. Dữ liệu tài chính cũ được giữ khi đang tải, kèm trạng thái đang cập nhật/lỗi, thay vì nhấp nháy toàn trang.
- Trang Return có bố cục phù hợp điện thoại, link quay lại app khi callback xác minh được, hướng dẫn riêng khi dùng Expo Go và CSP giới hạn. Return không ghi nhận tiền.
- Nhãn sandbox vẫn rõ ràng; hướng dẫn thẻ test chỉ hiện khi người dùng mở phần hướng dẫn thử nghiệm. Không quét QR thử nghiệm bằng app ngân hàng thật.

## Kiểm chứng

| Phạm vi | Kết quả |
|---|---|
| Toàn bộ Finance Maven tests | 107 đạt, không lỗi |
| Student app Node tests | 77 đạt, không lỗi |
| TypeScript app | `tsc --noEmit` đạt |
| UI ở viewport điện thoại | 8 kiểm tra đạt, không có JavaScript exception; API/browser mở cổng được giả lập, không tạo giao dịch thật |
| API trực tiếp qua Gateway | 13 kiểm tra đọc đạt, gồm batch, filter DUE, summary, route cũ, quyền sở hữu, 401/403/404 và Return |

Kiểm thử Finance gồm chống ghi phiếu trùng, cạnh tranh callback/thu trực tiếp, tiền đến muộn, đối soát, phân trang trạng thái hết hạn, batch ownership và hợp đồng HTTP. Kiểm thử app bổ sung mất phản hồi sau POST, recovery 404 rồi retry cùng request, dịch vụ recovery lỗi, từ chối rõ ràng, pending conflict, đổi account/focus, conditional clear và giữ metadata lỗi API.

Ảnh UI dùng dữ liệu giả lập kiểm thử:

- [Học phí](figures/payment-flow-fees.png)
- [Chi tiết học phí](figures/payment-flow-tuition.png)
- [Kết quả thành công](figures/payment-flow-success.png)
- [Lịch sử giao dịch](figures/payment-flow-history.png)

Evidence cục bộ tại `storage/payment-flow-update/`: `finance-tests.log`, `app-tests.log`, `app-typecheck.log`, `ui-report.json`, `smoke-report.json`. Ảnh thành công là kết quả UI với phản hồi giả lập, không phải bằng chứng giao dịch VNPAY thật.

## Chạy lại và phần còn lại

Không có migration mới, không reset dữ liệu và không đổi merchant/secret VNPAY. Finance đã được chạy lại bằng Maven trên cổng 8084 để nạp bản cập nhật; Auth, Academic và Gateway được giữ nguyên. Reload app/Expo Go để nhận code mới.

Giai đoạn 5 của kế hoạch (bổ sung luồng vận hành web, tìm kiếm/hỗ trợ theo giao dịch) chưa triển khai trong lượt này. Giai đoạn 6 vẫn cần nghiệm thu toàn luồng trên thiết bị thật: VNPAY thẻ test → IPN hợp lệ → đúng một phiếu thu → công nợ giảm; thêm hủy/thất bại, mạng chập chờn và deep link development build. Việc nhìn thấy trang thành công trên VNPAY hoặc trang Return không thay thế kiểm chứng ledger/IPN.
