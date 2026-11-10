# Use Case và đặc tả nghiệp vụ — G13

Ngày 08/11/2026; đối chiếu source G01–G12. SA=SUPER_ADMIN, A=ADMIN, S=STAFF, T=TEACHER. Đây là đặc tả phần mềm đã triển khai theo giả định G01-v1; không phải kết quả khảo sát trung tâm.

## Quy ước

Mọi tác vụ ngoài đăng nhập cần phiên hợp lệ, quyền ở cả controller/service và phạm vi hiện tại. Vai trò không kế thừa nhau trong các sơ đồ; liên kết một actor vào nhóm UC không có nghĩa được mọi thao tác trong nhóm. Tiền/hậu điều kiện và nhánh quyền bên dưới là phần bắt buộc. Không dùng include đăng nhập vì phiên hợp lệ là tiền điều kiện, không phải đăng nhập lại cho từng thao tác.

UC01–UC17 giữ mã G01; UC18 bổ sung lead đã có trong code. UC17 là vận hành CLI ngoài RBAC; RQ11/RQ12 còn sản phẩm tài liệu và nghiệm thu, không bịa thành API/màn hình của người dùng.

[Sơ đồ và chú giải](diagrams/README.md) · [Ma trận quyền](role-permissions.md) · [Quy trình](processes.md) · [Khảo sát](survey.md) · [Truy vết](traceability.md).

## Danh mục

| Mã | Use case | Actor / giới hạn | RQ |
|---|---|---|---|
| [UC01](#uc01) | Đăng nhập, bảo mật phiên và liên hệ cá nhân | SA/A/S/T | RQ10 |
| [UC02](#uc02) | Quản lý tài khoản, chi nhánh và phân công | SA; A trong quyền quản lý tài khoản | RQ02, RQ10 |
| [UC03](#uc03) | Quản lý hồ sơ và lịch sử học viên | SA/A/S | RQ01, RQ09, RQ10 |
| [UC04](#uc04) | Quản trị hồ sơ giáo viên | SA/A đủ phạm vi; S chỉ tra cứu khả dụng | RQ02, RQ10 |
| [UC05](#uc05) | Bàn giao và ngừng làm việc giáo viên | SA/A đủ phạm vi | RQ02, RQ06, RQ10 |
| [UC06](#uc06) | Quản lý chương trình, khóa, đầu vào và thời lượng | SA/A/S/T tùy thao tác | RQ03, RQ05, RQ07 |
| [UC07](#uc07) | Quản lý phòng, mở lớp và lịch mẫu | SA/A; S trong giới hạn | RQ04, RQ06, RQ10 |
| [UC08](#uc08) | Ghi nhận và xác nhận đánh giá đầu vào | SA/A/S | RQ01, RQ05 |
| [UC09](#uc09) | Gợi ý lớp, ghi danh, bảo lưu, học lại và chuyển lớp | SA/A/S | RQ01, RQ04, RQ05, RQ08 |
| [UC10](#uc10) | Đề nghị và duyệt ngoại lệ đầu vào | SA/A/S đề nghị; SA/A quyết định | RQ04, RQ05, RQ10 |
| [UC11](#uc11) | Điều phối buổi, nghỉ/bù/dạy thay và điểm danh | SA/A điều phối; SA/A/S/T điểm danh theo điều kiện | RQ06, RQ07, RQ10 |
| [UC12](#uc12) | Bài kiểm tra, điểm, quy tắc và đánh giá tạm | SA/A/T theo lớp; Staff không quản lý điểm | RQ07, RQ09, RQ10 |
| [UC13](#uc13) | Chốt/mở lại kết quả, hoàn thành học vụ và đóng lớp | SA/A chốt/mở/đóng lớp; S đồng bộ hoàn thành; T đọc lớp chính | RQ04, RQ07, RQ10 |
| [UC14](#uc14) | Học phí, thu tiền, điều chỉnh, đảo và hoàn | SA/A/S xem/thu; SA/A điều chỉnh/đảo/hoàn | RQ08, RQ09, RQ10 |
| [UC15](#uc15) | Tìm kiếm, dashboard và báo cáo | SA/A tổng hợp; S/T đọc nguồn theo quyền | RQ01, RQ07, RQ09, RQ10 |
| [UC16](#uc16) | Tài liệu giảng dạy, duyệt và phiên bản | SA/A quản lý/duyệt; T sở hữu; S chỉ bản được duyệt | RQ07, RQ10 |
| [UC17](#uc17) | Sao lưu, diễn tập phục hồi và bàn giao | Người vận hành được ủy quyền; ngoài bốn role RBAC | RQ11, RQ12 |
| [UC18](#uc18) | Tiếp nhận và chăm sóc khách hàng tiềm năng | SA/A/S; S chỉ lead được giao | RQ01, RQ09, RQ10 |

<a id="uc01"></a>

## UC01 — Đăng nhập, bảo mật phiên và liên hệ cá nhân

**Actor:** SA/A/S/T. **Yêu cầu:** RQ10.

**Kích hoạt:** người có quyền chọn thao tác của nhóm use case, cung cấp định danh/bộ lọc hoặc dữ liệu nghiệp vụ cần thiết.

**Tiền điều kiện:** Tài khoản hoạt động; role/phân công hợp lệ; đăng nhập chưa cần JWT.

**Luồng chính và các nhánh nghiệp vụ độc lập:**

1. Nhập username/password; Auth kiểm BCrypt và trạng thái hiện tại.
2. Nhận JWT, tải /auth/me và phạm vi làm việc; đổi mật khẩu bắt buộc nếu được yêu cầu.
3. Đổi mật khẩu hoặc đăng xuất mọi thiết bị làm tăng sessionVersion; tự sửa liên hệ qua /me/profile.

Các thao tác tạo/sửa/xóa/ngừng/đọc là biến thể độc lập; luồng nhóm không ép thực hiện hết hoặc tự động chạy bước tiếp theo.

**Ngoại lệ và quyền chi tiết:**

- Sai thông tin/phiên bị thu hồi:401; thiếu quyền hoặc bắt buộc đổi mật khẩu:403.
- Mật khẩu không hợp lệ bị validation; không ghi mật khẩu/JWT vào báo cáo hoặc URL.
- Teacher tự sửa liên hệ còn tăng Teacher.version/audit để quản lý không ghi đè bản cũ.

**Hậu điều kiện thành công:** Đăng nhập hợp lệ hoặc phiên đã bị thu hồi; không tự đổi role/phân công.

**Bảo đảm khi lỗi:** transaction local chưa commit rollback; dữ liệu đã commit ở service khác không tự rollback. Hiển thị lỗi, giữ lý do nhập khi có thể và tải lại/tra intent theo hợp đồng trước retry. Không áp dụng cơ chế UUID cho use case mà API không có requestKey/requestId. 401/403/404/409/503 theo controller/service; 404 có thể che đối tượng ngoài phạm vi.

**Bằng chứng source:** [AuthService.java](../../auth-service/src/main/java/com/example/auth/service/AuthService.java); [API](../api-list.md); [quy tắc](business-rules.md). Các bài test cụ thể và mức kiểm chứng theo bàn giao G02–G12, không coi TC dự kiến G01 là đã chạy.

<a id="uc02"></a>

## UC02 — Quản lý tài khoản, chi nhánh và phân công

**Actor:** SA; A trong quyền quản lý tài khoản. **Yêu cầu:** RQ02, RQ10.

**Kích hoạt:** người có quyền chọn thao tác của nhóm use case, cung cấp định danh/bộ lọc hoặc dữ liệu nghiệp vụ cần thiết.

**Tiền điều kiện:** Actor có quyền quản trị; đối tượng và chi nhánh tồn tại/khả dụng.

**Luồng chính và các nhánh nghiệp vụ độc lập:**

1. SA tạo/sửa/ngừng chi nhánh; mọi role chỉ đọc phạm vi được phép.
2. SA tạo ADMIN/STAFF/TEACHER; A chỉ tạo STAFF/TEACHER trong phạm vi. Không tạo SUPER_ADMIN qua API.
3. SA quản lý tập phân công và chi nhánh chính; quản lý khóa/mở/reset tài khoản theo quyền, lưu sự kiện.

Các thao tác tạo/sửa/xóa/ngừng/đọc là biến thể độc lập; luồng nhóm không ép thực hiện hết hoặc tự động chạy bước tiếp theo.

**Ngoại lệ và quyền chi tiết:**

- A không khóa ADMIN/SUPER_ADMIN, không tác động account có phân công ngoài toàn bộ quyền quản lý.
- Thu hồi Teacher còn trách nhiệm hoặc người phụ trách lead chưa bàn giao bị chặn; lỗi nguồn kiểm tra không coi là an toàn.
- Không coi primary branch là giới hạn duy nhất; branch assignments là nguồn phạm vi hiện hành.

**Hậu điều kiện thành công:** Thay đổi account/membership có audit; status làm việc Teacher vẫn tách khóa đăng nhập.

**Bảo đảm khi lỗi:** transaction local chưa commit rollback; dữ liệu đã commit ở service khác không tự rollback. Hiển thị lỗi, giữ lý do nhập khi có thể và tải lại/tra intent theo hợp đồng trước retry. Không áp dụng cơ chế UUID cho use case mà API không có requestKey/requestId. 401/403/404/409/503 theo controller/service; 404 có thể che đối tượng ngoài phạm vi.

**Bằng chứng source:** [AuthService.java](../../auth-service/src/main/java/com/example/auth/service/AuthService.java); [API](../api-list.md); [quy tắc](business-rules.md). Các bài test cụ thể và mức kiểm chứng theo bàn giao G02–G12, không coi TC dự kiến G01 là đã chạy.

<a id="uc03"></a>

## UC03 — Quản lý hồ sơ và lịch sử học viên

**Actor:** SA/A/S. **Yêu cầu:** RQ01, RQ09, RQ10.

**Kích hoạt:** người có quyền chọn thao tác của nhóm use case, cung cấp định danh/bộ lọc hoặc dữ liệu nghiệp vụ cần thiết.

**Tiền điều kiện:** Trong chi nhánh được phép; Teacher không có quyền CRM.

**Luồng chính và các nhánh nghiệp vụ độc lập:**

1. Tạo hồ sơ với họ tên/liên hệ và dữ liệu tùy chọn; mã học viên do hệ thống sinh.
2. Tìm kiếm/lọc/sửa; đổi trạng thái hồ sơ với version và lý do.
3. Mở hồ sơ học tập: đầu vào, từng enrollment/khoảng học, điểm danh, kết quả và liên kết học phí đúng billing root.

Các thao tác tạo/sửa/xóa/ngừng/đọc là biến thể độc lập; luồng nhóm không ép thực hiện hết hoặc tự động chạy bước tiếp theo.

**Ngoại lệ và quyền chi tiết:**

- Chặn xóa nếu có enrollment, lead chuyển đổi tham chiếu, assessment kể cả thu hồi hoặc lịch sử đề nghị ngoại lệ;409.
- Trạng thái hồ sơ không tự hoàn thành/bảo lưu mọi enrollment; hoàn thành hồ sơ không che đăng ký bảo lưu.
- Legacy enrollment ngoài quyền không xuất hiện; Staff chỉ summary cuối khóa, không điểm/snapshot.

**Hậu điều kiện thành công:** Thông tin hợp lệ được lưu; lịch sử và quan hệ nghiệp vụ được giữ.

**Bảo đảm khi lỗi:** transaction local chưa commit rollback; dữ liệu đã commit ở service khác không tự rollback. Hiển thị lỗi, giữ lý do nhập khi có thể và tải lại/tra intent theo hợp đồng trước retry. Không áp dụng cơ chế UUID cho use case mà API không có requestKey/requestId. 401/403/404/409/503 theo controller/service; 404 có thể che đối tượng ngoài phạm vi.

**Bằng chứng source:** [StudentService.java](../../academic-service/src/main/java/com/example/academic/student/StudentService.java); [API](../api-list.md); [quy tắc](business-rules.md). Các bài test cụ thể và mức kiểm chứng theo bàn giao G02–G12, không coi TC dự kiến G01 là đã chạy.

<a id="uc04"></a>

## UC04 — Quản trị hồ sơ giáo viên

**Actor:** SA/A đủ phạm vi; S chỉ tra cứu khả dụng. **Yêu cầu:** RQ02, RQ10.

**Kích hoạt:** người có quyền chọn thao tác của nhóm use case, cung cấp định danh/bộ lọc hoặc dữ liệu nghiệp vụ cần thiết.

**Tiền điều kiện:** Dùng teacher profileId, không nhầm accountId; A phải quản lý toàn bộ phân công khi sửa hồ sơ dùng chung.

**Luồng chính và các nhánh nghiệp vụ độc lập:**

1. Đọc hồ sơ chuyên môn/liên hệ và cờ canManage.
2. Quản lý sửa chuyên môn/bằng cấp/chứng chỉ/liên hệ với version mới nhất.
3. Lưu audit trước/sau và tải lại; quyền xem lịch sử theo quyền quản lý đầy đủ.

Các thao tác tạo/sửa/xóa/ngừng/đọc là biến thể độc lập; luồng nhóm không ép thực hiện hết hoặc tự động chạy bước tiếp theo.

**Ngoại lệ và quyền chi tiết:**

- A chỉ có một phần phân công có thể xem phần được phép nhưng không sửa toàn cục hoặc xem history/responsibilities quản trị.
- Stale version409; Teacher tự sửa chỉ email/phone qua UC01, không chuyên môn/workStatus.
- Không có xóa Teacher vật lý trên màn quản trị; ngừng làm việc dùng UC05.

**Hậu điều kiện thành công:** Teacher.version/audit nhất quán; không đổi account role/branches qua body hồ sơ.

**Bảo đảm khi lỗi:** transaction local chưa commit rollback; dữ liệu đã commit ở service khác không tự rollback. Hiển thị lỗi, giữ lý do nhập khi có thể và tải lại/tra intent theo hợp đồng trước retry. Không áp dụng cơ chế UUID cho use case mà API không có requestKey/requestId. 401/403/404/409/503 theo controller/service; 404 có thể che đối tượng ngoài phạm vi.

**Bằng chứng source:** [TeacherProfileService.java](../../auth-service/src/main/java/com/example/auth/service/TeacherProfileService.java); [API](../api-list.md); [quy tắc](business-rules.md). Các bài test cụ thể và mức kiểm chứng theo bàn giao G02–G12, không coi TC dự kiến G01 là đã chạy.

<a id="uc05"></a>

## UC05 — Bàn giao và ngừng làm việc giáo viên

**Actor:** SA/A đủ phạm vi. **Yêu cầu:** RQ02, RQ06, RQ10.

**Kích hoạt:** người có quyền chọn thao tác của nhóm use case, cung cấp định danh/bộ lọc hoặc dữ liệu nghiệp vụ cần thiết.

**Tiền điều kiện:** Actor đủ mọi chi nhánh của Teacher; nguồn Academic đọc được; phiên bản Teacher hiện hành.

**Luồng chính và các nhánh nghiệp vụ độc lập:**

1. Tải responsibilities: lớp đang/sắp học và mọi buổi SCHEDULED, gồm quá ngày, dạy thay, học bù.
2. Bàn giao từng lớp/buổi qua chức năng Academic; chọn giáo viên thay đủ điều kiện, kiểm tra lịch/phòng.
3. Tải lại; chỉ khi hết trách nhiệm mới chuyển workStatus INACTIVE với reason/version.

Các thao tác tạo/sửa/xóa/ngừng/đọc là biến thể độc lập; luồng nhóm không ép thực hiện hết hoặc tự động chạy bước tiếp theo.

**Ngoại lệ và quyền chi tiết:**

- Còn việc409; lỗi Academic503, không suy ra danh sách rỗng.
- Đua phân công/ngừng hoặc thu hồi được phối hợp khóa account trong schema dùng chung; không cam kết transaction HTTP phân tán.
- Giữ buổi COMPLETED/điểm danh lịch sử; INACTIVE không tự khóa đăng nhập và ACTIVE không tự mở account.

**Hậu điều kiện thành công:** Work status thay đổi có audit khi đủ điều kiện; lịch sử giảng dạy không bị đổi ngược.

**Bảo đảm khi lỗi:** transaction local chưa commit rollback; dữ liệu đã commit ở service khác không tự rollback. Hiển thị lỗi, giữ lý do nhập khi có thể và tải lại/tra intent theo hợp đồng trước retry. Không áp dụng cơ chế UUID cho use case mà API không có requestKey/requestId. 401/403/404/409/503 theo controller/service; 404 có thể che đối tượng ngoài phạm vi.

**Bằng chứng source:** [TeacherProfileService.java](../../auth-service/src/main/java/com/example/auth/service/TeacherProfileService.java); [API](../api-list.md); [quy tắc](business-rules.md). Các bài test cụ thể và mức kiểm chứng theo bàn giao G02–G12, không coi TC dự kiến G01 là đã chạy.

<a id="uc06"></a>

## UC06 — Quản lý chương trình, khóa, đầu vào và thời lượng

**Actor:** SA/A/S/T tùy thao tác. **Yêu cầu:** RQ03, RQ05, RQ07.

**Kích hoạt:** người có quyền chọn thao tác của nhóm use case, cung cấp định danh/bộ lọc hoặc dữ liệu nghiệp vụ cần thiết.

**Tiền điều kiện:** Chương trình/thang và scope hợp lệ; quyền danh mục toàn cục khác quyền khóa tại chi nhánh.

**Luồng chính và các nhánh nghiệp vụ độc lập:**

1. SA quản lý chương trình/các mức; SA/A tạo khóa thuộc phạm vi, SA có thể tạo khóa chung.
2. Quản lý cấu hình OPEN_ENTRY hoặc tập mức chấp nhận, mục tiêu đầu ra; lưu policy revision/lý do.
3. Xác nhận số phút khóa và áp dụng kế hoạch lớp theo quyền; giữ snapshot/version, không tự quy đổi duration legacy.

Các thao tác tạo/sửa/xóa/ngừng/đọc là biến thể độc lập; luồng nhóm không ép thực hiện hết hoặc tự động chạy bước tiếp theo.

**Ngoại lệ và quyền chi tiết:**

- S có thể sửa thông tin khóa theo API nhưng không đổi giá; T chỉ đọc khóa có lớp được phép.
- Khóa chung chỉ SA sửa; không ép thang IELTS/TOEIC/CEFR quy đổi khi không có căn cứ.
- Ngừng khóa chặn tạo lớp mới từ khóa CLOSED; không tự hủy lớp đã mở. Thay kế hoạch khóa không ghi đè mọi kế hoạch lớp.

**Hậu điều kiện thành công:** Danh mục/policy/phút được lưu rõ phiên bản; legacy chưa xác minh vẫn null/UNVERIFIED.

**Bảo đảm khi lỗi:** transaction local chưa commit rollback; dữ liệu đã commit ở service khác không tự rollback. Hiển thị lỗi, giữ lý do nhập khi có thể và tải lại/tra intent theo hợp đồng trước retry. Không áp dụng cơ chế UUID cho use case mà API không có requestKey/requestId. 401/403/404/409/503 theo controller/service; 404 có thể che đối tượng ngoài phạm vi.

**Bằng chứng source:** [CourseService.java](../../academic-service/src/main/java/com/example/academic/course/CourseService.java); [API](../api-list.md); [quy tắc](business-rules.md). Các bài test cụ thể và mức kiểm chứng theo bàn giao G02–G12, không coi TC dự kiến G01 là đã chạy.

<a id="uc07"></a>

## UC07 — Quản lý phòng, mở lớp và lịch mẫu

**Actor:** SA/A; S trong giới hạn. **Yêu cầu:** RQ04, RQ06, RQ10.

**Kích hoạt:** người có quyền chọn thao tác của nhóm use case, cung cấp định danh/bộ lọc hoặc dữ liệu nghiệp vụ cần thiết.

**Tiền điều kiện:** Chi nhánh, khóa, phòng và Teacher khả dụng; startDate<=endDate, sĩ số trong sức chứa/giới hạn.

**Luồng chính và các nhánh nghiệp vụ độc lập:**

1. Tạo/sửa phòng và trạng thái; kiểm tra lớp đang tham chiếu trước giảm sức chứa/ngừng/xóa.
2. Tạo lớp, gán khóa/phòng/Teacher, kế hoạch đầu vào/phút và lịch mẫu theo quyền.
3. Quản lý chỉnh lịch qua schedules, đồng bộ buổi thực tế; lớp có deep link để kiểm tra.

Các thao tác tạo/sửa/xóa/ngừng/đọc là biến thể độc lập; luồng nhóm không ép thực hiện hết hoặc tự động chạy bước tiếp theo.

**Ngoại lệ và quyền chi tiết:**

- S không gửi lịch kèm tạo/sửa lớp và không đổi Teacher/phòng/khóa/ngày của lớp đang có; không quản lý lịch như SA/A.
- Không xóa lớp có enrollment/lịch sử; không đổi khóa sau ghi danh; FULL là dẫn xuất sĩ số.
- Trùng lịch, Teacher ngoài phân công/inactive hoặc phòng bảo trì bị chặn. Nguồn đã có final phải mở lại UC13.

**Hậu điều kiện thành công:** Lớp/lịch hợp lệ được lưu; việc đóng lớp là UC13, hết ngày không là đóng thủ công.

**Bảo đảm khi lỗi:** transaction local chưa commit rollback; dữ liệu đã commit ở service khác không tự rollback. Hiển thị lỗi, giữ lý do nhập khi có thể và tải lại/tra intent theo hợp đồng trước retry. Không áp dụng cơ chế UUID cho use case mà API không có requestKey/requestId. 401/403/404/409/503 theo controller/service; 404 có thể che đối tượng ngoài phạm vi.

**Bằng chứng source:** [SchoolClassService.java](../../academic-service/src/main/java/com/example/academic/schoolclass/SchoolClassService.java); [API](../api-list.md); [quy tắc](business-rules.md). Các bài test cụ thể và mức kiểm chứng theo bàn giao G02–G12, không coi TC dự kiến G01 là đã chạy.

<a id="uc08"></a>

## UC08 — Ghi nhận và xác nhận đánh giá đầu vào

**Actor:** SA/A/S. **Yêu cầu:** RQ01, RQ05.

**Kích hoạt:** người có quyền chọn thao tác của nhóm use case, cung cấp định danh/bộ lọc hoặc dữ liệu nghiệp vụ cần thiết.

**Tiền điều kiện:** Học viên trong scope; chương trình/mức còn dùng được; có căn cứ kiểm tra thật.

**Luồng chính và các nhánh nghiệp vụ độc lập:**

1. Tạo nháp, ghi phương thức/ngày/kết quả/mức và chứng cứ trong các trường hỗ trợ.
2. Sửa nháp với version/lý do; xác nhận bằng expectedCurrentId để thay current đúng chủ đích.
3. Xem lịch sử hoặc thu hồi có reason/version; xác nhận một assessment hiện hành mỗi chương trình.

Các thao tác tạo/sửa/xóa/ngừng/đọc là biến thể độc lập; luồng nhóm không ép thực hiện hết hoặc tự động chạy bước tiếp theo.

**Ngoại lệ và quyền chi tiết:**

- Sai thang/mức, version hoặc current đã đổi bị chặn.
- Thu hồi không tự khôi phục bản cũ; không dùng free-text currentLevel làm chứng cứ xác nhận.
- Không tự tạo kết quả assessment hoặc quy đổi chứng chỉ thiếu nguồn để đủ điều kiện phân lớp.

**Hậu điều kiện thành công:** Chứng cứ đầu vào có trạng thái/revision/audit; không tự ghi danh.

**Bảo đảm khi lỗi:** transaction local chưa commit rollback; dữ liệu đã commit ở service khác không tự rollback. Hiển thị lỗi, giữ lý do nhập khi có thể và tải lại/tra intent theo hợp đồng trước retry. Không áp dụng cơ chế UUID cho use case mà API không có requestKey/requestId. 401/403/404/409/503 theo controller/service; 404 có thể che đối tượng ngoài phạm vi.

**Bằng chứng source:** [PlacementAssessmentService.java](../../academic-service/src/main/java/com/example/academic/assessment/PlacementAssessmentService.java); [API](../api-list.md); [quy tắc](business-rules.md). Các bài test cụ thể và mức kiểm chứng theo bàn giao G02–G12, không coi TC dự kiến G01 là đã chạy.

<a id="uc09"></a>

## UC09 — Gợi ý lớp, ghi danh, bảo lưu, học lại và chuyển lớp

**Actor:** SA/A/S. **Yêu cầu:** RQ01, RQ04, RQ05, RQ08.

**Kích hoạt:** người có quyền chọn thao tác của nhóm use case, cung cấp định danh/bộ lọc hoặc dữ liệu nghiệp vụ cần thiết.

**Tiền điều kiện:** Student/lớp đúng scope và trạng thái; preview không phải giữ chỗ; khi lưu phải kiểm tra lại.

**Luồng chính và các nhánh nghiệp vụ độc lập:**

1. Xem eligibility/gợi ý cùng chương trình, thời điểm và căn cứ; chọn lớp phù hợp hoặc đề nghị UC10.
2. Lưu ENROLL/RESUME/TRANSFER; kiểm tra scope, chỗ, lịch, profile/Teacher và điều kiện dưới khóa.
3. Mở/đóng EnrollmentPeriod, lưu admission snapshot và history; ENROLL phát sự kiện tạo học phí sau commit.
4. Bảo lưu cần ngày dự kiến sau hôm nay; học lại là thao tác rõ ràng; chuyển chỉ cùng khóa/chi nhánh và giữ billingEnrollmentId.

Các thao tác tạo/sửa/xóa/ngừng/đọc là biến thể độc lập; luồng nhóm không ép thực hiện hết hoặc tự động chạy bước tiếp theo.

**Ngoại lệ và quyền chi tiết:**

- Thiếu học thuật có thể qua ngoại lệ được duyệt; giới hạn cứng không được vượt. Lớp đầy/trùng/đối tượng không khả dụng bị chặn.
- Finance lỗi: enrollment vẫn lưu với tuitionDelivery FAILED/PENDING; retry tạo phí, không ghi danh mới.
- Terminal không mở lại; COMPLETED dùng UC13 với refs hiện hành. Current final khóa thay đổi membership/period đến khi mở lại.

**Hậu điều kiện thành công:** Enrollment/period/history và căn cứ đúng; thu tiền là UC14 riêng, không atomically cùng ghi danh.

**Bảo đảm khi lỗi:** transaction local chưa commit rollback; dữ liệu đã commit ở service khác không tự rollback. Hiển thị lỗi, giữ lý do nhập khi có thể và tải lại/tra intent theo hợp đồng trước retry. Không áp dụng cơ chế UUID cho use case mà API không có requestKey/requestId. 401/403/404/409/503 theo controller/service; 404 có thể che đối tượng ngoài phạm vi.

**Bằng chứng source:** [EnrollmentService.java](../../academic-service/src/main/java/com/example/academic/enrollment/EnrollmentService.java); [API](../api-list.md); [quy tắc](business-rules.md). Các bài test cụ thể và mức kiểm chứng theo bàn giao G02–G12, không coi TC dự kiến G01 là đã chạy.

<a id="uc10"></a>

## UC10 — Đề nghị và duyệt ngoại lệ đầu vào

**Actor:** SA/A/S đề nghị; SA/A quyết định. **Yêu cầu:** RQ04, RQ05, RQ10.

**Kích hoạt:** người có quyền chọn thao tác của nhóm use case, cung cấp định danh/bộ lọc hoặc dữ liệu nghiệp vụ cần thiết.

**Tiền điều kiện:** Có preview context/fingerprint mới nhất; không dùng ngoại lệ để vượt sĩ số, chi nhánh hoặc trùng lịch.

**Luồng chính và các nhánh nghiệp vụ độc lập:**

1. Người phụ trách ghi lý do và gửi REQUESTED theo ENROLL/RESUME/TRANSFER context.
2. Quản lý xem căn cứ, revalidate rồi approve/reject với version và lý do; căn cứ đổi thành INVALIDATED.
3. Người thực hiện ghi danh/học lại/chuyển bằng approvalId; server kiểm lại và đánh dấu CONSUMED cùng nghiệp vụ.

Các thao tác tạo/sửa/xóa/ngừng/đọc là biến thể độc lập; luồng nhóm không ép thực hiện hết hoặc tự động chạy bước tiếp theo.

**Ngoại lệ và quyền chi tiết:**

- Staff không duyệt; không dùng lại approval cho học viên/lớp/intent khác.
- Căn cứ hoặc enrollment/version/policy thay đổi: từ chối/invalidate; không tự duyệt lại.
- Hai yêu cầu tiêu thụ không tạo hai đăng ký; approval không giữ chỗ và có thể không còn dùng được khi lớp đầy.

**Hậu điều kiện thành công:** Quyết định và ảnh chụp căn cứ được giữ; chỉ lần tiêu thụ hợp lệ tạo thay đổi học vụ.

**Bảo đảm khi lỗi:** transaction local chưa commit rollback; dữ liệu đã commit ở service khác không tự rollback. Hiển thị lỗi, giữ lý do nhập khi có thể và tải lại/tra intent theo hợp đồng trước retry. Không áp dụng cơ chế UUID cho use case mà API không có requestKey/requestId. 401/403/404/409/503 theo controller/service; 404 có thể che đối tượng ngoài phạm vi.

**Bằng chứng source:** [PlacementService.java](../../academic-service/src/main/java/com/example/academic/placement/PlacementService.java); [API](../api-list.md); [quy tắc](business-rules.md). Các bài test cụ thể và mức kiểm chứng theo bàn giao G02–G12, không coi TC dự kiến G01 là đã chạy.

<a id="uc11"></a>

## UC11 — Điều phối buổi, nghỉ/bù/dạy thay và điểm danh

**Actor:** SA/A điều phối; SA/A/S/T điểm danh theo điều kiện. **Yêu cầu:** RQ06, RQ07, RQ10.

**Kích hoạt:** người có quyền chọn thao tác của nhóm use case, cung cấp định danh/bộ lọc hoặc dữ liệu nghiệp vụ cần thiết.

**Tiền điều kiện:** Buổi thuộc scope; T chỉ buổi được phân công thực tế khi ghi điểm danh; đúng roster theo ngày/enrollment.

**Luồng chính và các nhánh nghiệp vụ độc lập:**

1. Quản lý sinh/chỉnh/hủy/khôi phục/học bù, gán actual Teacher; kiểm tra xung đột và version/lý do.
2. Mở sheet buổi không hủy/không tương lai; gửi đủ bản ghi theo roster cùng version.
3. Lưu điểm danh làm buổi COMPLETED; quản lý sửa sau hoàn tất có reason/audit.
4. Đối soát giờ COMPLETED bằng correct-time khi có chứng cứ và đã mở khóa nguồn kết quả.

Các thao tác tạo/sửa/xóa/ngừng/đọc là biến thể độc lập; luồng nhóm không ép thực hiện hết hoặc tự động chạy bước tiếp theo.

**Ngoại lệ và quyền chi tiết:**

- T dạy thay không vì thế được xem CRM hoặc bảng điểm cả lớp chính không phụ trách.
- Buổi đã điểm danh khóa sửa thông thường ngày/giờ/phòng/Teacher; sửa điểm danh hoàn tất chỉ quản lý.
- Hủy không tính phút; legacy thiếu giờ UNKNOWN; buổi đã qua không tự COMPLETED. Current final chặn sửa nguồn.

**Hậu điều kiện thành công:** Buổi/attendance có version/audit; tổng phút theo nguồn biết được, không suy từ lịch quá ngày.

**Bảo đảm khi lỗi:** transaction local chưa commit rollback; dữ liệu đã commit ở service khác không tự rollback. Hiển thị lỗi, giữ lý do nhập khi có thể và tải lại/tra intent theo hợp đồng trước retry. Không áp dụng cơ chế UUID cho use case mà API không có requestKey/requestId. 401/403/404/409/503 theo controller/service; 404 có thể che đối tượng ngoài phạm vi.

**Bằng chứng source:** [SessionPlanner.java](../../academic-service/src/main/java/com/example/academic/session/SessionPlanner.java); [API](../api-list.md); [quy tắc](business-rules.md). Các bài test cụ thể và mức kiểm chứng theo bàn giao G02–G12, không coi TC dự kiến G01 là đã chạy.

<a id="uc12"></a>

## UC12 — Bài kiểm tra, điểm, quy tắc và đánh giá tạm

**Actor:** SA/A/T theo lớp; Staff không quản lý điểm. **Yêu cầu:** RQ07, RQ09, RQ10.

**Kích hoạt:** người có quyền chọn thao tác của nhóm use case, cung cấp định danh/bộ lọc hoặc dữ liệu nghiệp vụ cần thiết.

**Tiền điều kiện:** Lớp thuộc scope; T là Teacher chính; nguồn enrollment theo ngày thi/frozen roster có thể xác định.

**Luồng chính và các nhánh nghiệp vụ độc lập:**

1. SA/A/T tạo và quản lý bài hợp lệ; freeze roster khi nhập điểm.
2. T nhập/sửa điểm của roster; SA/A chỉ hiệu chỉnh điểm đã có với lý do, không nhập điểm ban đầu thay Teacher.
3. SA/A xác nhận policy lớp: bài bắt buộc, trọng số, ngưỡng và chuyên cần tùy chọn, có revision/history.
4. Đọc preview chuẩn hóa theo thang và trọng số; phân biệt PASS/FAIL tạm và INSUFFICIENT.

Các thao tác tạo/sửa/xóa/ngừng/đọc là biến thể độc lập; luồng nhóm không ép thực hiện hết hoặc tự động chạy bước tiếp theo.

**Ngoại lệ và quyền chi tiết:**

- 0 là điểm đã nhập; null/chưa chấm khác0. Thiếu bài bắt buộc, nguồn không rõ hoặc bài chưa đến ngày không thành PASS giả.
- Ngưỡng so với số chưa làm tròn; tỷ lệ chuyên cần không biết không mặc định100%.
- Policy/exam stale hoặc metadata bài đã dùng không được đổi tùy tiện; current final khóa mọi nguồn đến UC13 mở lại.

**Hậu điều kiện thành công:** Điểm/quy tắc có audit; preview không tự tạo kết quả cuối, không tự COMPLETED/FINISHED.

**Bảo đảm khi lỗi:** transaction local chưa commit rollback; dữ liệu đã commit ở service khác không tự rollback. Hiển thị lỗi, giữ lý do nhập khi có thể và tải lại/tra intent theo hợp đồng trước retry. Không áp dụng cơ chế UUID cho use case mà API không có requestKey/requestId. 401/403/404/409/503 theo controller/service; 404 có thể che đối tượng ngoài phạm vi.

**Bằng chứng source:** [EvaluationService.java](../../teaching-service/src/main/java/com/example/teaching/evaluation/EvaluationService.java); [API](../api-list.md); [quy tắc](business-rules.md). Các bài test cụ thể và mức kiểm chứng theo bàn giao G02–G12, không coi TC dự kiến G01 là đã chạy.

<a id="uc13"></a>

## UC13 — Chốt/mở lại kết quả, hoàn thành học vụ và đóng lớp

**Actor:** SA/A chốt/mở/đóng lớp; S đồng bộ hoàn thành; T đọc lớp chính. **Yêu cầu:** RQ04, RQ07, RQ10.

**Kích hoạt:** người có quyền chọn thao tác của nhóm use case, cung cấp định danh/bộ lọc hoặc dữ liệu nghiệp vụ cần thiết.

**Tiền điều kiện:** Có policy và nguồn đủ căn cứ; đủ phút xác minh, không buổi chưa xử lý; Enrollment STUDYING hoặc COMPLETED cần xác nhận lại.

**Luồng chính và các nhánh nghiệp vụ độc lập:**

1. SA/A xem preview/fingerprint/blockers rồi chốt từng enrollment với UUID intent/lý do.
2. Lưu snapshot policy/điểm/chuyên cần/phút, revision/actor/time; khóa nguồn toàn lớp.
3. SA/A/S đồng bộ COMPLETED riêng bằng finalResultId/revision hiện hành; PASS hoặc FAIL đều được hoàn tất quá trình học.
4. SA/A đóng lớp riêng khi hết STUDYING/RESERVED/SCHEDULED và mọi COMPLETED khớp refs; lưu closure revision/evidence.
5. Khi sửa: mở toàn bộ current với lý do/version; nếu lớp đã đóng thì mở lớp, sửa nguồn, chốt lại, đồng bộ refs và đối soát lại.

Các thao tác tạo/sửa/xóa/ngừng/đọc là biến thể độc lập; luồng nhóm không ép thực hiện hết hoặc tự động chạy bước tiếp theo.

**Ngoại lệ và quyền chi tiết:**

- INSUFFICIENT hoặc preview thay đổi409; cùng intent/payload trả cùng kết quả, payload khác409.
- Teaching/Academic lỗi503: giữ trạng thái đã commit ở service trước, tra intent/tải lại và retry bước thiếu; không distributed transaction.
- Mở kết quả không mở enrollment terminal/đảo phí; snapshot cũ bất biến. Legacy chưa có chứng cứ là LEGACY_UNVERIFIED; closure nguồn đổi là NEEDS_REVIEW.

**Hậu điều kiện thành công:** Tối đa một current/enrollment; có history; dateStatus FINISHED tách closed; học phí không quyết định PASS hoặc hoàn tất.

**Bảo đảm khi lỗi:** transaction local chưa commit rollback; dữ liệu đã commit ở service khác không tự rollback. Hiển thị lỗi, giữ lý do nhập khi có thể và tải lại/tra intent theo hợp đồng trước retry. Không áp dụng cơ chế UUID cho use case mà API không có requestKey/requestId. 401/403/404/409/503 theo controller/service; 404 có thể che đối tượng ngoài phạm vi.

**Bằng chứng source:** [FinalResultService.java](../../teaching-service/src/main/java/com/example/teaching/results/FinalResultService.java); [API](../api-list.md); [quy tắc](business-rules.md). Các bài test cụ thể và mức kiểm chứng theo bàn giao G02–G12, không coi TC dự kiến G01 là đã chạy.

<a id="uc14"></a>

## UC14 — Học phí, thu tiền, điều chỉnh, đảo và hoàn

**Actor:** SA/A/S xem/thu; SA/A điều chỉnh/đảo/hoàn. **Yêu cầu:** RQ08, RQ09, RQ10.

**Kích hoạt:** người có quyền chọn thao tác của nhóm use case, cung cấp định danh/bộ lọc hoặc dữ liệu nghiệp vụ cần thiết.

**Tiền điều kiện:** Enrollment billing root đọc được; phí/phiếu thuộc scope; version/requestKey theo hợp đồng.

**Luồng chính và các nhánh nghiệp vụ độc lập:**

1. Tạo hoặc retry học phí từ enrollment root, lấy giá snapshot từ Academic, chống tạo trùng.
2. Thu trong số dư cho phép với requestKey; ghi receipt, người thu, phương thức và thời điểm.
3. SA/A điều chỉnh khoản phải thu hoặc VOID/REFUND theo giới hạn, lý do/version/requestKey; giữ phiếu gốc và journal.
4. Xem lịch sử, công nợ/credit và đối soát ledger.

Các thao tác tạo/sửa/xóa/ngừng/đọc là biến thể độc lập; luồng nhóm không ép thực hiện hết hoặc tự động chạy bước tiếp theo.

**Ngoại lệ và quyền chi tiết:**

- Mất phản hồi sau commit: tra trạng thái và retry cùng requestKey, không lập phiếu mới vì chưa thấy UI cập nhật.
- VOID chỉ toàn phiếu hợp lệ; REFUND trong credit được phép; điều chỉnh xuống dưới thu ròng tạo credit, không xóa tiền thu.
- Teacher không tài chính; Staff không điều chỉnh/đảo/hoàn. Phương thức CARD/WALLET chỉ dữ liệu ghi nhận, chưa là tích hợp cổng thanh toán.

**Hậu điều kiện thành công:** Tiền/phiếu/journal khớp, không nhân đôi; số thu ròng tách khoản phải thu và credit.

**Bảo đảm khi lỗi:** transaction local chưa commit rollback; dữ liệu đã commit ở service khác không tự rollback. Hiển thị lỗi, giữ lý do nhập khi có thể và tải lại/tra intent theo hợp đồng trước retry. Không áp dụng cơ chế UUID cho use case mà API không có requestKey/requestId. 401/403/404/409/503 theo controller/service; 404 có thể che đối tượng ngoài phạm vi.

**Bằng chứng source:** [PaymentService.java](../../finance-service/src/main/java/com/example/finance/payment/PaymentService.java); [API](../api-list.md); [quy tắc](business-rules.md). Các bài test cụ thể và mức kiểm chứng theo bàn giao G02–G12, không coi TC dự kiến G01 là đã chạy.

<a id="uc15"></a>

## UC15 — Tìm kiếm, dashboard và báo cáo

**Actor:** SA/A tổng hợp; S/T đọc nguồn theo quyền. **Yêu cầu:** RQ01, RQ07, RQ09, RQ10.

**Kích hoạt:** người có quyền chọn thao tác của nhóm use case, cung cấp định danh/bộ lọc hoặc dữ liệu nghiệp vụ cần thiết.

**Tiền điều kiện:** Đăng nhập, scope và bộ lọc hợp lệ; tổng hợp chỉ trên nguồn có quyền.

**Luồng chính và các nhánh nghiệp vụ độc lập:**

1. Tìm/lọc/phân trang học viên, khóa/lớp, lịch và nguồn khác theo quyền.
2. SA/A xem báo cáo số học viên/ghi danh, thu ròng/công nợ/credit theo kỳ và chi nhánh.
3. Đọc đánh giá tạm theo policy và hồ sơ final riêng, không gộp trạng thái tạm/chính thức.

Các thao tác tạo/sửa/xóa/ngừng/đọc là biến thể độc lập; luồng nhóm không ép thực hiện hết hoặc tự động chạy bước tiếp theo.

**Ngoại lệ và quyền chi tiết:**

- Khoản thu theo thời điểm giao dịch không đồng nghĩa doanh thu kế toán dồn tích.
- Hồ sơ người học khác lượt enrollment; thiếu dữ liệu/nguồn lỗi không được thay bằng0/PASS.
- Không suy quyền báo cáo tài chính từ quyền xem dashboard cá nhân; T không báo cáo doanh thu.

**Hậu điều kiện thành công:** Thông tin đúng phạm vi và định nghĩa chỉ số; số liệu phải được nghiệm thu bộ expected tại G17/G18.

**Bảo đảm khi lỗi:** transaction local chưa commit rollback; dữ liệu đã commit ở service khác không tự rollback. Hiển thị lỗi, giữ lý do nhập khi có thể và tải lại/tra intent theo hợp đồng trước retry. Không áp dụng cơ chế UUID cho use case mà API không có requestKey/requestId. 401/403/404/409/503 theo controller/service; 404 có thể che đối tượng ngoài phạm vi.

**Bằng chứng source:** [StatsService.java](../../academic-service/src/main/java/com/example/academic/stats/StatsService.java); [API](../api-list.md); [quy tắc](business-rules.md). Các bài test cụ thể và mức kiểm chứng theo bàn giao G02–G12, không coi TC dự kiến G01 là đã chạy.

<a id="uc16"></a>

## UC16 — Tài liệu giảng dạy, duyệt và phiên bản

**Actor:** SA/A quản lý/duyệt; T sở hữu; S chỉ bản được duyệt. **Yêu cầu:** RQ07, RQ10.

**Kích hoạt:** người có quyền chọn thao tác của nhóm use case, cung cấp định danh/bộ lọc hoặc dữ liệu nghiệp vụ cần thiết.

**Tiền điều kiện:** File và scope class/course hợp lệ; Teacher chỉ nguồn được giao và tài liệu sở hữu.

**Luồng chính và các nhánh nghiệp vụ độc lập:**

1. Người có quyền upload file/metadata; lưu storageKey riêng, trạng thái PENDING.
2. SA/A approve/reject bằng version, từ chối cần lý do.
3. Chủ sở hữu gửi lại bản REJECTED với revision mới; lịch sử giữ file cũ, tải đúng revision theo quyền.

Các thao tác tạo/sửa/xóa/ngừng/đọc là biến thể độc lập; luồng nhóm không ép thực hiện hết hoặc tự động chạy bước tiếp theo.

**Ngoại lệ và quyền chi tiết:**

- Staff chỉ xem/download APPROVED, không history/resubmit/upload; T không xem tài liệu người khác chỉ vì đã duyệt.
- Stale version/không đúng chủ sở hữu/trạng thái không REJECTED bị chặn; file sai loại/kích thước bị validation.
- File trên filesystem tách metadata MySQL; backup cần cả hai, không cam kết transaction nguyên tử DB/file.

**Hậu điều kiện thành công:** Metadata, review/revision/history đúng; không công khai đường dẫn storage hoặc cho ghi đè phiên bản cũ.

**Bảo đảm khi lỗi:** transaction local chưa commit rollback; dữ liệu đã commit ở service khác không tự rollback. Hiển thị lỗi, giữ lý do nhập khi có thể và tải lại/tra intent theo hợp đồng trước retry. Không áp dụng cơ chế UUID cho use case mà API không có requestKey/requestId. 401/403/404/409/503 theo controller/service; 404 có thể che đối tượng ngoài phạm vi.

**Bằng chứng source:** [DocumentService.java](../../teaching-service/src/main/java/com/example/teaching/document/DocumentService.java); [API](../api-list.md); [quy tắc](business-rules.md). Các bài test cụ thể và mức kiểm chứng theo bàn giao G02–G12, không coi TC dự kiến G01 là đã chạy.

<a id="uc17"></a>

## UC17 — Sao lưu, diễn tập phục hồi và bàn giao

**Actor:** Người vận hành được ủy quyền; ngoài bốn role RBAC. **Yêu cầu:** RQ11, RQ12.

**Kích hoạt:** người có quyền chọn thao tác của nhóm use case, cung cấp định danh/bộ lọc hoặc dữ liệu nghiệp vụ cần thiết.

**Tiền điều kiện:** Có quyền môi trường và kế hoạch backup; bản sao đích riêng; không tự suy SA có quyền hệ điều hành.

**Luồng chính và các nhánh nghiệp vụ độc lập:**

1. Tạm ngừng ghi để lấy điểm phục hồi đủ điều kiện, backup SQL và file với manifest/hash.
2. Verify, restore vào schema mới, áp dụng migration theo thứ tự và chạy smoke trên cổng riêng.
3. Lưu kết quả, giới hạn và hướng dẫn vận hành; giữ dữ liệu thử, không tự DROP schema.

Các thao tác tạo/sửa/xóa/ngừng/đọc là biến thể độc lập; luồng nhóm không ép thực hiện hết hoặc tự động chạy bước tiếp theo.

**Ngoại lệ và quyền chi tiết:**

- Online rehearsal không chứng minh điểm khôi phục production nhất quán.
- Checksum sai hoặc tài liệu thiếu thì dừng, không coi thành công.
- Không khôi phục đè database đang dùng; không có UI backup cho một role ứng dụng trong source hiện tại.

**Hậu điều kiện thành công:** Có bản sao/biên bản kiểm chứng đúng mức; không chứng nhận SLA hoặc production khi chưa nghiệm thu.

**Bảo đảm khi lỗi:** transaction local chưa commit rollback; dữ liệu đã commit ở service khác không tự rollback. Hiển thị lỗi, giữ lý do nhập khi có thể và tải lại/tra intent theo hợp đồng trước retry. Không áp dụng cơ chế UUID cho use case mà API không có requestKey/requestId. 401/403/404/409/503 theo controller/service; 404 có thể che đối tượng ngoài phạm vi.

**Bằng chứng source:** [operations.cjs](../../scripts/operations.cjs); [API](../api-list.md); [quy tắc](business-rules.md). Các bài test cụ thể và mức kiểm chứng theo bàn giao G02–G12, không coi TC dự kiến G01 là đã chạy.

<a id="uc18"></a>

## UC18 — Tiếp nhận và chăm sóc khách hàng tiềm năng

**Actor:** SA/A/S; S chỉ lead được giao. **Yêu cầu:** RQ01, RQ09, RQ10.

**Kích hoạt:** người có quyền chọn thao tác của nhóm use case, cung cấp định danh/bộ lọc hoặc dữ liệu nghiệp vụ cần thiết.

**Tiền điều kiện:** Lead và người phụ trách thuộc scope; version mới nhất; người được giao khả dụng.

**Luồng chính và các nhánh nghiệp vụ độc lập:**

1. Tạo lead, ghi nhu cầu/khóa quan tâm và người phụ trách theo quyền.
2. Ghi tương tác có channel/note/status/lần liên hệ tiếp; SA/A bàn giao người phụ trách với lý do.
3. Khi trạng thái đủ điều kiện, convert tạo Student và đánh dấu liên kết trong transaction Academic.

Các thao tác tạo/sửa/xóa/ngừng/đọc là biến thể độc lập; luồng nhóm không ép thực hiện hết hoặc tự động chạy bước tiếp theo.

**Ngoại lệ và quyền chi tiết:**

- NOTE/đổi trạng thái không tự là liên hệ thực tế; không đặt CONVERTED bằng PATCH trạng thái.
- Không sửa lead đã CONVERTED; Staff không sửa lead người khác.
- Version cũ/trùng convert bị chặn hoặc trả dữ liệu đã có theo hợp đồng; không tạo hai hồ sơ do retry.

**Hậu điều kiện thành công:** Lead có history và liên kết Student khi convert; ghi danh vẫn là UC09 riêng.

**Bảo đảm khi lỗi:** transaction local chưa commit rollback; dữ liệu đã commit ở service khác không tự rollback. Hiển thị lỗi, giữ lý do nhập khi có thể và tải lại/tra intent theo hợp đồng trước retry. Không áp dụng cơ chế UUID cho use case mà API không có requestKey/requestId. 401/403/404/409/503 theo controller/service; 404 có thể che đối tượng ngoài phạm vi.

**Bằng chứng source:** [LeadService.java](../../academic-service/src/main/java/com/example/academic/lead/LeadService.java); [API](../api-list.md); [quy tắc](business-rules.md). Các bài test cụ thể và mức kiểm chứng theo bàn giao G02–G12, không coi TC dự kiến G01 là đã chạy.
