# G3–G4 — API và web quản lý phân loại khóa

Ngày 16/11/2026. Phạm vi local, không deploy.

## Đã triển khai

- GET `/api/v1/academic/courses/classification-catalog` cung cấp các chương trình chuẩn và đối tượng từ backend.
- POST/PUT `/courses` yêu cầu `programId`, `audienceIds` không trùng/không rỗng và `classificationRevision`. Không nhận loại mới dưới dạng type tự do. Trường type cũ được bảo toàn khi chỉnh sửa, không còn là nguồn nhập liệu của giao diện.
- Course DTO bổ sung programId, audiences, classificationRevision và classificationComplete. Các trường đọc cũ vẫn được giữ; ứng dụng đọc dữ liệu cũ không phải chuyển đổi ngay.
- Tạo khóa mới phải chọn chương trình chuẩn đang hoạt động và ít nhất một đối tượng hợp lệ. Không tự gán theo tên/tuổi.
- Dữ liệu cũ thiếu phân loại vẫn xem được; chỉnh sửa qua API mới cần quản lý xác nhận đủ các trường.
- Superadmin/Admin quản lý phân loại theo quyền khóa dùng chung/chi nhánh hiện hành. Staff không được thay chương trình hoặc đối tượng. Staff chỉ sửa các thuộc tính trước đây được phép khi phân loại đã đầy đủ và giữ nguyên.
- Đối tượng ngừng dùng không được chọn mới; có thể giữ lựa chọn cũ khi sửa thông tin khác.
- Phiên bản cũ bị từ chối với 409. Các thay đổi thuộc tính và phân loại lưu cùng transaction; không lưu nửa chừng khi validation thất bại.
- Khóa đã có lớp không được đổi sang chương trình khác. Chương trình mới phải khớp chính sách đầu vào hiện hành; xác nhận policy mới cũng phải khớp chương trình khóa. Không sửa các snapshot lịch sử.
- CMS khóa không được khai báo chương trình khác nguồn gốc. Catalog công khai không tiếp tục hiển thị metadata đã lệch chương trình.
- Bộ lọc nội bộ `/courses` thêm programId/audienceId; dùng subquery để tránh đếm hoặc trả khóa trùng khi có nhiều đối tượng. Collection audiences có batch loading.
- API tạo chương trình chỉ nhận GE/COMMUNICATION/IELTS/TOEIC; mã của bản ghi cũ vẫn bất biến. Luồng trình độ/đánh giá lịch sử tiếp tục dùng các chương trình đã lưu.

## Giao diện

- Quản lý khóa học: chương trình dạng Select, đối tượng dạng chọn nhiều; bỏ ô loại tự do.
- Danh sách hiển thị chương trình, các tag đối tượng và nhãn **Chưa xác nhận đủ phân loại** nếu thiếu.
- Bộ lọc chương trình/đối tượng lấy từ API; lưu và khôi phục theo URL, kết hợp tìm kiếm/chi nhánh/phân trang hiện có.
- Staff được thông báo nhờ quản lý xác nhận nếu khóa cũ thiếu đối tượng.
- Trong quản lý chương trình, tạo mới chọn mã cố định thay cho tự gõ. Trình độ vẫn nhập mã riêng theo nghiệp vụ cũ.

## Cách dùng local

1. V027 đã áp dụng local, thêm `courses.classification_revision` với mặc định 0, không đổi chương trình/đối tượng hiện tại. Máy khác cần chạy V026 trước V027; dev ddl-auto=update có thể bổ sung cột, production validate cần migration.
2. Khởi động lại academic-service và web quản lý cùng nhau để nạp hợp đồng API mới. FE cũ khi ghi khóa mà thiếu phân loại sẽ nhận 400; không âm thầm bỏ qua dữ liệu.
3. Đăng nhập Admin/Superadmin → Quản lý khóa học → Sửa từng khóa → chọn đúng nhóm đối tượng → Lưu.
4. Có thể chọn nhiều nhóm nếu thực sự phù hợp. Không gán đồng loạt cho 16 khóa vì chưa có thông tin được xác nhận.
5. Đối chiếu chương trình và các tag trên danh sách, thử lọc rồi tải lại trang.

Hiện **16 khóa vẫn chờ xác nhận đối tượng**. Đợt code này không tự phân nhóm trẻ em/người lớn hoặc sửa học phí, lớp, tài khoản.

Ví dụ phần thuộc tính mới gửi kèm payload khóa hiện hành:

```json
{
  "programId": 1,
  "audienceIds": [2, 3],
  "classificationRevision": 0
}
```

ID chỉ minh họa: dùng ID từ catalog và revision từ DTO mới nhất, không giả định ID theo mã danh mục. Sau lần thay đổi phân loại revision tăng; sửa tiếp phải dùng revision mới.

## Kiểm chứng

51 test backend đạt trong các suite CourseClassificationWorkflowTest (8), CourseWriteRulesTest (3), AdmissionPolicyWorkflowTest (12), WebsiteCatalogTest (12), WebsiteIntakeWorkflowTest (13), AcademicServiceApplicationTests (1), ListQueryBudgetTest (2).

Build web quản lý đạt. Có cảnh báo bundle lớn đã có trước. Chưa thực hiện thao tác browser đầy đủ hoặc ghi thử phân loại vào MySQL vận hành; các tình huống ghi/rollback kiểm thử bằng H2 riêng. V027 bổ sung schema local không gán đối tượng hoặc tạo khóa thử.

G5–G8 còn lại: website/CMS hiển thị trục đối tượng mới, mobile, chatbot/thống kê và nghiệm thu toàn luồng. Không coi cập nhật giao diện quản lý này là đã hoàn thành các phần đó.
