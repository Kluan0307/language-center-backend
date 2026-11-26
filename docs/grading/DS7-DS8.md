# ĐS7–ĐS8: sử dụng, đối chiếu dữ liệu cũ và kết thúc khóa

## Giao diện và thao tác

Trong **Quản lý điểm → chọn lớp → Đánh giá theo quy chế** có ba tab:

1. **Quy chế lớp**: phiên bản và thành phần đang áp dụng, quy chế mặc định cho khóa học tại chi nhánh. Phiên bản đã chốt không đổi ngầm khi sửa mẫu.
2. **Nhập kết quả**: chọn bài, tìm học viên, theo dõi số kết quả còn thiếu. Chỉ sửa tên/ngày bài trước khi có điểm và chốt danh sách. Bài sao chép từ dữ liệu cũ giữ nguyên dấu vết nguồn.
3. **Tổng hợp & công bố**: tìm/lọc học viên, xem cách tính và từng lần thi lại, xuất CSV nháp hoặc bản công bố, gửi duyệt → người quản lý khác duyệt → công bố. Mở lại/thu hồi có lý do.

CSV dùng UTF-8 BOM, trích dẫn ô và chặn chuỗi bị hiểu là công thức. Bản xuất đã công bố lấy từ snapshot được duyệt; không dựng lại bằng điểm hiện tại. Bản công bố trước ĐS7 không có lịch sử chi tiết thì chỉ hiện thông tin đã lưu, không bổ sung lịch sử từ nguồn có thể đã thay đổi.

Nhân viên xem bản công bố trong hồ sơ học tập, đúng chi nhánh và đăng ký được Academic xác minh. Giáo viên chỉ thao tác lớp được phân công. Học viên xem và tải/chia sẻ CSV của đăng ký thuộc mình trên mobile; API xác minh lại quyền khi tải. Nhận xét nhập ở quy chế mới là **nhận xét dành cho học viên**.

## Đối chiếu dữ liệu cũ

Lớp có bài kiểm tra cũ và chưa áp dụng quy chế mới sẽ có phần **Lớp có điểm theo cấu trúc cũ** ở tab Quy chế lớp.

1. Quản lý chọn phiên bản **điểm nội bộ** phù hợp thang gốc.
2. Ánh xạ **tất cả** bài cũ vào thành phần, ghi lý do và xem trước.
3. Kiểm tra điểm dự kiến và các lý do chặn. Chỉ cho sao chép khi cùng thang, điểm hợp lệ và xác minh được đăng ký tại ngày thi.
4. Tích xác nhận và sao chép thành bản nháp. Kiểm tra, bổ sung phần còn thiếu, gửi duyệt và công bố theo luồng mới.

Không tự lấy điểm tổng IELTS/TOEIC cũ để suy ra từng kỹ năng, không tự quy đổi thang, không tự nhận xét từ điểm số. Lớp không đủ nguồn đối chiếu tiếp tục giữ dữ liệu cũ cho tới khi quản lý quyết định phù hợp. Nếu cần quy chế nội bộ khác thang, SUPER_ADMIN tạo phiên bản mẫu phù hợp trước khi đối chiếu.

Điểm, bài thi và công bố cũ được giữ nguyên. Sau khi chuyển, sửa điểm/công bố mới thực hiện trong quy chế mới; công bố cũ vẫn được đọc hoặc thu hồi. Chỉ mang theo `studentComment`, không sao chép ghi chú nội bộ `comment` sang nội dung cho học viên. Nhật ký lưu người thực hiện, lý do, fingerprint nguồn và liên kết bài cũ–bài mới. Gửi lại cùng mã yêu cầu không tạo bản sao trùng.

**Không tự chuyển hàng loạt dữ liệu thật.** V032 chỉ bổ sung cấu trúc; việc chọn thành phần và xác nhận thay đổi cách tính thuộc bước nghiệp vụ có xem trước.

## Công bố điểm và hoàn thành học vụ

Hai bước tách biệt sau khi công bố:

1. Quản lý bấm **Kiểm tra điều kiện → Chốt kết quả chính thức**. Chỉ chốt khi nguồn hiện hành khớp bản công bố, đã đủ thời lượng/chuyên cần và không còn bài tương lai. Mỗi đăng ký được liên kết tới phiên bản công bố; toàn bộ batch cùng giao dịch và có chống lặp.
2. Bấm **Kiểm tra / hoàn thành** cho từng đăng ký, xem trạng thái hiện tại và xác nhận có lý do. Academic xác minh đúng kết quả, phiên bản và nguồn trước khi đổi trạng thái thành COMPLETED.

PASS/FAIL giữ ý nghĩa học thuật. Quan sát hoặc bài thi thử không đặt mục tiêu có kết quả chính thức ASSESSED (đã đánh giá), không bị gán PASS tùy tiện. COMPLETED xác nhận kết thúc quá trình học, không đổi công nợ và không cấp chứng chỉ IELTS/TOEIC. Nếu nguồn cần sửa sau chốt, mở lại/thu hồi bản công bố sẽ vô hiệu các kết quả chính thức hiện hành, giữ lịch sử và yêu cầu duyệt/chốt lại.

## API bổ sung

Các đường dẫn sau có tiền tố `/api/v1/teaching`:

| Phương thức | Đường dẫn | Quyền |
|---|---|---|
| GET | `/grading/classes/{id}/legacy/source` | SUPER_ADMIN/ADMIN đúng phạm vi |
| POST | `/grading/classes/{id}/legacy/preview` | SUPER_ADMIN/ADMIN |
| POST | `/grading/classes/{id}/legacy/apply` | SUPER_ADMIN/ADMIN |
| PATCH | `/grading/classes/{classId}/activities/{id}` | Quản lý/giáo viên được phân công, trước khi nhập điểm |
| POST | `/grading/classes/{id}/completion/preview` | SUPER_ADMIN/ADMIN |
| POST | `/grading/classes/{id}/completion/confirm` | SUPER_ADMIN/ADMIN |
| GET | `/grading/classes/{id}/export?published=true` | Quản lý/giáo viên được phân công |
| GET | `/students/{studentId}/enrollments/{enrollmentId}/grading-result` | Quản lý/nhân viên đúng phạm vi |
| GET | `/student/me/enrollments/{id}/grading-result/export` | STUDENT chính chủ |

`published=false` xuất bản tính nháp hiện tại, `true` xuất snapshot công bố. API hoàn thành trạng thái đăng ký vẫn là `PATCH /api/v1/academic/enrollments/{id}/status`, tham chiếu `finalResultId`, `finalResultRevision` và phiên bản đăng ký hiện hành.

## Cơ sở dữ liệu và vận hành

- Migration: `docs/migrations/V032__grading_transition.sql`.
- Docker mới: `deployment/docker/mysql/006-grading-transition.sql`.
- Docker hiện có: sao lưu rồi chạy `node scripts/docker/apply-grading-transition-migration.cjs`; script kiểm tra cột trước khi ALTER và so sánh dữ liệu cũ trước/sau.
- Bổ sung `grading_legacy_imports`, `grading_completion_batches`, `final_results.grading_release_id`, `grading_activities.version`; tổng schema hiện tại 99 bảng.
- Backup trước migration local: `ops-1795614773868-8b89c3` (97 bảng). Backup trước cập nhật image: `ops-1795614874630-02a2d9` (99 bảng).
- Cập nhật image Teaching, Academic và web-admin cùng nhau. Máy chạy Maven với database riêng cần áp dụng V032 trên database đó; thay đổi Docker không sửa MySQL Windows.
- Mobile đã cập nhật mã nguồn; APK cũ cần build/cài lại để có màn hình và nút xuất mới.

## Kiểm chứng

- Kết quả tổng hợp: `docs/grading/ds78-completion.json`.
- Kiểm thử MySQL/giao diện riêng: `docs/grading/ds78-live.json`, script `scripts/docker/verify-grading-transition-live.cjs`. Dữ liệu Academic của phép thử này là dữ liệu tổng hợp để kiểm tra hợp đồng API; không sửa hồ sơ/đăng ký thật.
- Academic `CompletionBridgeTest` kiểm tra ASSESSED chỉ hợp lệ với tham chiếu grading release, đúng nguồn và phiên bản. Hồi quy hiện có tiếp tục kiểm tra giao dịch hoàn thành học vụ.
- Kiểm thử mobile bằng TypeScript và bộ test tự động; chưa xác minh chia sẻ CSV trên Android vật lý trong phiên này.
