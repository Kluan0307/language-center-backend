# C2 — API tra cứu cho trợ lý

Ngày cập nhật: 13/11/2026. Tiếp nối [C1](README.md); chỉ triển khai backend truy vấn. Chưa có MCP server, vòng lặp agent, chat UI hoặc lời gọi Gemini. Không sửa `.env.local`, không seed/reset database và không có migration mới.

## 1. Đã triển khai

| Backlog | Kết quả |
|---|---|
| GAP-01 | Khoảng tiền BigDecimal lọc trước phân trang; phân biệt `<` và `<=`; mặc định khóa OPEN riêng cho trợ lý. API courses cũ có thêm query tùy chọn, mặc định cũ giữ nguyên. Khóa chung chỉ xuất hiện một lần |
| GAP-02 | Projection lớp/buổi thật; lọc trạng thái và còn chỗ tại SQL; lịch giới hạn 31 ngày; giáo viên chỉ lớp chính mình và buổi actual teacher; đếm trước limit |
| GAP-03 | Tìm tên/mã học viên với projection không phone/email; lớp học viên có ACTIVE/NOT_STARTED/INACTIVE/RESERVED/TRANSFERRED…; giáo viên chỉ nhìn roster/lớp chính mình, không mở StudentController chung |
| GAP-04 | TOTAL_PROFILES, PROFILE_STUDYING và COUNT DISTINCT ACTIVE_IN_CLASS; khoảng học có ngày kết thúc exclusive; có scope/observedAt; không nhầm hồ sơ STUDYING với đang có lớp |
| GAP-05 | Điểm danh NOT_RECORDED thay vì PRESENT mặc định; roster theo ngày và bản ghi cũ, phân trang tại SQL; hủy/tương lai trả trạng thái buổi. Điểm CURRENT/PUBLISHED tách endpoint/quyền, 0 khác chưa chấm, STAFF chỉ bản công bố còn hiệu lực |
| GAP-06 | Lead ưu tiên theo nhóm hẹn, thời gian và id ổn định, trước LIMIT, có priorityReason; STAFF bị ép người phụ trách hiện tại. Phiếu thu có aggregate scope; DTO giữ số hủy/hoàn/khả dụng và dùng logic ledger cũ |
| GAP-07 | Envelope chung, requestId, status, scope, source, observedAt, nextPage/hasMore; projection tối thiểu; link relative do server dựng; hai chủ đề hướng dẫn cố định WEEKLY_CALENDAR/FIND_COURSES |

Ngày hiện tại lấy Asia/Ho_Chi_Minh. `asOf` ở thống kê và lớp học viên hiện chỉ chấp nhận hôm nay (hoặc bỏ trống). Không có snapshot trạng thái lịch sử: ngày khác bị từ chối 400, không dùng trạng thái hiện tại để giả trả lịch sử. `ACTIVE_IN_CLASS` yêu cầu Enrollment STUDYING, khoảng hiệu lực, lớp chưa kết thúc và đã bắt đầu; legacy không có period dùng enrolledAt và trạng thái hiện hành. Lớp cũ còn trạng thái STUDYING nhưng không còn hiệu lực trả INACTIVE.

`recruitingOnly` nghĩa là còn chỗ theo dữ liệu: khóa OPEN, lớp chưa kết thúc, currentSize < maxSize. Không bảo đảm học viên cụ thể đủ điều kiện ghi danh/trình độ/lịch; vẫn cần quy trình ghi danh hiện tại. CURRENT grades dùng danh sách dự thi đã đóng băng để hiện người chưa chấm; bài chưa đóng băng chỉ trả điểm đã lưu, có rosterState=SAVED_GRADES_ONLY. Không gọi thao tác đóng băng/sinh lịch từ GET trợ lý.

## 2. Endpoint mới — 13 GET

Prefix academic: `/api/v1/academic/assistant-query`.

| Suffix | Quyền | Query riêng |
|---|---|---|
| `/courses` | SA/A/S/T | keyword,level,type,status,minTuition,maxTuition,maxInclusive=true,page,size |
| `/classes` | SA/A/S/T, T lớp chính mình | keyword,courseId,status,recruitingOnly=false,page,size |
| `/students` | SA/A/S; T chỉ roster lớp chính mình | keyword,classId,page,size |
| `/student-classes` | SA/A/S; T chỉ lớp chính mình | studentId bắt buộc,asOf,page,size |
| `/student-statistics` | SA/A/S | metric=TOTAL_PROFILES,asOf |
| `/sessions` | SA/A/S/T, T actual teacher | from/to bắt buộc,classId,teacherId,status,page,size |
| `/attendance` | SA/A/S/T theo quyền buổi actual/primary | sessionId bắt buộc,page,size |
| `/priority-leads` | SA/A/S, S chỉ lead được giao | limit=5, tối đa 20 |
| `/usage-help` | SA/A/S/T | topic bắt buộc, chỉ hai topic cố định đã nêu |

Prefix finance: `/api/v1/finance/assistant-query`.

| Suffix | Quyền | Query riêng |
|---|---|---|
| `/tuitions` | SA/A/S | studentId,classId,overdue,status,page,size |
| `/receipts` | SA/A/S | studentId,tuitionId,classId,page,size |

Prefix teaching: `/api/v1/teaching/assistant-query`.

| Suffix | Quyền | Query riêng |
|---|---|---|
| `/current-grades` | SA/A/T, T lớp chính mình | examId bắt buộc,page,size |
| `/published-grades` | SA/A/S | studentId/enrollmentId bắt buộc,page,size |

Query chung: `scopeMode=ACTIVE_BRANCH` mặc định, `branchId` tùy chọn, `page=0`, `size=5`, tối đa 20. WHOLE_SYSTEM chỉ SA; ALL_ASSIGNED chỉ A; hai chế độ tổng hợp không nhận branchId. Tất cả dùng phiên/role/phân công server hiện hành, không đọc role từ query. SA chưa chọn chi nhánh phải truyền branchId hoặc yêu cầu WHOLE_SYSTEM rõ ràng; không tự chuyển thiếu branch thành toàn hệ thống. T không đổi teacherId sang người khác. A scope rỗng không thành toàn hệ thống; lớp xác thực phiên hiện hành vẫn có thể chặn tài khoản không còn phân công.

Đơn vị tiền VND; min/max không âm, tối đa 13 chữ số phần nguyên và 2 số lẻ; min<=max. “Dưới 5 triệu” dùng maxTuition=5000000&maxInclusive=false. API không tự nhận diện lời nói; C4 sẽ chuyển câu hỏi thành query.

## 3. Hợp đồng và tương thích

Kết quả `{requestId,status,scope,observedAt,source,data,warnings,nextPage,hasMore}`. Danh sách trong data có content/page/size/totalElements/totalPages; grades và attendance bọc danh sách ở data.records. NO_RESULTS nghĩa là truy vấn thành công, tổng 0; thống kê value=0 vẫn là ANSWER. Không có fallback số 0 khi exception. Input lỗi 400, chưa đăng nhập 401, không quyền 403 hoặc bản ghi không tìm thấy trong quyền 404; lỗi downstream giữ xử lý lỗi HTTP hiện hành, chưa phải giao thức hội thoại C4.

Không gửi nội dung khóa dài, ghi chú, liên hệ học viên, nội bộ điểm số, token hoặc audit actor vào projection. Trả link server đã xác định; riêng khóa link tới danh sách lọc theo mã vì trang khóa chưa có deep-link chi tiết ổn định. Link teacher đi tới my-classes/my-schedule, không trang hồ sơ chung. Link published grades đi tới hồ sơ học viên để người có quyền tiếp tục xem tab; chưa thêm UI chat.

Hai mở rộng API cũ:

- GET `/api/v1/academic/courses`: thêm minTuition,maxTuition,maxInclusive=true,allAssigned=false; không đổi mặc định status cũ. Java overload cũ vẫn còn.
- GET `/api/v1/finance/payments`: thêm allAssigned=false; giữ search/list overload cũ. S không được dùng aggregate; mặc định vẫn chi nhánh làm việc.

Không đổi response hợp đồng web/mobile cũ. GET mới nằm dưới prefix gateway đã có; không cần thêm route gateway/module Maven. Source trợ lý không có mutation handler.

## 4. Kiểm thử và giới hạn

Đã đạt 470 test: academic 249, finance 118, teaching 103; gồm 28 test C2 mới (16/6/6), không fail/error/skip. Các service không đổi auth/gateway và web/mobile không chạy lại trong C2. Log local `storage/c2-*-tests.log`, tổng hợp `storage/c2-verification.json`. Test mới `AssistantQueryTest` ở academic/finance/teaching chạy H2 riêng, rollback dữ liệu; các lời gọi AcademicDirectory/RemoteApi trong test teaching được mock. Có kiểm HTTP academic bằng MockMvc (query binding, 401, STUDENT 403, POST 405), không phải server live/MySQL E2E.

Ca bao gồm range học phí, phân trang, trùng tên, scope không phép, teacher roster/substitute, period exclusive, count distinct, lớp tương lai, điểm danh chưa lưu/đã ghi/hủy/tương lai, lead assignee/sort, refund, điểm 0/null và retract. Bộ 50 câu hỏi hội thoại C1 vẫn NOT_RUN; chưa có agent để nghiệm thu tiếng Việt, chống prompt injection hoặc quota Gemini.

Chưa chạy API trên MySQL đang dùng và chưa benchmark trên dữ liệu lớn. SQL mới đã kiểm trên H2 chế độ MySQL; cần nghiệm thu MySQL thật bằng database thử riêng ở C7. Không yêu cầu người dùng reset/migrate database hiện tại để thử C2.

## 5. Chạy và bước tiếp theo

Khởi động lại academic-service, finance-service và teaching-service bằng cách chạy Maven/config môi trường hiện tại; giữ auth-service và gateway hoạt động. Chưa cần khởi động service AI hoặc sửa web. Không dùng GEMINI_API_KEY trong bất kỳ request trình duyệt nào.

Ví dụ tra cứu sau khi có JWT và chi nhánh đúng quyền:

```powershell
$headers = @{ Authorization = 'Bearer <JWT_CUA_BAN>'; 'X-Branch-Id' = '<CHI_NHANH_DUOC_GIAO>' }
Invoke-RestMethod -Headers $headers -Uri 'http://localhost:8080/api/v1/academic/assistant-query/courses?maxTuition=5000000&maxInclusive=false'
Invoke-RestMethod -Headers $headers -Uri 'http://localhost:8080/api/v1/academic/assistant-query/student-statistics?metric=ACTIVE_IN_CLASS'
```

C3 tiếp theo: đăng ký công cụ MCP với schema hữu hạn, chuyển ngữ cảnh đã xác thực sang các GET này và kiểm direct tool calls. Không expose một HTTP client tổng quát hoặc các API ghi hiện có cho model.
