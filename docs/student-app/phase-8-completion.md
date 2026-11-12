# Student App — G8: thanh toán học phí qua VNPAY sandbox

G8 đã hiện thực code backend, student app và màn hình vận hành web. Người dùng chọn VNPAY sandbox và xác nhận chưa có TmnCode/HashSecret hoặc HTTPS callback. **Chưa nghiệm thu với VNPAY sandbox thật**. Môi trường mặc định tắt tạo đơn online, hiện hướng dẫn liên hệ trung tâm; không cần merchant để chạy các chức năng khác.

## Sử dụng

App → Học phí → khoản còn nợ đã xác minh → nhập số tiền → tạo đơn → mở trình duyệt hệ thống → trở về app → kiểm tra trạng thái → xem phiếu thu. Danh sách đơn cho phép khôi phục khi mất phản hồi hoặc đóng app. Chỉ trạng thái backend đã ghi phiếu mới hiện thành công. Không lưu checkout URL, token hoặc mật khẩu trong pending intent. Native lưu UUID và nội dung yêu cầu bằng SecureStore; web giữ trong bộ nhớ, sau reload tra danh sách đơn trên server.

Web → **Thanh toán trực tuyến** cho SUPER_ADMIN/ADMIN/STAFF: lọc đơn, xem trạng thái, phiếu thu, sự kiện đã xác thực. SUPER_ADMIN/ADMIN được truy vấn đối soát với lý do, UUID và version; STAFF chỉ xem theo phân công, TEACHER không có quyền. Không có nút đánh dấu đã thanh toán bằng tay. Sự kiện không tìm thấy đơn chỉ SUPER_ADMIN thấy, chưa gán người nhận.

## Cấu hình khi có merchant

1. Backup database đang sử dụng. Kiểm tra đã áp dụng V014–V016; áp dụng tiếp [V017](../migrations/V017__online_payment_orders.sql). Không xóa bảng hoặc nạp lại clean schema vào database hiện tại. Migration thêm 5 bảng, không ghi lại phiếu thu/số dư cũ. Các bảng có khóa chống lặp request/giao dịch và một đơn đang chờ trên mỗi khoản học phí.
2. Đăng ký merchant **sandbox** với VNPAY, nhận TmnCode/HashSecret. Điền `.env.local` tại backend, không đưa vào app/web hoặc git:

   ```dotenv
   VNPAY_ENABLED=true
   VNPAY_TMN_CODE=<TmnCode sandbox được cấp>
   VNPAY_HASH_SECRET=<HashSecret sandbox được cấp>
   VNPAY_RETURN_URL=https://<gateway-public>/api/v1/finance/payment-providers/vnpay/return
   VNPAY_SERVER_IP=<IP server phù hợp cấu hình merchant>
   VNPAY_TTL_MINUTES=15
   PAYMENT_JOBS_ENABLED=true
   ```

3. Giữ nguyên hai endpoint sandbox trong `.env.example`. Bản G8 giới hạn sandbox, chưa hỗ trợ production. Không dùng profile `payment-test` trên database dự án; profile này chỉ được phép với H2 bộ nhớ hoặc database kiểm thử `lc_student_g8_<timestamp>` và bị chặn khi kết hợp `prod`.
4. Công khai gateway qua HTTPS hợp lệ. Đăng ký **IPN URL** với VNPAY: `https://<gateway-public>/api/v1/finance/payment-providers/vnpay/ipn`. Return URL chỉ đưa người dùng về app; IPN là server-to-server. Không dùng `localhost`/địa chỉ điện thoại cho IPN. Proxy phải giữ nguyên đầy đủ query và không yêu cầu JWT cho đúng hai GET này; chữ ký VNPAY là bằng chứng xác thực. Không mở công khai các API tài chính quản trị.
5. Build/restart Finance và Gateway. Với `mvn spring-boot:run`, Finance đọc env/properties theo cấu hình hiện hành; các biến VNPAY có binding tường minh trong `application.properties`. Build lại app có `expo-web-browser`, cấu hình gateway HTTPS/LAN phù hợp, schema `languagecenterstudent`. Không đặt `EXPO_PUBLIC_PAYMENT_PROTOCOL_TEST=1` trong bản bàn giao.
6. Thực hiện giao dịch sandbox bằng dữ liệu thử do VNPAY cung cấp. Kiểm tra IPN nhận 00, đúng một phiếu thu, netPaid/cache khớp ledger và truy vấn querydr xác minh đúng. Thử hủy/thất bại, return trước IPN, IPN lặp, mất mạng, hết hạn và thu tiền mặt song song. Mở deep link trên Android thật. Chỉ sau bước này mới đổi trạng thái nghiệm thu sandbox.

Tắt `VNPAY_ENABLED` chỉ dừng checkout mới; giữ credentials cho callback/đối soát các đơn đang tồn tại. Không thay/xóa merchant secret trong lúc còn giao dịch chờ. Xoay secret hoặc đổi merchant cần quy trình xử lý các đơn cũ; bản đầu không có key ring đa merchant.

### Tự điền Return URL từ ngrok

Sau khi cấu hình tài khoản ngrok và chạy `ngrok http 8080`, giữ cửa sổ đó mở. Từ backend root chạy:

```powershell
.\scripts\set-vnpay-return-url.ps1
```

Script lấy HTTPS tunnel đang trỏ tới cổng8080, kiểm endpoint Return đúng Language Center rồi chỉ cập nhật `VNPAY_RETURN_URL` trong `.env.local`. Các secret và `VNPAY_ENABLED` được giữ nguyên. Nếu endpoint trả401/404 hoặc tunnel chưa chạy, script không sửa file; restart Finance/Gateway bằng code hiện tại rồi chạy lại. Script in Return/IPN URL công khai để đăng ký với merchant. Nếu dùng domain backend riêng, truyền `-BaseUrl https://<backend-domain>`; không dùng URL trang chủ của website khác.

Sau khi script ghi URL, restart Finance lần nữa để nạp giá trị mới. Khi domain tunnel đổi, chạy lại script và cập nhật IPN tại VNPAY. Script chỉ kiểm tra đường dẫn kết nối, không thay thế nghiệm thu thanh toán sandbox thật.

## Tính đúng đắn tài chính

- UUID request gắn account + nội dung bất biến; retry/recovery không tạo đơn hoặc phiếu mới. Chủ sở hữu kiểm bằng accountId **và** studentId; query/header không thay chủ.
- Khoản học phí phải là billing root đã xác minh theo G7, đang hiệu lực, ledger/cache khớp. Khóa tuition trước order/event, đọc lại sau khóa; thu tay và callback cùng dùng khóa khoản nợ. Giá tiền dùng BigDecimal/decimal string, VND, không cộng/làm tròn bằng JavaScript Number.
- VNPAY 2.1.0/HMAC-SHA512; đối chiếu merchant, tham chiếu, số tiền, mã giao dịch, ngày và **cả** responseCode=00, transactionStatus=00. Tham số trùng/chữ ký sai không vào inbox. Return GET không cập nhật tài chính.
- Inbox được commit trước xử lý; jobs thử lại sự kiện RECEIVED và hết hạn mỗi phút. IPN lỗi lưu/xử lý trả HTTP503 để nhà cung cấp retry. Callback hợp lệ trùng nhận 00, không ghi thêm phiếu; callback thất bại đến sau thành công không đảo phiếu.
- Tiền hợp lệ đến muộn, sai số tiền, vượt nợ mới, khoản bị hủy hoặc transaction trùng đơn khác được giữ REVIEW_REQUIRED. Không gán receipt hoặc ép âm nợ. Querydr do manager yêu cầu phải xác minh chữ ký phản hồi, rồi kiểm lại nợ/ledger trước phân bổ; lỗi chữ ký/mạng/not found trả503 và giữ audit PENDING để retry cùng intent.
- Đơn đã ghi phiếu vẫn SUCCEEDED khi phiếu bị VOID/REFUND; nghiệp vụ đảo/hoàn thuộc ledger hiện hành, xem lịch sử phiếu. Một giao dịch bổ sung chưa phân bổ tiếp tục giữ cảnh báo kể cả query lại giao dịch đã phân bổ.
- Thành công, hết hạn và cần đối soát tạo outbox cùng transaction. **G9 chưa triển khai giao thông báo/push**. Outbox không đồng nghĩa đã gửi cho học viên.

**Giới hạn xử lý tiền:** khoản đã bị trừ nhưng không thể phân bổ vẫn ở hàng đợi đối soát. G8 không gọi API hoàn tiền VNPAY, không tự gán đơn cho tiền không rõ nguồn và không có nút giả xác nhận đã hoàn. Nhân sự cần xác minh/hoàn bên nhà cung cấp; quy trình ghi nhận kết quả hoàn online và xử lý tiền không rõ nguồn còn cần thiết kế khi tích hợp khả năng thực tế của merchant. Không dùng VOID kế toán để tuyên bố ngân hàng đã hoàn tiền.

## API đã hiện thực

Tất cả đường dẫn có prefix `/api/v1/finance`:

| Method | Path | Quyền / mục đích |
|---|---|---|
| GET | `/student/me/payment-config` | STUDENT; trạng thái khả dụng, môi trường, checkoutOrigin |
| POST | `/student/me/payment-orders` | STUDENT; `{requestId UUID,tuitionId,amount decimal string,currency:"VND"}`, trả201 |
| GET | `/student/me/payment-orders` | STUDENT; page,size1..100,state?,tuitionId? |
| GET | `/student/me/payment-orders/{id}` | STUDENT; đúng chủ |
| GET | `/student/me/payment-orders/requests/{requestId}` | STUDENT; khôi phục intent |
| GET | `/payment-orders` | SA/A/STAFF; page,size,state?,tuitionId?,branchId? |
| GET | `/payment-orders/{id}` | SA/A/STAFF; kiểm chi nhánh, không trả checkout URL |
| POST | `/payment-orders/{id}/reconcile` | SA/A; `{requestId,expectedVersion,reason}` |
| GET | `/payment-orders/reconciliations/{requestId}` | SA/A; `{operationStatus,order}`; PENDING không có nghĩa hoàn tất |
| GET | `/provider-events` | SA/A/STAFF; sự kiện đã xác thực theo chi nhánh |
| GET | `/payment-providers/vnpay/ipn` | Public; chữ ký ngân hàng, JSON RspCode/Message |
| GET | `/payment-providers/vnpay/return` | Public; HTML không cache, deep link app, không settlement |

Order DTO: id,tuitionId,state,amount,currency,provider,environment,createdAt,expiresAt,receiptId,message,reviewRequired,version,checkoutUrl. Không trả HashSecret/TmnCode hoặc payload ngân hàng nguyên bản. Thời gian order có offset +07:00. Event DTO là dữ liệu kiểm toán nhân sự, không dùng API student.

12 API mới; tổng hiện tại **225**: Auth24, Academic135, Teaching37, Finance29. Deep links `/payment-orders` và `/payment-orders/{UUID}` giữ đích qua login/đổi mật khẩu, backend kiểm lại sở hữu.

## Kiểm chứng

Kết quả local: **458 tests backend** (Auth61, Academic204, Teaching95, Finance90, Gateway8), **56 app**, **96 web**. TypeScript app, lint/build web và Android Hermes export đạt. Finance có25 kiểm thử nghiệp vụ online và5 kiểm thử chốt cấu hình. Kiểm thử MySQL:63 setup,52 hồi quy G7,42 API G8,9 luồng UI,11 đối chiếu SQL; không có lỗi runtime JS được ghi trong9luồng UI. Không cộng các nhóm này thành số giao dịch ngân hàng thật.

Ảnh kiểm chứng: [đơn chờ](figures/phase8-pending.png), [đã ghi phiếu](figures/phase8-success.png), [STAFF chỉ xem](figures/phase8-web-staff.png), [ADMIN đối soát](figures/phase8-web-admin.png). Kiểm tra popup phát hiện và sửa lỗi web session bị mất khi focus trở lại; token web chỉ giữ trong bộ nhớ, mất khi full reload/logout.

Kết quả chạy thực tế và hash được ghi tại `storage/student-phase8-latest.txt` → thư mục kiểm thử riêng. Báo cáo `phase8-api.json` luôn ghi `realVnpaySandbox=NOT_VERIFIED_NO_MERCHANT_CREDENTIALS`; không diễn giải PASS giao thức cục bộ thành VNPAY đã thu tiền.

Chuỗi tái hiện: `phase3/prepare.cjs --phase=8` → Maven package → `phase3/runtime.ps1 -Phase 8` → `phase8/protocol-server.cjs` → `phase4/live-api.cjs --phase=8` → `phase7/live-api.cjs --phase=8` → `phase8/live-api.cjs`. Mô phỏng chỉ listen loopback29180, dùng secret ngẫu nhiên riêng trong storage, không đưa vào cấu hình thật. Runtime dùng validate và V017 replay2lần. API fixture không thay thế kiểm thử toàn bộ writer học vụ.

Native browser/SecureStore/app-killed và deep link trên điện thoại thật chưa được nghiệm thu; Android bundle hoặc Edge viewport chỉ kiểm chứng phần tương ứng. Database nguồn và release ZIP cũ giữ nguyên.

Tham chiếu giao thức chính thức: [Thanh toán/IPN](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html), [querydr/hoàn tiền](https://sandbox.vnpayment.vn/apis/docs/truy-van-hoan-tien/querydr%26refund.html). Cần đối chiếu cấu hình merchant và phiên bản tài liệu khi nhận credentials thực tế.
