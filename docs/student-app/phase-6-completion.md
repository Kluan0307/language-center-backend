# Student App — Giai đoạn 6: tài liệu và hỗ trợ

Thực hiện ngày 12/11/2026, thuộc lộ trình mobile 10 giai đoạn. Không phải mốc G6 của bản release web trước đây.

## Sử dụng và nâng cấp

Backup trước khi nâng schema. Áp dụng V014, V015 nếu database chưa có; sau đó dừng ghi Academic/Teaching và chạy [V016](../migrations/V016__student_documents_support.sql). Không tạo lại database và không xóa dữ liệu. Maven không tự thực thi migration. Database nguồn chưa được thay đổi trong lần triển khai này; V016 đã apply/replay hai lần trên MySQL riêng và cả5service chạy prod/validate.

Ví dụ trong MySQL client, sau khi chọn đúng database và kiểm tra backup:

```sql
USE language_center_db;
SOURCE D:/hoctap/KLTN/language-center/language-center-backend/language-center-backend/docs/migrations/V016__student_documents_support.sql;
```

DDL MySQL không rollback toàn bộ. Migration không bật chia sẻ cho tài liệu cũ. Nâng Academic/Teaching và web cùng phiên bản; app cài dependencies theo package-lock (`npm ci`). Giữ cấu hình gateway `/api/v1`, cổng/LAN như G4. Expo FileSystem, Sharing, Crypto được cài theo SDK dự án; native build cần cập nhật module/plugin tương ứng.

Web: Tài liệu → Xem → duyệt (nếu pending) → Chia sẻ cho học viên, nhập lý do. Ngừng chia sẻ sẽ chặn tải qua ứng dụng, kể cả URL đã lưu. SUPER_ADMIN/ADMIN đúng chi nhánh và giáo viên sở hữu tài liệu được thao tác; STAFF chỉ xem/tải tài liệu được duyệt như trước.

App: Học tập → mở lớp → Tài liệu lớp học. Tải/mở file dùng API có bearer header. Cá nhân → Hỗ trợ → tạo yêu cầu, chọn loại/lớp liên quan, gửi và theo dõi phản hồi. Trạng thái được lọc bằng bộ chọn native. Khi quên mật khẩu, màn hình đăng nhập vẫn mở Liên hệ trung tâm mà không bắt đăng nhập để gửi ticket.

Web: menu Hỗ trợ học viên cho SUPER_ADMIN/ADMIN/STAFF. Mở yêu cầu để phản hồi công khai, lưu ghi chú nội bộ hoặc cập nhật trạng thái. Giáo viên không có menu/quyền đọc ticket cá nhân.

## Tài liệu: phạm vi và thu hồi

Tài liệu phải APPROVED và sharedToStudents=true. Academic trả scope đăng ký do chính account/profile sở hữu; Teaching khớp lớp hoặc khóa, đồng thời khớp branchId của lớp đăng ký. Khóa dùng chung ID giữa nhiều chi nhánh không tự cấp quyền chéo chi nhánh. Tài liệu lớp không được coi thành tài liệu khóa chỉ vì cùng course.

Đăng ký hiện tại/bảo lưu/chuyển/hoàn thành vẫn có quyền học liệu đã chia sẻ. Đăng ký ngày tương lai không cấp quyền sớm hơn ngày đăng ký. DROPPED không có period học thực tế đã bắt đầu thì không cấp học liệu; nếu có period xác thực giao với thời gian lớp trước ngày hiện tại thì giữ lịch sử. Mở trước ngày khai giảng cho đăng ký hiện hành là có chủ đích để học viên chuẩn bị bài. Lịch sử tài liệu không phụ thuộc danh sách lớp hiện tại của giáo viên.

List và download đều kiểm quyền lại. Không có đường dẫn storageKey tuyệt đối, URL công khai hoặc token trong query. Student không gọi được download phiên bản/quản trị cũ. File hiện hành được trả dạng attachment, no-store, nosniff; revision cũ không được phát riêng cho student. Resubmit chuyển về pending và xóa cờ chia sẻ. Sharing có kiểm version và audit; không sửa audit cũ. Download đã lấy về/chia sẻ sang ứng dụng khác là bản sao do người dùng chủ động xuất, không thể thu hồi bản sao đó bằng đổi cờ server.

App chỉ lấy nhị phân tối đa10MB qua client dùng chung, giữ xử lý401/timeout/cancel và chặn redirect. Browser tải Blob URL cục bộ; native dùng thư mục cache riêng của app và mở share sheet hệ thống. Dọn file tạm sau khi share, khi đổi phiên/logout và lần mở app tiếp theo; không ghi vào thư mục công khai tự động. Định dạng được giới hạn bởi upload hiện hành. Native share sheet/back/keyboard vẫn phải nghiệm thu trên thiết bị thật ở G10.

## Hỗ trợ: lifecycle và phân quyền

OPEN → IN_PROGRESS → RESOLVED → CLOSED. Phản hồi công khai đầu tiên của trung tâm chuyển OPEN sang IN_PROGRESS; ghi chú nội bộ không tự đổi trạng thái. Student trả lời RESOLVED sẽ mở lại OPEN. Quản lý có thể đưa RESOLVED về IN_PROGRESS; student có thể đóng yêu cầu chưa đóng. CLOSED không thêm tin/ghi chú hay đổi trạng thái; cần tạo yêu cầu mới.

Các loại PROFILE/GRADE/ATTENDANCE/PAYMENT/LEAVE/MAKEUP/OTHER. Không có attachment ở bản đầu. Liên kết tùy chọn hỗ trợ PROFILE, ENROLLMENT, SESSION đã kiểm sở hữu. PAYMENT có thể gửi theo đăng ký hoặc không liên kết; chưa nhận trực tiếp TUITION/RECEIPT khi API tài chính sở hữu chưa triển khai G7, không đoán ID hoặc mở API quản trị cho student.

branchId/studentId luôn do server xác định theo người gửi và tài nguyên đã kiểm quyền; bỏ qua giả mạo trong body/header. Branch xử lý được ghi tại lúc mở yêu cầu. Nhân sự đọc/ghi theo phân công branch hiện tại; teacher và student không gọi endpoint xử lý. Student chỉ thấy yêu cầu của chính mình, người gửi Bạn/Trung tâm và tin công khai; không có actorId, actorName, internalNote, lý do xử lý nội bộ hoặc số lượng ghi chú nội bộ.

Mỗi command có UUID và version. Message/state khóa request, kiểm version và ghi operation/message/trạng thái trong cùng transaction; retry cùng intent trả trạng thái hiện tại, không gửi trùng hoặc áp lại trạng thái cũ. UUID dùng cho intent khác bị409; UUID của người khác không tra cứu được. Hai phản hồi cùng version chỉ một commit. App/web giữ intent khi chưa rõ phản hồi, cho gửi lại hoặc tra cứu. Intent trong bộ nhớ khi đang thao tác; sau khởi động lại dùng danh sách/chi tiết để kiểm tra lịch sử trước khi tạo yêu cầu mới. Không có offline queue giả.

Yêu cầu không tự sửa hồ sơ, điểm, điểm danh, đăng ký hoặc học phí. Nhân sự xử lý nghiệp vụ gốc ở màn hình tương ứng rồi phản hồi; ticket chỉ ghi trao đổi và trạng thái.

## API đã triển khai

Thêm prefix `/api/v1`, bearer hợp lệ. Page mặc định0, size20, tối đa100.

| Method | Path | Quyền / dữ liệu |
|---|---|---|
| GET | `/academic/student/me/document-scopes` | STUDENT; enrollmentId?; scope đăng ký đủ điều kiện, không roster |
| GET | `/teaching/student/me/documents` | STUDENT; enrollmentId?,page,size |
| GET | `/teaching/student/me/documents/{id}/download` | STUDENT sở hữu; attachment, kiểm quyền hiện hành |
| PATCH | `/teaching/documents/{id}/student-sharing` | SA/A/T sở hữu; version,sharedToStudents,reason3..500 |
| GET | `/academic/student/me/support-requests` | STUDENT; page,size,status?,category? |
| POST | `/academic/student/me/support-requests` | STUDENT; requestId,category,subject5..120,description10..2000,resourceType?,resourceId?;201 |
| GET | `/academic/student/me/support-requests/{id}` | STUDENT sở hữu; request + messages công khai |
| POST | `/academic/student/me/support-requests/{id}/messages` | STUDENT sở hữu; requestId,version,message1..2000 |
| POST | `/academic/student/me/support-requests/{id}/close` | STUDENT sở hữu; requestId,version |
| GET | `/academic/student/me/support-requests/operations/{requestId}` | STUDENT đúng người gửi command; phục hồi |
| GET | `/academic/support-requests` | SA/A/S; branchId?,page,size,status?,category? |
| GET | `/academic/support-requests/{id}` | SA/A/S đúng branch; có ghi chú nội bộ |
| POST | `/academic/support-requests/{id}/messages` | SA/A/S đúng branch; requestId,version,message,internalNote |
| PATCH | `/academic/support-requests/{id}/status` | SA/A/S đúng branch; requestId,version,status,reason1..500 |
| GET | `/academic/support-requests/operations/{requestId}` | Nhân sự đúng người gửi, còn quyền branch |

Document projection: id,title,description,filename,mimeType,size,revision,sharedAt. Student ticket: request{id,category,subject,status,branchId,resourceType,resourceId,createdAt,updatedAt,version; studentId/studentName=null}, messages{id,sender,message,createdAt}. Không trả entity SupportMessage trực tiếp.

15API mới; tổng hiện tại207: Auth24, Academic134, Teaching37, Finance12. API thiết kế trong G1 ngoài nhóm đã hiện thực vẫn là dự kiến.

## Kiểm chứng local

- 407 tests backend:Auth61, Academic200, Teaching95, Finance43, Gateway8; không failure/error/skip. 43 tests app,96 tests web. TypeScript, web lint/build và Android Hermes export đạt.
-MySQL riêng: V014/V015/V016 replay hai lần; prod/validate. 63 kiểm tra setup/G4 +56 kiểm tra API G6;6 kiểm tra toàn vẹn SQL. Upload/duyệt/chia sẻ/tải và commands hỗ trợ chạy API thật; enrollment/session fixtures seed SQL riêng, không tuyên bố kiểm lại writer các nghiệp vụ đó.
- 12 luồng UI Edge với API/MySQL thật: tải file thật, deep link qua login, chọn loại native, tạo/trả lời ticket, chặn ticket người khác, staff ghi chú riêng và trả lời công khai, student đóng, admin thu hồi/chia sẻ, teacher không có menu hỗ trợ. Đọc CAPTCHA canvas chỉ phục vụ automation login web, không phải kiểm thử CAPTCHA.
- Ảnh: [tài liệu](figures/phase6-documents.png), [yêu cầu trên app](figures/phase6-student-ticket.png), [xử lý trên web](figures/phase6-web-support.png), [chia sẻ](figures/phase6-web-sharing.png). Snapshot UI có tên/nội dung fixture kiểm thử, không dùng làm dữ liệu demo sản phẩm.

Con trỏ evidence riêng `storage/student-phase6-latest.txt`, báo cáo summary.json/manifest hash5JAR và source/config. Không thay đổi database nguồn hoặc ZIP release cũ. Chưa kiểm chứng thiết bị Android/iOS thật hoặc production; web còn cảnh báo chunk lớn như trước, không tuyên bố Expo Doctor đạt. Các process kiểm thử riêng được dừng sau nghiệm thu.

Tái tạo: prepare.cjs `--phase=6` → Maven package → runtime.ps1 `-Action start -Phase 6` → phase4/live-api.cjs `--phase=6` → phase6/live-api.cjs → phase6/start-ui.ps1 → phase6/live-ui.cjs → verify-sql.cjs → summarize.cjs. Chuẩn bị các log phase6 app/typecheck/test/export và web/lint/test/build trước summarize. Mỗi lần seed cần prepare run mới. Dừng bằng runtime.ps1 `-Action stop -Phase 6` và phase6/stop-ui.ps1. Không chạy scripts fixture lên database nguồn.

Tiếp theo G7: học phí, công nợ và phiếu thu cá nhân theo billing root. Online payment/push thuộc G8/G9.
