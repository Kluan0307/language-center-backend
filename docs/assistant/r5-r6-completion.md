# R5–R6 — Tài chính, chăm sóc và thống kê

Cập nhật 13/11/2026. Đã triển khai API chỉ đọc, MCP/host, kết quả trên web và kiểm thử tổng hợp. Tiếp nối [R3–R4](r3-r4-completion.md). [Ma trận 120 câu](readonly-audit.md) có audit đến R6; execution từng prompt vẫn NOT_RUN vì chưa chạy nghiệm thu Gemini thật.

## R5 — Tra cứu vận hành

| Nhu cầu | Tool → API | Quy tắc |
|---|---|---|
| Nợ còn lại/quá hạn | `search_tuitions`, `get_debt_statistics` → Finance `/assistant-query/tuitions`, `/debt-statistics` | Billing-root Tuition, không cộng các trang. Balance=max(amount-paidAmount,0); credit tính riêng; aggregate bỏ CANCELLED. Quá hạn là dueDate trước hôm nay và còn balance |
| Khoản sắp đến hạn | `search_tuitions` với dueFrom/dueTo, outstandingOnly=true | Lọc ở SQL trước limit, bỏ đã trả đủ và CANCELLED. Khoảng ngày due date khác snapshot công nợ quá khứ |
| Phiếu thu/hủy/hoàn | `search_receipts` → `/receipts` | Có receiptId/receiptNo, receiptState ALL/VOIDED/REFUNDED/AVAILABLE/LEGACY, from/to. VOIDED/REFUNDED lọc theo ngày correction; số hủy/hoàn trên phiếu là lũy kế, không phải tổng kỳ |
| Hỏi tiếp một phiếu | `search_receipts` bằng receiptId đã xác định | “Phiếu này” chỉ dùng khi kết quả trước có duy nhất một phiếu; hỏi số hoàn phải đọc cả phiếu chưa hoàn để trả đúng 0, không biến thành “không có dữ liệu” |
| Đơn online đang chờ có tính đã đóng không? | `get_usage_help` topic PAYMENT_ACCOUNTING | Nội dung đã duyệt; PENDING chưa là phiếu thu. Không gọi model để suy trạng thái đơn hoặc xác nhận thu tiền |
| Khách theo tên/lịch hẹn | `search_leads` → Academic `/assistant-query/leads` | openOnly mặc định true; NEW/CONTACTED/CONSULTING. TODAY là ngày lịch hôm nay, OVERDUE là trước thời điểm hiện tại. STAFF luôn assignee chính mình, không trả phone/email/note |
| Khách ưu tiên | `list_priority_leads` hiện có | Giữ xếp theo lịch hẹn, không dự đoán khả năng mua. Câu cần lọc hôm nay/quá hẹn/theo tên dùng search_leads để không trả một top chung sai điều kiện |
| Khách của nhân viên cụ thể | `find_management_accounts` → chọn ACCOUNT → `get_care_statistics` | Assignee dùng accountId, không phải Staff profile ID. Trùng tên kiểm lại đúng account trong scope; ADMIN chỉ tra tài khoản STAFF/TEACHER |
| Support theo mã/trạng thái | `search_support_requests` → Academic `/support-requests` | Khi có supportId, host ép openOnly=false để đọc cả yêu cầu CLOSED. Chỉ metadata id/studentId/branch/category/status/time; không subject, trao đổi hoặc ghi chú nội bộ |

Các đường dẫn rút gọn ở bảng đều dưới `/api/v1/{service}/assistant-query`. STAFF được đọc support trong chi nhánh như quyền SupportService hiện có; SupportRequest chưa có assignee, không giả thêm cơ chế “chỉ ticket giao cho tôi”. TEACHER không được đọc tài chính, lead, support nội bộ hoặc tài khoản quản lý.

## R6 — Metric và tổng hợp

| Metric / tool | Nguồn và định nghĩa | Quyền |
|---|---|---|
| `get_debt_statistics` | Sổ Tuition hiện tại theo root: outstanding, credit, billCount. billCount là số khoản học phí khớp bộ lọc, có thể gồm khoản đã trả đủ nếu không lọc; không gọi nó là số khoản còn nợ | SA/A/S; nhóm BRANCH chỉ SA/A |
| `get_cash_flow` | Gross=sum Payment PAID theo paidAt trong kỳ; void/refund=sum PaymentCorrection theo createdAt trong kỳ; net=gross-void-refund. Có thể âm khi hoàn phiếu kỳ trước. Group NONE hoặc BRANCH, count nhóm trước limit | Chỉ SA/A |
| `compare_cash_flow` | Hai kỳ bằng số ngày, không chồng lấn; difference=net kỳ so sánh − net kỳ đầu. Cần đủ from/to/compareFrom/compareTo; không tự chọn kỳ và không chia cho số 0 | Chỉ SA/A |
| `get_care_statistics` | Count lead khớp scope, openOnly, assignee, followUp trước limit; group NONE/BRANCH/ASSIGNEE | SA/A; STAFF chỉ scalar của chính mình |
| `get_support_statistics` | Count yêu cầu OPEN/IN_PROGRESS hiện tại hoặc status chỉ định; nhóm BRANCH do quản trị | SA/A/S, S không nhóm nhiều chi nhánh |
| `get_center_statistics` | BRANCHES_ACTIVE, BRANCHES_WITHOUT_ADMIN, ROOMS_COUNT, CLASSES_COUNT, COURSES_COUNT; khóa chung tính một lần. Nhánh thiếu admin là ACTIVE và không có ADMIN account mở + membership active | SA/A trong scope |
| `get_account_statistics` | Count distinct account theo accountRole/accountStatus; EXISTS membership active tránh đếm trùng người nhiều chi nhánh | SA mọi role; A chỉ STAFF/TEACHER trong phân công |
| Nhân sự, lớp, người học theo chi nhánh | Tái sử dụng get_personnel_statistics/get_roster_statistics/get_academic_statistics từ R2–R4 | Không đổi định nghĩa distinct và quyền đã có |

Ngày nghiệp vụ dùng Asia/Ho_Chi_Minh. Cửa sổ tiền tối đa **366 ngày**, from/to bao gồm cả hai ngày (SQL dùng đầu kỳ inclusive, đầu ngày sau cuối kỳ exclusive). Không trộn tiền tệ: aggregate tài chính chỉ hỗ trợ VND; dữ liệu tiền khác trong phạm vi bị từ chối, không âm thầm cộng/quy đổi.

Công nợ là hiện trạng, dùng paidAmount đang được hệ thống duy trì; không sửa hoặc tự đồng bộ sổ thu trong GET. Chưa có snapshot công nợ/nhân sự/công việc quá khứ. Cash-flow có thể tra lịch sử vì có paidAt/createdAt bất biến. Không lấy khoản thu trước kỳ trừ lại toàn bộ các correction trong đời phiếu để giả dòng tiền kỳ hiện tại.

“Doanh thu” chưa rõ metric sẽ được hỏi lại: hệ thống có **thực thu theo bút toán**, chưa có doanh thu ghi nhận theo thời gian giảng dạy. Các phép tổng hợp thuộc scope đã chọn; câu xếp hạng/tổng hợp nhiều chi nhánh yêu cầu chọn WHOLE_SYSTEM (SA) hoặc ALL_ASSIGNED (A). Nhóm không có dữ liệu không tự dựng thành một hàng; aggregate scalar không có dữ liệu trả số 0.

## API, code và UI

- Thêm **10 GET**: Finance debt-statistics/cash-flow/cash-comparison; Academic leads/care-statistics/support-requests/support-statistics/center-statistics; Auth management-accounts/account-statistics.
- Giữ đường dẫn GET tuitions/receipts, chuyển HTTP handler sang FinanceInsightController để có bộ lọc SQL trước phân trang. FinanceReadService và adapter Java cũ vẫn dùng được; controller cũ không đăng ký route trùng.
- FinanceInsights dùng query cố định và tham số bind; dòng tiền được đối chiếu với RevenueController.overview hiện có trong test. Không có SQL/URL/role/scope tùy ý từ model.
- CareInsights giữ branch/assignee, projection tối thiểu. OrganizationController dùng kiểm actor/scope của PersonnelController; không trả password, token, email, điện thoại.
- Có **36 tool khác nhau**; SA/A thấy **35**, STAFF **27**, TEACHER **16**. [Schema theo role](mcp-tool-catalog.json). Không đổi provider, model hoặc billing.
- Kiểm kê backend: **278 API chính**, 3 alias, 2 diagnostic; Auth 32, Academic 165, Teaching 41, Finance 37, Assistant 3.
- Web hỗ trợ ACCOUNT choice, nhãn tài khoản/nhóm hỗ trợ, công nợ, gross/net, hủy/hoàn, hai kỳ và chênh lệch; định dạng VND kể cả số âm. Giữ BotMessageSquare và Ant Design/styled-components.
- Không seed/reset, không migration hoặc thao tác ghi nghiệp vụ. Các bảng dùng chung hiện tại tiếp tục được dùng khi kiểm phân công tổ chức; nếu tách DB sau này cần API/read model nội bộ thay cho join chéo.

## Kiểm thử và phạm vi nghiệm thu

- Auth: **85/85** test đạt, gồm **17 PersonnelQueryTest** kiểm distinct membership, tài khoản khóa, role và projection.
- Academic: **269/269** test đạt, gồm **36 AssistantQueryTest**; lọc lead, branch/assignee, đếm support, yêu cầu đã đóng và admin bị khóa không được coi là đang phụ trách.
- Finance: **127/127** test đạt, gồm **15 AssistantQueryTest**; số dư/credit/CANCELLED, due date, correction khác kỳ, dòng tiền âm, PENDING, so kỳ, quyền và tiền tệ. Fixture PENDING được tạo trước persist vì status Payment bất biến.
- Assistant: clean test cuối có **172 ca, 171 đạt và 1 live opt-in skip**; FinanceCareExpansionTest có **17 ca**, gồm chọn đúng tài khoản trùng tên và số hoàn 0 của phiếu cụ thể.
- Frontend: **125/125** test đạt, TypeScript/Vite build và ESLint ba file TypeScript thay đổi đạt. Có kiểm render số tiền âm và lựa chọn ACCOUNT.

Log local: `storage/r5-r6-business-tests.log`, `r5-r6-assistant-final-tests.log`, `r5-r6-web-tests.log`, `r5-r6-web-build.log`. Một lượt incremental test gặp bytecode báo “Unresolved compilation problems” trong target; clean test sau đó biên dịch và chạy đạt. Không lấy lượt lỗi làm bằng chứng PASS.

Chưa gọi Gemini thật hoặc thử bằng dữ liệu vận hành trong đợt này. Unit/API/MCP/UI fixture không chứng minh mọi cách diễn đạt của 120 prompt đều được model chọn đúng. Audit vẫn tách READ_CONTRACT_AVAILABLE với execution NOT_RUN; C7/R8 còn nghiệm thu model và tích hợp MySQL thật.

## Khởi động và thử nhanh

Khởi động lại **auth-service, academic-service, finance-service, assistant-service**; tải lại web và tạo hội thoại mới. Gateway giữ cấu hình hiện tại. Thử theo quyền:

- Quản trị: “Tổng nợ quá hạn là bao nhiêu?”, “Thực thu từ 2026-11-01 đến 2026-11-10?”, “Phiếu thu nào đã bị hủy trong khoảng ngày đó?”.
- SA, scope toàn hệ thống: “Chi nhánh nào chưa có admin phụ trách?”, “Admin An đang quản lý những chi nhánh nào?”, “Đếm tài khoản theo role và trạng thái toàn hệ thống”.
- STAFF: “Khách nào của tôi cần liên hệ hôm nay?”, “Tìm khách hàng được giao cho tôi theo tên An”, “Yêu cầu hỗ trợ #1 đang ở trạng thái nào?”.

Tiếp theo là R7 để mở rộng cách diễn đạt/hỏi tiếp và R8 để nghiệm thu toàn bộ. Các giới hạn dữ liệu của R3–R4 (hạn chấm điểm, thời điểm đóng lớp thực tế, lịch chưa sinh) vẫn giữ nguyên; không được thay bằng số liệu phỏng đoán.
