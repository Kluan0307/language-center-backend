# Quy tắc nghiệp vụ mục tiêu — quyết định G01

Ngày chốt đặc tả: **07/11/2026**. Phiên bản đặc tả: **G01-v1**.

**Đây là quyết định/giả định triển khai cho đồ án dựa trên code và đề cương, chưa được xác nhận bằng khảo sát trung tâm thật.** “Chốt” nghĩa là các giai đoạn tiếp theo có thể dùng để code; không có nghĩa người dùng đã duyệt từng chính sách hoặc code hiện tại đã thực hiện chúng. Nếu khảo sát thực tế thay đổi quyết định, ghi phiên bản/lý do và đánh giá ảnh hưởng trước khi đổi code/dữ liệu.

## BR-00 — Ranh giới và nguyên tắc

- Giữ kiến trúc, bốn role, ownership service và thời gian `Asia/Ho_Chi_Minh`. Không tạo portal học viên, thi online, rule engine đa mục đích hoặc thay hệ thống tài chính trong đợt này.
- Chức năng hoàn thành phải có BE/FE/quyền/validation/migration/tài liệu/test liên quan. Tên cấu trúc dữ liệu dưới đây là mô hình mục tiêu, không tuyên bố endpoint hoặc bảng mới đã tồn tại.
- Giữ snapshot nghiệp vụ đã phát sinh; dữ liệu chưa biết là null/chưa xác minh, không điền giả. Các lỗi dùng quy ước hiện tại: 400 validation, 401 chưa đăng nhập, 403 thiếu quyền, 404 ngoài phạm vi/không có theo cơ chế hiện hành, 409 version/trạng thái xung đột, 503 phụ thuộc không khả dụng. Không trả 200 giả khi chưa kiểm tra được.
- Kiểm tra/lưu dùng cùng quy tắc nhưng preview không giữ chỗ. Tại commit phải kiểm tra lại dữ liệu và version cần thiết. Không mở khóa bằng cờ do FE tự gửi.

## BR-01 — Quyền và vòng đời dữ liệu

| Hành vi mục tiêu | SA | Admin | Staff | Teacher |
|---|---|---|---|---|
| Quản trị danh mục chương trình/thang chung, khóa chung | Có | Không | Không | Không |
| Khóa/lớp/chính sách trong chi nhánh | Có | Theo phân công | Giữ quyền học vụ hiện có; không sửa học phí/duyệt ngoại lệ/chốt chính sách mới | Chỉ tài nguyên giảng dạy đã được giao |
| Xem đầy đủ hồ sơ giáo viên phục vụ quản trị | Có | Có giao chi nhánh; xem có giới hạn theo cơ chế Auth | Chỉ danh sách khả dụng tối thiểu | Không có quyền quản trị nhân sự |
| Sửa hồ sơ chung/ngừng làm việc giáo viên | Có | Quản lý **tất cả** phân công đang hoạt động của giáo viên và có ít nhất một phân công | Không | Không; tự sửa email/phone theo luồng hiện có |
| Thu hồi/phân công chi nhánh | Có | Không mở thêm quyền | Không | Không |
| Hồ sơ học viên/đánh giá đầu vào/đề nghị ngoại lệ | Có | Chi nhánh được giao | Chi nhánh được giao | Không mở quyền CRM; nhập điểm lớp theo quyền cũ |
| Xác nhận ngoại lệ đầu vào | Có | Chi nhánh của lớp/học viên | Không | Không |
| Chốt/mở lại kết quả, chốt chính sách đánh giá | Có | Chi nhánh của lớp | Không | Xem kết quả lớp được giao; không chốt/mở lại |
| Đánh dấu đăng ký học xong | Có | Theo chi nhánh | Theo chi nhánh **sau khi đã có kết quả xác nhận hợp lệ** | Không |

Admin không được sửa hồ sơ chung chỉ vì nhìn thấy giáo viên ở một trong nhiều chi nhánh. Giáo viên chưa có phân công chỉ SA quản trị; không dùng phép `containsAll(empty)` để cho mọi Admin sửa. Giá trị primaryBranch chỉ là mặc định lựa chọn, không thay thế kiểm tra toàn bộ phạm vi. Các quyền không nêu giữ theo code hiện có.

Chính sách xóa: học viên/lớp/phòng không có nghiệp vụ và đủ ràng buộc được xóa qua API hiện có; khi thêm assessment/final-result/audit phải mở rộng guard. Giáo viên đã có hồ sơ tài khoản/giảng dạy dùng ngừng làm việc thay xóa vật lý. Khóa có lớp dùng đóng trạng thái. Phiếu thu/correction, điểm/audit, assessment đã xác nhận và final result không bị xóa để sửa lịch sử; dùng sự kiện/phiên bản thay thế. Tài liệu vật lý/revision theo chính sách đã có, không đưa vào web root.

## BR-02 — Hồ sơ giáo viên (G02)

1. ID cho API quản trị mới dùng **teacher profileId**; response trả riêng profileId và accountId. Không dùng hai ID thay thế nhau. API account/membership hiện có vẫn dùng accountId.
2. Nhóm sửa hồ sơ: fullName, email, phone, specialization, qualification, experience, certificate, hireDate. `workStatus`, role, username, password và branchIds không thuộc request sửa hồ sơ. Ngày vào làm là tùy chọn, không tự điền hôm nay khi thiếu.
3. Bắt buộc fullName hợp lệ; email/phone validation theo quy ước Auth và độ dài DB hiện có. Không tự ép các trường chuyên môn/chứng chỉ thành bắt buộc khi đề cương không yêu cầu. Tìm kiếm quản trị giáo viên cần tên đăng nhập hoặc họ tên; tên/số điện thoại không là định danh duy nhất.
4. Teacher có optimistic version bắt đầu 0 cho dữ liệu cũ; PUT yêu cầu version đã đọc, trả version mới, stale trả 409. Tự cập nhật email/phone qua `/me/profile` phải tham gia version/audit của Teacher để không thành đường bỏ qua.
5. Mọi sửa hồ sơ ghi tác giả, thời điểm và thay đổi trước/sau của trường nghiệp vụ; không ghi password/token. Thay đổi tên không viết lại tên snapshot trên phiếu/điểm/buổi cũ. Màn chọn phân công mới đọc tên hiện tại.
6. G02 bổ sung detail API quản trị có thể xem giáo viên inactive; không tái sử dụng endpoint chọn giáo viên khả dụng theo cách làm mất hồ sơ ngừng làm việc. Giữ nguyên endpoint khả dụng để Academic kiểm tra.

## BR-03 — Khả dụng, ngừng làm việc và bàn giao (G03)

- `Teacher.workStatus`: dùng ACTIVE/INACTIVE, tách `Account.status`. Giáo viên khả dụng cho phân công mới khi account bật, workStatus ACTIVE, có phân công chi nhánh phù hợp và không xung đột lịch. Giá trị cũ khác hai trạng thái cần đối soát; không tự đổi WORKING thành ACTIVE trong G01.
- ACTIVE → INACTIVE chỉ khi người sửa đủ quyền toàn bộ phân công và không còn trách nhiệm chưa bàn giao. INACTIVE → ACTIVE kiểm tra version/quyền; không tự mở account đã khóa, không tự khôi phục chi nhánh đã thu hồi.
- Khóa tài khoản vì bảo mật vẫn được thực hiện theo quyền hiện có, thu hồi session; không cần chờ bàn giao. UI phải cho quản lý thấy còn việc cần xử lý; tránh tạo phân công mới cho account khóa.
- Trách nhiệm cần xử lý gồm lớp đang/sắp học do giáo viên phụ trách (kể cả chưa sinh lịch), mọi buổi SCHEDULED được giao thực tế/dạy thay/học bù tại phạm vi bị tác động, kể cả buổi quá ngày nhưng chưa xác nhận xử lý. Không tự coi buổi đã qua là đã dạy. Lớp hết ngày nhưng còn buổi chưa xử lý vẫn chặn.
- Bàn giao từng lớp/buổi qua chức năng Academic theo quyền; xác nhận người thay khả dụng, phòng, thời gian và lịch. Không tự thay toàn bộ mà chưa xem danh sách/kiểm tra. Giữ planned/actual/snapshot và lịch sử; không đổi ngược giáo viên của buổi COMPLETED.
- Thu hồi một chi nhánh chỉ kiểm tra trách nhiệm thuộc chi nhánh đó; ngừng làm việc kiểm tra toàn bộ chi nhánh của giáo viên. Admin thiếu phạm vi không được xem/sửa trách nhiệm ngoài quyền để hoàn tất một thao tác global.
- Tận dụng khóa account giáo viên đang có trong `TeacherDirectory.lock()` và `AuthService.assignBranches`: mọi đường phân công phải phối hợp cùng khóa và kiểm tra trạng thái **sau khi giữ khóa**. Auth đổi workStatus cũng phải tham gia khóa đó. G03 rà thứ tự khóa hiện có, timeout và test race; không chỉ kiểm tra HTTP hai lần rồi coi là nguyên tử. Đây là ngoại lệ phối hợp khóa hẹp của database dùng chung, không cho phép join dữ liệu liên service tùy ý.
- Academic/Auth không khả dụng hoặc danh sách trách nhiệm chưa kiểm tra xong: không chuyển INACTIVE/thu hồi. Nếu phân công thắng trước thì ngừng bị chặn; nếu ngừng thắng trước thì phân công bị chặn. Không được có kết quả cả hai cùng thành công để lại trách nhiệm cho người inactive.

## BR-04 — Chương trình, thang và đầu vào khóa (G04)

1. Mô hình tối thiểu: **Program**, **ProgramLevel**, **CourseAdmissionPolicy**. ProgramLevel có mã/tên và thứ tự để hiển thị trong một chương trình; không so sánh chuỗi, không suy ra quy đổi liên chương trình. Có thể cấu hình chương trình giao tiếp, IELTS, TOEIC; đây là ví dụ dữ liệu, không tự tạo chuẩn quy đổi chứng chỉ.
2. Danh mục chương trình/thang chung do SA quản trị; A dùng các mức đã xác định cho khóa thuộc chi nhánh. Không thêm một chức năng đồng bộ chứng chỉ bên ngoài.
3. Course phân biệt chương trình, danh sách mức đầu vào được chấp nhận và mục tiêu đầu ra. Tối thiểu dùng **tập levelId được chấp nhận**; không mặc định cứ rank cao hơn là phù hợp.
4. Policy có ba mức: `UNVERIFIED` (dữ liệu cũ/chưa đối soát), `OPEN_ENTRY` (quản lý xác nhận không cần đánh giá đầu vào), `REQUIRES_ASSESSMENT` (cần kết quả chương trình/mức được chấp nhận). OPEN_ENTRY cho phép thiếu assessment vì đó là điều kiện đầu vào đã xác nhận, không phải approval ngầm. Thiếu assessment với hai policy còn lại phải bổ sung hoặc duyệt ngoại lệ.
5. Mục tiêu IELTS 6.5 là đầu ra. Điều kiện đầu vào do policy ghi riêng; không lấy 6.5 làm yêu cầu đầu vào tự động.
6. Khóa mới phải có policy đã xác nhận để dùng luồng ghi danh bình thường. Khóa/lớp cũ chưa đối soát vẫn đọc được và được gắn UNVERIFIED; không biến text hiện có thành mức đã xác nhận.
7. Mỗi lớp giữ snapshot/version điều kiện đầu vào khi mở lớp. Sửa course không tự đổi lớp đã mở. Quản lý chỉ cập nhật điều kiện của lớp qua thao tác rõ ràng có version/lý do; không hồi tố đăng ký đã có, nhưng preview/approval chưa dùng phải kiểm tra lại.

## BR-05 — Đánh giá đầu vào (G05)

- Bản ghi thuộc student/branch/program, gồm phương thức TEST/INTERVIEW/CERTIFICATE/OTHER, kết quả levelId, ngày đánh giá, nguồn/người đánh giá và người ghi nhận. Người đánh giá có thể là tên/nguồn bên ngoài; không bắt buộc là một teacher account nội bộ.
- Tên học viên/điện thoại không dùng làm khóa; levelId phải cùng Program. TEST có điểm và maxScore >0, 0<=score<=maxScore; phương thức không dùng điểm cho phép null cả cặp. Ngày đánh giá không ở tương lai. Không tự quy đổi điểm thành level ngoài cấu hình được xác nhận.
- Nhân viên ghi nhận kết quả từ nguồn có thể giải thích; lưu bằng chứng mô tả/tham chiếu, không bắt buộc xây upload chứng chỉ mới. Không bịa người/ngày đánh giá từ currentLevel cũ.
- Một học viên có tối đa một kết quả **hiện hành đã xác nhận** trong mỗi chương trình. Dùng DRAFT/CONFIRMED/SUPERSEDED/REVOKED hoặc trạng thái tương đương giữ đúng invariant. SA/A/S trong chi nhánh có thể ghi nhận/xác nhận nguồn theo quyền học vụ hiện tại; thao tác xác nhận nguồn khác với quyền duyệt ngoại lệ của quản lý.
- Sửa kết quả CONFIRMED bằng bản thay thế có lý do; không update mất bản cũ. Thu hồi để lại lịch sử. Version/student-program lock bảo đảm hai thao tác không sinh hai kết quả hiện hành. Không có thời hạn hết hiệu lực theo số ngày ở bản cơ bản; chỉ kết quả bị thay thế/thu hồi mất hiệu lực.
- `Student.currentLevel` cũ chỉ là nhãn kế thừa. Nó không còn là nguồn quyết định ghi danh; FE hiển thị rõ nguồn hiện hành theo chương trình. Không tự sửa chuỗi đó thành chứng nhận hoặc trộn các chương trình.

## BR-06 — Phù hợp, preview và lưu ghi danh (G06)

| Trạng thái học thuật | Quy tắc |
|---|---|
| ELIGIBLE | OPEN_ENTRY đã xác nhận, hoặc đánh giá CONFIRMED hiện hành cùng chương trình và level thuộc tập chấp nhận |
| MISSING_ASSESSMENT | Policy cần đánh giá nhưng không có kết quả hiện hành |
| PROGRAM_MISMATCH / LEVEL_MISMATCH | Có nguồn được chọn nhưng sai chương trình hoặc mức không thuộc tập chấp nhận |
| UNVERIFIED_POLICY | Điều kiện lớp chưa được xác minh |

Các trạng thái cần ngoại lệ phải có lý do có cấu trúc. Ràng buộc **không thể vượt** là role/phạm vi, lớp/khóa/chi nhánh khả dụng, trạng thái học viên, lớp đầy, đăng ký trùng và trùng lịch. FE phân biệt lỗi học thuật với các lỗi này; gợi ý không lọc mất nguyên nhân chặn.

Khi lưu, kiểm tra lại policy/assessment version và các ràng buộc hiện tại dưới khóa/transaction phù hợp; lưu snapshot căn cứ vào Enrollment hoặc bản ghi admission gắn Enrollment. Không tạo tuition trước khi ghi danh được chấp nhận; giữ cơ chế phát sự kiện/tuition retry và billing root hiện có.

Chuyển cùng khóa/chi nhánh: xét policy của **lớp đích**, giữ billing gốc. Học lại cùng Enrollment/lớp: dùng căn cứ đã chấp nhận nếu policy lớp không đổi; không bắt thi lại chỉ vì level hiện tại đã cao hơn. Policy đã đổi thì xét lại/approval. Đăng ký kế thừa không có snapshot vẫn giữ là LEGACY_UNVERIFIED, không bị hủy/đổi học phí tự động; nếu tiếp tục hoặc chuyển khi chưa đủ căn cứ thì cần xác nhận quản lý có lý do. Không mở chuyển khác khóa/chi nhánh trong đợt này.

## BR-07 — Ngoại lệ đầu vào (G07)

- SA/A trong phạm vi lớp có thể duyệt yêu cầu do S/A tạo. Đồ án không bắt buộc hai quản lý khác nhau; Admin có thể duyệt đề nghị do mình tạo nhưng vẫn ghi actor/lý do. Staff/Teacher không được tự duyệt.
- State tối thiểu: REQUESTED → APPROVED/REJECTED; APPROVED → CONSUMED hoặc INVALIDATED. Không sửa lý do/đối tượng của yêu cầu đã quyết định; tạo đề nghị mới nếu cần.
- Gắn studentId, classId, branchId, assessment hiện hành hoặc “không có”, policyVersion, admission-context fingerprint và lý do (3–500 ký tự). APPROVED không tự tạo ghi danh, không giữ chỗ, không có lịch hết hạn tùy tiện ở bản cơ bản.
- Assessment/policy/phạm vi/đối tượng đổi thì không dùng approval cũ. Một approval chỉ chấp nhận một Enrollment theo intent; consume và ghi danh cùng transaction Academic. Retry cùng intent trả lại đăng ký đã tạo; không dùng approval cho lớp/người khác hoặc nhiều lần.
- Khi request mất phản hồi, FE tra cứu Enrollment/approval trước khi cho gửi lại. G06 trước khi G07 hoàn tất chỉ chặn ngoại lệ với thông báo rõ, không thêm cờ bỏ kiểm tra tạm.

## BR-08 — Thời lượng (G08–G09)

- Lưu **số phút nguyên dương**, aggregate bằng kiểu đủ lớn; UI hiển thị giờ/phút hoặc số phút có đơn vị. Khóa giữ thời lượng dự kiến; lớp snapshot thời lượng tại khi mở hoặc khi quản lý xác minh/chỉnh kế hoạch có version/lý do.
- `Course.duration` cũ chưa rõ đơn vị được giữ nguyên như legacy; expectedMinutes mới null/UNVERIFIED đến khi quản lý đối chiếu. Không nhân 24 thành 24 giờ, không đặt tổng lịch hiện có thành mục tiêu khóa tự động. Chưa xác minh vẫn xem/tổ chức học được nhưng không xác nhận hoàn thành cuối khóa mới.
- Số phút buổi = end-start khi giờ hợp lệ cùng ngày. Không mở buổi qua đêm hoặc đa ca/lớp/ngày trong đợt này.
- **Đã xếp** = tổng SCHEDULED + COMPLETED có giờ đã biết; **đã học** = tổng COMPLETED có giờ đã biết; CANCELLED không cộng. MAKEUP được tính đúng một buổi riêng, nguồn đã hủy không cộng. Không cộng thời lượng thiếu giờ thành 0 rồi báo đủ; trả kèm số buổi thiếu dữ liệu và trạng thái UNKNOWN.
- Điểm danh đã xác nhận đang là nguồn chuyển COMPLETED của session; G09 tận dụng nguồn này. Ngày đã qua không tự là COMPLETED. Sửa giờ/trạng thái sau hoàn tất cần quyền quản lý/audit và xử lý ảnh hưởng kết quả đã chốt.
- Thời lượng chênh lệch kế hoạch hiển thị cảnh báo. Chốt cuối khóa/đóng lớp mới yêu cầu expectedMinutes đã xác minh, taughtMinutes >= expectedMinutes và không còn SCHEDULED chưa xử lý. Nếu thiếu giờ phải học bù hoặc điều chỉnh kế hoạch có lý do, không chỉ bỏ cảnh báo.

## BR-09 — Quy tắc đánh giá và điểm tạm (G10)

1. Policy đánh giá theo lớp, có version, ngưỡng phần trăm (0–100), danh sách bài áp dụng/bắt buộc và tùy chọn chuyên cần. Quản lý xác nhận cấu hình; Teacher tạo bài/nhập điểm theo quyền cũ nhưng không tự nới ngưỡng hoặc bỏ bài bắt buộc của policy đã xác nhận.
2. Giữ `maxScore>0`, `weight>0`, `0<=score<=maxScore`. Điểm 0 là đã chấm; null/chưa có bản ghi là chưa chấm. Một bài thuộc policy có thể chưa đến ngày thi: phải hiển thị chưa đủ điều kiện, không tự bỏ khỏi danh sách bắt buộc.
3. Điểm tạm = `sum((score/maxScore)*100*weight)/sum(weight)` trên các bài áp dụng đã chấm. Tính BigDecimal độ chính xác phù hợp (ít nhất 10 chữ số thập phân trung gian), so sánh ngưỡng trước làm tròn 2 chữ số chỉ để hiển thị. Không lấy trung bình các phần trăm đã làm tròn.
4. Điểm cuối yêu cầu policy đã xác nhận, có ít nhất một bài áp dụng có điểm/trọng số, đủ mọi bài bắt buộc, không còn nguồn điểm chưa hợp lệ. Bài tùy chọn chưa chấm không mặc định 0; mẫu số chỉ các bài đã chấm được áp dụng. Bài không áp dụng phải được xác định theo roster và policy, không dựa solely vào STUDYING hôm nay.
5. Ngưỡng 50% hiện có chỉ là gợi ý cấu hình ban đầu cho đồ án, **quản lý phải xác nhận theo lớp** trước xét cuối khóa. Không tự công nhận bài cũ là bắt buộc/không bắt buộc. Policy cũ chưa chốt → UNCONFIGURED.
6. Nếu không yêu cầu chuyên cần: `minAttendancePercent=null`, ghi NOT_REQUIRED. Nếu bật: lấy các session COMPLETED thuộc khoảng học có hiệu lực; PRESENT/LATE được tính tham dự, ABSENT (kể cả hasPermission=true) vẫn là vắng; nghỉ có phép được hiển thị riêng, không tự xóa khỏi mẫu số. Thiếu bản ghi/giờ/ngữ cảnh cần thiết hoặc mẫu số 0 → UNKNOWN, không giả định 100%. Ngưỡng do quản lý cấu hình; không tự gán chuẩn 80%.
7. Thiếu dữ liệu → INSUFFICIENT, không gọi FAIL. Đã đủ dữ liệu nhưng điểm/chuyên cần thấp hơn ngưỡng → FAIL. Đủ và đạt mọi điều kiện → PASS. Kết quả preview chưa phải CONFIRMED cuối khóa.

## BR-10 — Chốt kết quả, COMPLETED và FINISHED (G11)

### Kết quả thuộc Teaching

- Kết quả cuối gắn **Enrollment** (kèm studentId/classId/branchId), không chỉ studentId/classId vì có thể có đăng ký lịch sử khác nhau. Snapshot chứa policy/version, tập điểm/roster, chuyên cần nếu bật, thời lượng và các căn cứ đã dùng; có actor, thời gian, lý do/version.
- Quản lý xem preview rồi xác nhận PASS/FAIL khi đủ điều kiện BR-08/09. Teacher/Staff không chốt. Chưa đủ dữ liệu không được ép thành một kết quả cuối.
- Tối đa một kết quả hiện hành cho một Enrollment. Hai người xác nhận cùng version chỉ một intent thành công; retry cùng intent nhận kết quả đã tạo, khác nội dung trả xung đột. Không dùng FE preview làm bảo đảm nguồn chưa thay đổi.
- Policy/điểm/điểm danh/buổi ảnh hưởng kết quả đã CONFIRMED phải bị chặn sửa hoặc yêu cầu quản lý mở lại phạm vi kết quả bị ảnh hưởng có lý do. Mở lại giữ nguyên snapshot cũ, ghi REOPENED và kết quả hiện hành là UNDER_REVIEW; chỉnh nguồn rồi chốt revision mới. Không xóa snapshot cũ hoặc tiếp tục báo nó là kết quả hiện hành sau mở lại.
- Các đường sửa nguồn và chốt phải có cơ chế phối hợp khóa/version và test race trên kiến trúc dùng chung DB hiện tại. G11 cần thiết kế kỹ thuật cụ thể, ghi ngoại lệ khóa hẹp nếu cần; kiểm tra HTTP trước và sau không tự tạo transaction phân tán. Invariant bắt buộc: không có CONFIRMED hiện hành vừa được tạo trên dữ liệu bị thay đổi không kiểm soát.

### Trạng thái Academic độc lập và đồng bộ tường minh

- Enrollment.COMPLETED nghĩa đã hoàn tất quá trình học, **không có nghĩa đạt**. Cho phép hoàn tất sau kết quả CONFIRMED PASS **hoặc FAIL**, đủ thời lượng và các điều kiện học vụ; không dùng công nợ để kết luận học thuật.
- Xác nhận kết quả Teaching và chuyển COMPLETED Academic là hai thao tác có liên kết kiểm tra, không cập nhật distributed transaction ngầm. SA/A/S có quyền học vụ chỉ chuyển COMPLETED khi tham chiếu kết quả hiện hành hợp lệ và version/căn cứ còn khớp. Teaching thành công nhưng Academic thất bại phải hiển thị “đã chốt kết quả, chưa hoàn tất đăng ký”, cho retry kiểm tra rõ; không hoàn thu/thu phí lại.
- Enrollment đã COMPLETED/DROPPED/TRANSFERRED tiếp tục là terminal theo hiện trạng; mở lại kết quả chỉ để sửa học thuật, không tự mở enrollment/period hoặc tạo tuition. Nếu muốn học tiếp dùng nghiệp vụ học vụ phù hợp đã có, không tái sử dụng thao tác mở lại điểm.
- FINISHED theo `dateStatus()` chỉ cho biết lịch lớp đã qua ngày kết thúc; nó **không chứng minh** đã đóng học vụ/chốt kết quả. G11 phân biệt hiển thị “hết lịch” với hành động đóng lớp có người xác nhận. Chặn endpoint đóng thủ công nếu còn SCHEDULED chưa xử lý, giờ chưa xác minh/thiếu hoặc Enrollment STUDYING/RESERVED chưa được xử lý; các Enrollment học xong phải có kết quả xác nhận hiện hành.
- Giữ các lớp/đăng ký cũ đã terminal dưới nhãn LEGACY_UNVERIFIED nếu thiếu chứng cứ chốt; không tự sinh kết quả PASS/FAIL hay người chốt từ trạng thái cũ. Mở lại kết quả cũ chỉ thực hiện sau khi đối soát đủ nguồn; không tự mở lớp đang khóa vì dữ liệu thiếu.

## BR-11 — Tài chính, thống kê và trải nghiệm giữ ổn định

- Không làm lại tài chính: giữ snapshot giá/billing root, transaction/idempotency, phiếu bất biến và journal correction. Ghi danh bị chặn đầu vào không tạo phí mới. Học lại/chuyển cùng khóa không tạo tuition thứ hai.
- Báo cáo thu ròng = thu gốc - đảo - hoàn, theo ngày nghiệp vụ của từng giao dịch; công nợ hiện tại ghi đúng là hiện tại. Không đổi tên collected thành doanh thu kế toán được ghi nhận theo dồn tích. Chuyển khoản hiện là ghi nhận thủ công.
- Số học viên distinct khác số đăng ký/chỗ lớp; tỷ lệ tổng không lấy trung bình tỷ lệ các chi nhánh. Đạt tạm khác đạt cuối, UNDER_REVIEW không tính vào đã đạt cuối hiện hành.
- Hồ sơ học tập tổng hợp có quyền của học viên/chi nhánh; Teacher không được mở toàn bộ học phí/CRM chỉ vì thấy học viên trong lớp. Dùng route thật từ App/navigation, không lắp module mock cũ.
- FE hiển thị rõ UNKNOWN/UNVERIFIED/UNDER_REVIEW, lỗi service và cách retry; không thay lỗi bằng số 0 hay trạng thái đã hoàn thành. Đổi chi nhánh phải bỏ kết quả request cũ, mọi update quan trọng có stale-version handling.

## BR-12 — Ảnh hưởng triển khai và migration

| Giai đoạn | Dữ liệu mục tiêu | Hợp đồng/giao diện chịu ảnh hưởng | Cần kiểm chứng |
|---|---|---|---|
| G02 | Teacher version + audit hồ sơ | Auth quản trị detail/update, `/me/profile` tham gia version; FE AccountManagement | Quyền nhiều chi nhánh, stale/self-update, lịch sử |
| G03 | Audit trạng thái, dùng chung khóa account hiện có | Auth trạng thái/đầu mục bàn giao; mọi đường phân công Academic | Race/phụ thuộc/dạy thay/học bù |
| G04 | Program/ProgramLevel, policy version/snapshot lớp | Course/Class DTO và UI; danh mục SA | Legacy, OPEN_ENTRY, không hồi tố |
| G05 | Assessment có lịch sử/hiện hành | Student detail + đánh giá, không mở CRM cho T | Mỗi student/program một hiện hành |
| G06–G07 | Căn cứ admission + approval consume | Enroll/resume/transfer preview/save, FE ghi danh/duyệt | Quota/lịch, approval cũ, idempotency |
| G08–G09 | ExpectedMinutes/verification, snapshot lớp | Course/Class/session aggregates | Giờ thiếu, hủy/bù, sau hoàn tất |
| G10–G11 | Policy đánh giá, final result revision/audit/ref | Teaching grading/result; Academic completion/close; FE | Policy snapshot, roster, race, phục hồi |
| G12 | Projection theo quyền, không sao chép truth tùy ý | Hồ sơ tổng hợp/deep link/báo cáo | Scope/filter/unknown/mobile |

Đây là tác động thiết kế, **chưa phải migration/API đã được tạo**. G02 kiểm tra số migration mới nhất, tại G01 mới thấy V001–V006; dự kiến V007 nhưng không giữ chỗ số nếu có thay đổi khác trước đó. Chỉ migration bổ sung, kiểm tra trên bản sao và bảo toàn dữ liệu gốc. Mọi quy tắc trên phải có case kiểm thử; các case trong traceability là dự kiến cho các giai đoạn cài đặt/nghiệm thu.

## Tình trạng triển khai sau G02–G03

Ngày 07/11/2026, BR-01/02/03 phần Teacher profile/lifecycle và kiểm tra phân công đã được cài đặt/kiểm thử. Các nhóm chương trình/assessment/admission/duration/final-result còn là đặc tả cho G04–G12. Quy tắc vẫn mang nhãn giả định G01-v1, chưa biến thành khảo sát thực tế. [Bằng chứng G03](../thesis-progress/G03.md).

## Trạng thái triển khai BR-04/BR-05 — 08/11/2026

G04 hoàn thiện danh mục và policy revision/snapshot lớp; G05 hoàn thiện đánh giá theo chương trình, source/assessor/date, lifecycle và lịch sử. Không thay đổi giả định G01-v1 thành khảo sát thật. Không tự chuyển currentLevel, quy đổi thang hay suy ra điểm. Các quy tắc eligibility/ngoại lệ G06/G07 chưa enforce. Chi tiết hợp đồng/kiểm chứng: [G04](../thesis-progress/G04.md), [G05](../thesis-progress/G05.md).

## G06/G07 đã triển khai — 08/11/2026

BR-06/BR-07 và UC09/UC10 đã có preview/gợi ý, kiểm tra lại khi lưu, snapshot và ngoại lệ theo role/chi nhánh, gồm transfer/resume/retry. 263 BE tests và 32 kiểm tra runtime được mô tả tại [G06](../thesis-progress/G06.md), [G07](../thesis-progress/G07.md). Các TC dự kiến ở G01 vẫn cần truy vết cụ thể, không tự gán toàn bộ đã đạt. T3 (G04–G07) hoàn thành phần triển khai; G08–G20 còn lại, không tuyên bố toàn đồ án hoàn tất.

## G08/G09 triển khai — 08/11/2026

BR-08 đã có kế hoạch phút/version/snapshot, tổng session thật, unknown và gate thời lượng đóng lớp. [G08](../thesis-progress/G08.md), [G09](../thesis-progress/G09.md) ghi26tests mới, 33runtimechecks và giới hạn. UC06/UC07/UC11 có bằng chứng liên quan, không tự đánh dấu toàn bộ TC G01 đã đạt. T4 còn G10–G12; kết quả cuối khóa và sửa nguồn ảnh hưởng kết quả thuộc G10/G11.

## G10 triển khai — 08/11/2026

BR-09/UC12 có policy lớp phiên bản, điểm chuẩn hóa trước rounding, required/optional/applicability, chuyên cần và trạng thái provisional. [G10](../thesis-progress/G10.md) có34tests mới và 30runtimechecks. Không đánh dấu các TC G01 dự kiến khác đã đạt; BR-10/final results còn G11 và hồ sơ tổng hợp G12. T4 chưa hoàn tất.

## BR-10 — Kết quả chính thức, đồng bộ và mở lại (G11/G12)

- Kết quả thuộc Enrollment, không chỉ Student/Class. PASS/FAIL chỉ chốt khi row có đủ căn cứ và đủ phút xác minh, không buổi chưa xử lý; STUDYING hoặc COMPLETED cần xác minh lại. RESERVED/DROPPED/TRANSFERRED không tự được coi hoàn tất toàn khóa.
- Một current CONFIRMED mỗi enrollment. UUID intent, fingerprint, phiên bản và lý do chống chốt từ preview cũ hoặc gửi lặp; unique enrollment/revision và generated unique current key ở MySQL. Snapshot bất biến; mở lại chỉ thêm trạng thái/actor/time/reason, không sửa snapshot cũ.
- Có một current bất kỳ là khóa các nguồn toàn lớp: policy/exam/grade, attendance/session/plan và membership/period. Mở lại toàn lớp là thao tác SA/A, cần phiên bản và lý do; mở lại không hoàn tác học phí hoặc enrollment terminal.
- Chốt Teaching và COMPLETED Academic là hai bước. Academic kiểm current refs, shared barrier token/sourceRevision và đủ phút dưới khóa; dependency không đọc được thì503 và rollback. Hoàn tất đóng khoảng học đến hết hôm nay là chuyển tiếp kết thúc bảo toàn nguồn đã chốt, không tăng sourceRevision; không còn buổi chưa xử lý, không bỏ điểm/attendance đã dùng. Xác nhận lại trên enrollment COMPLETED cập nhật refs và thêm COMPLETION_RESULT_REFRESHED, không mở khoảng học.
- Đóng lớp riêng có audit/revision/intent, không còn STUDYING/RESERVED/SCHEDULED, mọi COMPLETED đã liên kết đúng current. Hết ngày học chỉ là dateStatus, không chứng minh đóng lớp. Metadata closure lưu epoch nguồn/kết quả; thay bằng chứng sau mở lại báo NEEDS_REVIEW. Kết quả trên lớp đóng phải mở lớp trước khi chốt mới. Legacy terminal thiếu bằng chứng giữ LEGACY_UNVERIFIED.
- Phối hợp hẹp qua class_result_barriers trong cùng MySQL schema: Academic tăng sourceRevision khi ghi nguồn; Teaching khóa barrier rồi evaluation state/exam và tăng teaching sourceRevision. Không join bảng nghiệp vụ chéo service. Final giữ barrier và chỉ gọi HTTP Academic đọc MVCC; Academic completion/closure giữ khóa và chỉ gọi Teaching summary đọc MVCC. Không gọi HTTP mutating khi giữ barrier. Token khác database thì chặn chốt; đây là ràng buộc triển khai hiện tại, không phải mô hình các service có database độc lập.
- Staff chỉ xem summary để hoàn thành; không policy/điểm/snapshot. Teacher đọc kết quả/lịch sử của lớp chính, không quản lý CRM toàn học viên hoặc chốt/mở lại. Hồ sơ tổng hợp không đổi currentLevel theo kết quả một cách ngầm định.


## Đặc tả as-built G13/G14 — 08/11/2026

Các dòng kế hoạch trước G12 ở trên giữ ý nghĩa lịch sử. Bản hiện tại có UC13/G11 kết quả và UC03/G12 hồ sơ tổng hợp. [Use Case](use-cases.md), [quyền theo thao tác](role-permissions.md), [vòng đời dữ liệu](design/lifecycles-and-invariants.md) và [schema](design/schema-verification.md) mô tả code hiện hành. BR-10 gốc và phần bổ sung G11/G12 cùng phân biệt state REOPENED lưu trong FinalResult với nhãn đang xem xét lại trên UI; không có thêm enum UNDER_REVIEW trong bảng. Chính sách vẫn là giả định G01-v1, chưa trở thành khảo sát thực tế.
