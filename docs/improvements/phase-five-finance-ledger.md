# Cải thiện 5 — Tài chính, sửa sai và đối soát

Triển khai ngày 05/11/2026 trên Finance và frontend Ant Design/styled-components, giữ cấu trúc trang/service/hook hiện có.

## Cách dùng

1. Khởi động lại **finance-service**, giữ Auth/Academic/Gateway đang chạy đúng cấu hình rồi tải lại frontend.
2. **Học phí & công nợ → Xem → Điều chỉnh**: nhập số gốc, giảm trừ, hạn đóng và lý do. Mỗi lần lưu có lịch sử giá/hạn trước và sau, thu ròng tại lúc sửa, người thực hiện.
3. Nếu giảm số phải thu xuống dưới số đã thu ròng, hệ thống hiển thị **tiền dư**. Giảm học phí không tự chi tiền hay sửa các phiếu thu.
4. **Phiếu thu → Mở phiếu → Hoàn tiền**: chỉ hoàn trong tiền dư của học phí và phần còn hiệu lực của phiếu. Chọn CASH/BANK_TRANSFER; chuyển khoản bắt buộc mã giao dịch. Đây là ghi nhận khoản đã thực chi theo chứng từ.
5. Với phiếu nhập nhầm, dùng **Đảo phiếu thu nhầm**, ghi lý do. Chỉ đảo toàn bộ phiếu chưa xử lý/hoàn; sau đó lập phiếu thu đúng nếu có. Không dùng đảo phiếu thay cho hoàn khoản đã thu đúng rồi trả lại.
6. **Báo cáo → Tài chính**: xem thu gốc, đảo, hoàn, thu ròng, công nợ, tiền dư và đối soát nội bộ theo phạm vi chi nhánh.
7. Khi mất mạng hoặc tải lại trang giữa thao tác, chọn **Mở yêu cầu tài chính** để gửi lại đúng nội dung/mã yêu cầu. Nếu đang mở drawer, đóng drawer trước để thao tác banner. Khi còn yêu cầu chờ, giao diện chặn thu mới; yêu cầu thu tiền chờ cũng chặn lập điều chỉnh/đảo/hoàn mới.

## Phân quyền

| Thao tác | Super Admin | Admin | Staff | Teacher |
|---|---|---|---|---|
| Đọc học phí, phiếu, lịch sử | Có | Chi nhánh được giao | Chi nhánh được giao | Không |
| Thu tiền ban đầu | Có | Trong phạm vi | Trong phạm vi | Không |
| Điều chỉnh học phí | Có | Trong phạm vi | Không | Không |
| Đảo phiếu / hoàn tiền | Có | Trong phạm vi | Không | Không |
| Báo cáo và đối soát | Có | Trong phạm vi; allAssigned nếu cần | Không | Không |

Backend kiểm tra vai trò và phân công hiện tại, độc lập với sidebar/giao diện.

## Sổ tài chính và công thức

Phiếu thu gốc giữ nguyên amount, paidAt, trạng thái gốc PAID, số phiếu và người thu. PaymentCorrection ghi thêm VOID hoặc REFUND, lý do, người xử lý, thời gian, phương thức/mã chuyển hoàn. Không có API sửa/xóa journal.

- Thu gốc = tổng phiếu PAID.
- Thu ròng đang giữ của học phí = thu gốc − toàn bộ VOID − toàn bộ REFUND.
- Phải thu = originalAmount − discountAmount, không âm.
- Công nợ = max(phải thu − thu ròng, 0).
- Tiền dư = max(thu ròng − phải thu, 0).
- Số phiếu còn hiệu lực = số gốc − số đảo − số hoàn của riêng phiếu đó.

Ví dụ: thu 1.000.000; giảm học phí còn 700.000 → công nợ 0, tiền dư 300.000. Hoàn 200.000 → thu ròng 800.000, tiền dư 100.000. Không tự mở lại công nợ do hoàn tiền trong phần dư.

Đảo phiếu làm giảm số đã ghi nhận và có thể mở lại công nợ. Sau khi phiếu đã hoàn một phần, không đảo toàn bộ phiếu đó. Muốn hoàn khi chưa có tiền dư, quản lý phải điều chỉnh nghĩa vụ phải thu theo quyết định nghiệp vụ trước. Bảo lưu/chuyển lớp/ngừng học không tự giảm phí hoặc hoàn tiền; root học phí của đăng ký chuyển lớp được giữ nguyên.

paidAmount trong DTO là **thu ròng đang giữ**, không còn chỉ là tổng phiếu gốc. balance không âm; credit là trường riêng. PaymentDto trả voidedAmount, refundedAmount, availableAmount; status vẫn là trạng thái chứng từ gốc.

## Tính nhất quán và phục hồi

Điều chỉnh và correction bắt buộc reason 3–500 ký tự, version học phí mới nhất và requestKey riêng cho mỗi ý định. Khóa Tuition và READ_COMMITTED tuần tự hóa thu/điều chỉnh/đảo/hoàn cùng một khoản. Phiên bản tăng trong cùng giao dịch cập nhật số dư.

Gửi lại cùng key/nội dung trả kết quả đã lưu, kể cả version hiện tại đã đổi. Dùng lại key để đổi số tiền, phiếu, lý do, phương thức, mã tham chiếu hoặc version bị 409. Khóa key duy nhất theo chi nhánh trong từng journal; Payment giữ namespace thu tiền riêng.

Frontend giữ payload trong localStorage theo tài khoản **trước** khi gọi API, khóa sửa nội dung khi chưa xác nhận và chỉ gửi lại nguyên payload. Không tự đổi version hoặc sinh key mới sau timeout. Lỗi từ chối rõ 400/409 yêu cầu đóng/mở lại để tải dữ liệu mới; lỗi mạng/5xx/401/403 giữ yêu cầu để xác minh tiếp. Dữ liệu trình duyệt bị lỗi không được tự xóa bỏ. Lưu trữ local có thể bị người dùng xóa; khi đó phải đối chiếu lịch sử/chứng từ trước khi lập ý định mới.

Mọi mutation kiểm tra paidAmount khớp sổ thu và chặn nếu còn phiếu cũ không PAID chưa xác minh. Nếu lệch, chặn và yêu cầu đối soát; không tự chữa số dư từ dữ liệu cũ chưa xác minh.

## Báo cáo và giới hạn

- grossCollected, voided, refunded tính trong khoảng ngày của **từng giao dịch**; collected và nhóm tháng/khóa/chi nhánh là thu ròng trong kỳ.
- Thu tháng trước, hoàn/đảo tháng này: giữ thu gốc ở tháng trước, ghi khoản giảm ở tháng này; thu ròng kỳ có thể âm.
- outstanding và credit là số hiện tại, không phải số tại ngày kết thúc bộ lọc.
- Đối soát nội bộ có phân trang: cachedPaid so với ledgerPaid và difference; legacyPayments nhắc kiểm tra phiếu cũ không PAID. Không tự thay dữ liệu chênh lệch. Đây chưa phải đối soát sao kê ngân hàng, hóa đơn thuế, Expense hay sổ cái kế toán.
- CASH/BANK_TRANSFER là hai phương thức mới. Hoàn tiền chỉ ghi nhận chứng từ đã chi; chưa có tích hợp tự chuyển khoản, cổng thanh toán, quy trình hai người duyệt hoặc khóa sổ theo ngày.
- Thời gian phát sinh do server đặt theo Asia/Ho_Chi_Minh; chưa nhập lùi ngày hoặc sửa journal. Điều chỉnh sai được xử lý bằng một quyết định mới có lý do, không sửa lịch sử.

## API và migration

Thêm 4 API: GET /tuitions/{id}/history, GET/POST /payments/{id}/corrections, GET /stats/reconciliation. [API list](../api-list.md), [contracts](../api-contracts.md). Tổng hiện tại **108 API chính**: Auth 14, Academic 67, Finance 12, Teaching 15.

[V005](../migrations/V005__finance_ledger.sql) thêm version, mở rộng adjustment_reason, tuition_adjustments và payment_corrections. Sao lưu trước, dừng bản cũ, áp dụng sau V004 rồi khởi động bản mới. Database local đã sao lưu/áp dụng; không tạo lại database, không sửa mật khẩu/số dư/phiếu thu cũ. Migration replay không reset journal/version/số dư.

Lịch sử điều chỉnh đầy đủ bắt đầu từ bản này; các trường adjustedBy/At/reason cũ vẫn giữ thông tin gần nhất. Với giá gốc/giảm trừ cũ bị thiếu, snapshot giá hiệu dụng dùng amount/0 như DTO trước đây; không suy diễn quyết định giảm giá cũ. Không tự biến trạng thái REFUNDED/PARTIAL cũ thành chứng từ hoàn/đảo mới; các dòng này và chênh lệch phải được kiểm tra chứng từ trước khi thao tác. SQL có chẩn đoán, không tự sửa.

DDL MySQL không rollback như một transaction dữ liệu. Quay lại bằng bản sao lưu đã kiểm tra sau khi dừng service; không xóa journal trên database đang chạy để thử quay phiên bản.

## Nghiệm thu

Có 17 test tích hợp mới: tiền dư, partial refund, đảo toàn phiếu, giữ tác giả, idempotency/key collision/version, phương thức hoàn, báo cáo khác kỳ, chênh lệch sổ thu và hoàn đồng thời. MySQL/gateway/UI đã thử timeout sau commit cho cả điều chỉnh và hoàn, reload rồi gửi lại không ghi trùng; kiểm tra cả giao dịch đồng thời và migration replay.

Bước tiếp theo: **cải thiện 6 — chăm sóc Lead, gửi duyệt lại tài liệu, trải nghiệm và hiệu năng**.
