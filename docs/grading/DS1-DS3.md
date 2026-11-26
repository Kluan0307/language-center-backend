# Điểm số — ĐS1, ĐS2, ĐS3

Phần tính điểm, IELTS/TOEIC và duyệt/công bố đã được bổ sung tiếp tại [ĐS4–ĐS6](DS4-DS6.md). Các giới hạn giai đoạn dưới đây là trạng thái tại mốc ĐS1–ĐS3.

Ngày cập nhật: 25/11/2026. Phạm vi triển khai: teaching-service, web quản lý và schema Docker local. Không thay đổi database MySQL riêng chạy trên Windows.

## ĐS1 — Kết quả rà soát

| Phần hiện có | Đánh giá và quyết định |
|---|---|
| `exams`, `grades` | Một điểm số bắt buộc/bài/học viên, thang tối đa và trọng số gợi ý; giữ nguyên cho dữ liệu cũ. |
| Danh sách dự thi | Đã lấy theo ngày thi, có chốt danh sách; tiếp tục nguyên tắc này cho đánh giá mới. |
| Quy tắc tổng kết lớp | Đã có `evaluation_policies` và các phiên bản theo lớp, nhưng không có thư viện quy chế theo khóa học hoặc kết quả nhận xét. Giữ lại cho lớp cũ. |
| Công bố điểm | Đã có bản chụp kết quả, phiên bản, thu hồi và thông báo; không sửa luồng này trong ĐS1–ĐS3. |
| Kết quả chính thức | Đã có khóa nguồn và mở lại có lý do; không đưa điểm nháp mới vào công thức cũ. |
| Đánh giá đầu vào | Thuộc Academic, phục vụ xếp lớp; không trộn với điểm học tập. |
| Web/app học viên | Web có nhập điểm/công bố/tổng kết cũ; app chỉ đọc kết quả đã công bố. Điểm mới vẫn là bản nháp, chưa gửi sang app. |

Số lượng dữ liệu thực tế: [inventory](ds1-inventory.json). Không tự phân loại điểm cũ thành band IELTS hoặc điểm TOEIC. Một tên khóa IELTS không chứng minh mọi bài kiểm tra trong lớp đều chấm theo band.

## Quy tắc chuyển tiếp

1. Không sửa cột `grades.score` thành nullable, không ghi lại điểm cũ, không tính lại kết quả đã công bố.
2. Lớp đã có bài kiểm tra cũ chưa được gắn quy chế mới. Cần một đợt đối chiếu/chuyển đổi riêng trong ĐS8, có mapping và xác nhận.
3. Lớp chưa có bài kiểm tra cũ được quản lý chủ động áp dụng quy chế. Khi tạo bài đánh giá đầu tiên, bản chốt của lớp bị khóa. Không cập nhật ngược từ mẫu mới hoặc mặc định khóa học.
4. Mặc định khóa học được quản lý theo `courseId + branchId`, vì khóa học dùng chung có thể được triển khai tại nhiều chi nhánh. Đây là lựa chọn mặc định, không tự áp dụng hồi tố. Dùng “Chọn từ mặc định khóa học”, kiểm tra rồi “Áp dụng cho lớp”.
5. Bài/điểm mới nằm trong module `grading`; lớp không dùng đồng thời công thức tổng kết cũ và quy chế mới. Các trang tổng kết cũ trả thông báo nghiệp vụ nếu lớp đã gắn quy chế mới.

## ĐS2 — Quy chế có phiên bản

| Mẫu | Thành phần ban đầu | Ràng buộc |
|---|---|---|
| Nhận xét trẻ nhỏ | Nghe hiểu, phản xạ nói, từ vựng, tham gia | Mức Cần hỗ trợ / Đang phát triển / Đạt yêu cầu; không nhập điểm số. |
| Điểm nội bộ | Bài tập 10%, định kỳ 20%, giữa kỳ 30%, cuối kỳ 40% | Tổng trọng số 100%; thang mặc định 0–10, ngưỡng tham khảo 60%, chuyên cần 80%. Không có hiệu lực đến khi quản lý áp dụng. |
| IELTS thi thử | Listening, Reading, Writing, Speaking | Thang nhập 0–9, bước 0,5; không trộn trọng số nội bộ. Chưa tính overall trong giai đoạn này. |
| TOEIC L&R thi thử | Listening, Reading | Điểm từng phần 5–495, bước 5. Nhập điểm quy đổi đã xác định, không nhập số câu đúng vào trường này. Chưa xây bảng quy đổi/tổng điểm ở ĐS1–ĐS3. |

Superadmin tạo phiên bản mới từ mẫu, không sửa phiên bản cũ. Quản lý chọn rõ phiên bản để áp dụng. Lớp lưu bản sao đặc tả độc lập, ID mẫu và thời điểm áp dụng. Lịch sử ghi trước/sau và lý do.

Các nhóm mẫu giáo/tiểu học/THCS/người lớn là phân loại khóa học; chúng không tự quyết định công thức. Quản lý chọn quy chế phù hợp mục tiêu thực tế. Không tự suy ra trình độ CEFR hoặc chứng chỉ từ điểm tổng kết.

## ĐS3 — Bài đánh giá và kết quả nháp

- Bài nội bộ thuộc một thành phần quy chế; quy chế kỹ năng/nhận xét cho phép chọn nhiều thành phần trong cùng bài.
- Ngày tương lai chỉ xem danh sách, chưa được nhập kết quả.
- Lần lưu đầu chốt danh sách học viên/đăng ký tại ngày đánh giá. Học viên chuyển lớp về sau không bị mất lịch sử bài đã làm.
- Mỗi kết quả thuộc một bài, một học viên và một thành phần. Chưa có bản ghi nghĩa là **Chưa nhập**; không coi là 0.
- Trạng thái: Đã đánh giá, Vắng, Được miễn, Chưa nộp. Ba trạng thái sau không được kèm điểm hoặc mức nhận xét, phải có ghi chú.
- Nhận xét trẻ nhỏ cần mức đánh giá và nội dung cụ thể. Điểm số phải đúng min/max/bước của quy chế.
- Lưu nhiều dòng trong một transaction: một dòng sai thì toàn bộ lần lưu thất bại. Phiên bản từng kết quả ngăn ghi đè thay đổi của người khác; lỗi 409 yêu cầu tải lại.
- Mọi lần lưu là bản nháp và có lịch sử trước/sau. ĐS6 mới bổ sung duyệt/công bố mới. App hiện tại không được đọc dữ liệu nháp này.

Giới hạn giai đoạn: chưa có sửa/xóa bài mới, thi lại, tổng hợp nhiều bài trong cùng thành phần, xét đạt, IELTS overall, TOEIC quy đổi hoặc công bố điểm mới. Không dùng kết quả nháp mới để xét hoàn thành khóa.

## Phân quyền

| Thao tác | Superadmin | Admin | Teacher | Staff/Student |
|---|---|---|---|---|
| Xem mẫu quy chế | Có | Có | Có | Không |
| Tạo phiên bản mẫu | Có | Không | Không | Không |
| Đặt mặc định khóa học, áp dụng lớp | Có | Chi nhánh được giao | Không | Không |
| Tạo bài, nhập kết quả nháp | Có | Chi nhánh được giao | Lớp được phân công | Không |
| Xem kết quả/lịch sử nháp | Có | Chi nhánh được giao | Lớp được phân công | Không |

Giáo viên không được dùng ID lớp khác để vượt quyền. Lớp đã đóng không được sửa dữ liệu đánh giá. Quyền nháp mới này không thay đổi quyền sửa điểm của luồng cũ.

## Cách sử dụng

Web http://localhost:15173 → Bảng điểm → chọn lớp → **Đánh giá theo quy chế**.

1. Admin/Superadmin chọn mẫu và phiên bản, nhập lý do, bấm **Áp dụng cho lớp**. Với lớp có dữ liệu kiểm tra cũ, tiếp tục dùng tab cũ.
2. Có thể lưu/chọn mặc định khóa học tại chi nhánh. Thay mặc định không sửa lớp đã áp dụng.
3. Tạo bài đánh giá, chọn ngày và thành phần. Hành động này chốt phiên bản quy chế của lớp.
4. Chọn bài, bấm **Nhập kết quả** ở dòng học viên, nhập điểm hoặc nhận xét/trạng thái, lưu bản nháp.
5. Xem lịch sử trong cùng khu vực. Học viên chưa thấy điểm mới cho đến khi có luồng công bố ở ĐS6.

## API mới

Base: `/api/v1/teaching/grading`. Tất cả yêu cầu cần đăng nhập; các quyền bảng trên được kiểm tra cả ở controller và service.

| Method | Đường dẫn | Chức năng |
|---|---|---|
| GET | `/templates` | Danh sách mẫu và phiên bản |
| POST | `/templates` | Tạo phiên bản tiếp theo, có `expectedRevision` |
| GET | `/course-default?courseId=&branchId=` | Mặc định khóa học tại chi nhánh |
| PUT | `/course-default` | Cập nhật mặc định có phiên bản/lý do |
| GET | `/classes/{classId}/binding` | Bản chốt quy chế của lớp; rỗng nếu chưa áp dụng |
| PUT | `/classes/{classId}/binding` | Áp dụng/chọn lại trước khi tạo bài; có `expectedRevision` |
| GET | `/classes/{classId}/activities` | Bài đánh giá của lớp |
| POST | `/classes/{classId}/activities` | Tạo bài và khóa quy chế lớp |
| GET | `/classes/{classId}/activities/{id}/sheet` | Danh sách và kết quả nháp |
| PUT | `/classes/{classId}/activities/{id}/marks` | Lưu lô kết quả, `expectedVersion` từng dòng, lý do |
| GET | `/classes/{classId}/history` | 100 sự kiện gần nhất của lớp |

Đối với dòng chưa có kết quả, `expectedVersion=null`; dòng đã có dùng `version` từ sheet. `score=0` là dữ liệu hợp lệ nếu thang cho phép. Không gửi điểm hoặc mức nhận xét cho Vắng/Miễn/Chưa nộp.

## Database và vận hành

Migration: `docs/migrations/V030__grading_schemes.sql`, chỉ thêm 7 bảng. Fresh Docker dùng `deployment/docker/mysql/004-grading-schemes.sql`. Teaching khởi tạo 4 mẫu tham khảo nếu chưa có; không tự gắn lớp hoặc tạo điểm.

Backup trước migration: `ops-1795601933773-2a3fed` (88 bảng). Backup trước cập nhật image: `ops-1795602035232-efd552` (95 bảng). Migration đối chiếu checksum xác nhận toàn bộ bảng cũ không thay đổi. Giữ backup local, không dùng restore toàn bộ chỉ để gỡ một cấu hình.

Để chạy backend ngoài Docker, áp dụng V030 vào đúng database đang sử dụng trước khi khởi động bản teaching-service mới. Database Windows chưa được áp dụng tự động trong tác vụ này.

## Kiểm chứng và phần tiếp theo

Đã hoàn tất nghiệm thu phần còn dang dở: 9 kiểm tra tích hợp MySQL/API/Chrome đều đạt, thao tác Drawer Ant Design lưu được điểm nháp thực tế. Đã chụp desktop/mobile, xác nhận không thay đổi điểm ở database chính và đã dọn database/container kiểm thử. Hệ thống chính có đủ 9 container khỏe. Inventory ghi nhận 36 bài kiểm tra, 432 điểm cũ và 432 điểm đã công bố; không tự chuyển đổi dữ liệu này.

- Teaching-service: toàn bộ kiểm thử hồi quy và kiểm thử quy chế H2; log local `storage/grading-regression.log`, `storage/grading-atomic-tests.log`.
- Frontend: TypeScript/Vite build, theo cấu trúc Ant Design + styled-components hiện có.
- MySQL/API/Chrome trên database riêng và nguồn Academic tổng hợp: [báo cáo](ds123-live.json), [desktop](screenshots/ds123-1440.png), [mobile](screenshots/ds123-390.png). Không tạo bài/điểm thử trong database chính.

Tiếp theo **ĐS4**: chốt cách tổng hợp nhiều bài trong mỗi thành phần, xử lý miễn/vắng/thi lại, tính điểm có trọng số, xét điều kiện và giải thích kết quả. **ĐS5**: hoàn thiện IELTS/TOEIC. **ĐS6**: duyệt/công bố. **ĐS7**: trải nghiệm đầy đủ web/app; **ĐS8**: chuyển điểm cũ có kiểm soát.
