# Actor và quyền theo thao tác — G13

Ngày08/11/2026; đối chiếu controller, service, BranchScope/TeacherDirectory và navigation hiện tại. SA=SUPER_ADMIN, A=ADMIN, S=STAFF, T=TEACHER. **Không dùng kế thừa actor để suy quyền.** Mọi ô còn chịu scope/trạng thái/sở hữu; API là nguồn thực thi, sidebar không cấp quyền.

| Thao tác | SA | A | S | T |
|---|---|---|---|---|
| Đăng nhập, mật khẩu, phiên/liên hệ cá nhân | Có | Có | Có | Có; tự liên hệ Teacher tham gia version |
| Tạo/sửa/ngừng chi nhánh; thay phân công | Có | Không | Không | Không |
| Tạo tài khoản | ADMIN/STAFF/TEACHER; không tạo SA qua API | STAFF/TEACHER trong phạm vi | Không | Không |
| Khóa/mở/reset account | Theo guard; không khóa SA | Không ADMIN/SA; đủ mọi phân công mục tiêu | Không | Không |
| Hồ sơ Teacher dùng chung, history/responsibilities, ngừng làm việc | Có | Cần quyền tất cả phân công | Chỉ tra cứu Teacher khả dụng | Không quản trị; chỉ UC01 tự liên hệ |
| Học viên, assessment, CRM/hồ sơ tổng hợp | Có | Trong phạm vi | Trong phạm vi | Không |
| Lead, contact, convert | Có | Trong phạm vi | Lead được giao | Không |
| Đổi người phụ trách lead | Có | Trong phạm vi | Không | Không |
| Danh mục chương trình/thang | Quản lý | Đọc | Đọc | Đọc theo quyền nguồn |
| Tạo khóa / sửa khóa chi nhánh | Có | Có | Không tạo; sửa nhưng giữ giá | Đọc khóa có lớp được phép |
| Sửa khóa chung / policy/phút khóa chung | Có | Không | Không | Không |
| Policy đầu vào, áp dụng lớp, xác nhận phút | Có | Phạm vi khóa/lớp | Chỉ đọc | Chỉ đọc nguồn được phép |
| Phòng / lớp | Quản lý theo guard | Quản lý theo guard | Phòng; lớp giới hạn teacher/room/course/dates và không lịch kèm | Đọc lớp chính |
| Lịch mẫu / sinh/chỉnh/hủy/khôi phục/bù/dạy thay buổi | Có | Trong phạm vi | Đọc | Lịch/buổi được giao |
| Điểm danh trước hoàn tất | Có | Có | Có trong phạm vi | Buổi thực tế được giao, không hủy/tương lai |
| Sửa điểm danh COMPLETED / đối soát giờ | Có, reason/version | Có, reason/version | Không | Không |
| Bài kiểm tra | Có | Có | Không | Lớp chính |
| Điểm ban đầu | Không nhập thay Teacher | Không nhập thay Teacher | Không | Roster theo ngày/frozen |
| Sửa điểm đã có | Lý do/audit | Lý do/audit | Không | Theo roster/quyền; giữ audit |
| Policy đánh giá / chốt-mở final | Có | Trong phạm vi | Không | Đọc lớp chính |
| Summary final / đồng bộ enrollment COMPLETED | Có | Có | Có; không điểm/snapshot | Chỉ đọc final lớp chính, không đồng bộ |
| History snapshot final | Có | Trong phạm vi | Không | Lớp chính |
| Đóng/mở lớp | Có | Trong phạm vi | Không | Không |
| Đề nghị ngoại lệ / quyết định | Đề nghị và duyệt | Đề nghị và duyệt; được tự duyệt, có actor/lý do | Chỉ đề nghị/tiêu thụ hợp lệ | Không |
| Xem phí, phiếu, journal; thu | Có | Có | Có | Không |
| Điều chỉnh phí, VOID/REFUND, reconciliation/báo cáo tài chính | Có | Trong phạm vi | Không | Không |
| Tài liệu | Upload/quản lý/duyệt; resubmit đúng chủ sở hữu | Tương tự trong phạm vi | Chỉ APPROVED, không history | Tài liệu sở hữu; resubmit REJECTED, không duyệt |
| Dashboard/tìm nguồn | Toàn hệ thống theo nguồn | Trong phạm vi | Nguồn vận hành được phép | Lớp/lịch/nguồn giảng dạy được phép |
| Backup/restore môi trường | Không suy từ role ứng dụng | Không suy từ role ứng dụng | Không | Không |

Teacher dạy thay được ghi điểm danh buổi phân công thực tế; quyền đọc toàn lớp/bảng điểm vẫn theo Teacher chính. Admin nhiều chi nhánh dùng active assignments, không chỉ branchId chính. Người quản trị chỉ một phần phân công của Teacher không được sửa hồ sơ chung hoặc ngừng toàn cục. Không có role học viên/phụ huynh/kế toán độc lập trong bản hiện tại.

Bằng chứng: [AuthService](../../auth-service/src/main/java/com/example/auth/service/AuthService.java), [TeacherProfileService](../../auth-service/src/main/java/com/example/auth/service/TeacherProfileService.java), [SchoolClassService](../../academic-service/src/main/java/com/example/academic/schoolclass/SchoolClassService.java), [SessionPlanner](../../academic-service/src/main/java/com/example/academic/session/SessionPlanner.java), [GradeService](../../teaching-service/src/main/java/com/example/teaching/grade/GradeService.java), [DocumentService](../../teaching-service/src/main/java/com/example/teaching/document/DocumentService.java), [API](../api-list.md). Các link source dùng backend root; xem chú giải diagram, không đọc đường nối nhóm như toàn quyền.
