# G8 — Nghiệm thu phân loại khóa học

Ngày nghiệm thu tự động: 16/11/2026. Phạm vi local; không deploy, không gọi model AI thật, không thay đổi dữ liệu nghiệp vụ MySQL.

## Các lỗi đã sửa

1. Web quản lý không còn lấy `type` cũ làm tên chương trình khi thiếu danh mục. Danh sách và chi tiết hiển thị “Chưa xác nhận chương trình”; chi tiết dùng nhãn “Chương trình”. Thông tin cũ trong phần điều kiện đầu vào vẫn là dữ liệu tham khảo riêng.
2. Website chi tiết lớp lấy tên/mã chương trình từ chính sách gắn với lớp, cùng quy tắc với API học viên. Đổi tên chương trình hiện tại không viết lại nhãn chương trình của lớp lịch sử.
3. API danh mục quản lý loại bỏ mã đối tượng ngoài năm mã chuẩn, tránh cho người dùng chọn một giá trị mà API lưu sẽ từ chối. Danh mục inactive đã có tham chiếu vẫn được bảo toàn theo quy tắc G3.
4. Kiểm thử sidebar được cập nhật theo chức năng CMS đã có từ trước: `/website-content` chỉ xuất hiện với SUPER_ADMIN/ADMIN. Không mở thêm quyền cho STAFF/TEACHER.

## Kết quả

| Phạm vi | Kết quả | Điều được kiểm chứng |
|---|---|---|
| Backend academic | 124 test đạt | Quy tắc phân loại, chính sách đầu vào, student ownership, public catalog, phân quyền, thống kê, query budget |
| Kiểm thử tích hợp G8 trong bộ backend | 3 test đạt | Cùng khóa được đọc qua quản lý, website, API app và assistant; snapshot lớp; không suy từ type; danh mục chuẩn |
| Web quản lý | Build và 126 test đạt | TypeScript, bản build, hồi quy logic/quyền/sidebar |
| Chrome web quản lý | 11 kiểm tra đạt | Phân loại nhiều đối tượng không lặp dòng, bộ lọc bằng ID chuẩn, biểu mẫu chuẩn, giữ giới hạn trang theo role |
| Website | Production build và 38 kiểm tra Chrome đạt | Lọc chương trình/đối tượng, tư vấn giữ ngữ cảnh, kích thước màn hình, lỗi backend, không lộ khóa riêng trong bundle |
| Mobile | TypeScript và 108 test đạt | Hồi quy logic app ở mức mã nguồn; chưa thử APK mới trên thiết bị |
| Quy tắc ánh xạ G2 | 4 test đạt | Không đoán từ tên/tuổi; chặn mâu thuẫn; bảo toàn dữ liệu đã xác nhận |
| MySQL local | Audit chỉ đọc đạt kiểm tra toàn vẹn | 16/16 khóa có chương trình; không quan hệ mồ côi, trùng nhóm hoặc lệch chương trình policy/website |

Tổng chạy mới trong G8: **362 test và 49 kiểm tra trình duyệt đạt**. Ba test tích hợp G8 đã nằm trong 124 test backend, không cộng thêm lần nữa. Bộ assistant offline 346 test từ G7 không được cộng vào số chạy mới của G8; mã assistant không thay đổi ở G8.

Kiểm thử tích hợp backend dùng một database H2 riêng, tạo khóa qua controller quản lý thật và chuẩn bị fixture lớp/chính sách/ghi danh để đối chiếu các API đọc trên cùng dữ liệu. Đây không phải kiểm thử thao tác đăng nhập, tạo lớp và thu phí trên database thật. Hai bộ kiểm tra Chrome dùng HTTP fixture riêng, không gọi MySQL nghiệp vụ.

## Quyết định với `type`

**Giữ cột và hợp đồng đọc cũ; không tạo migration xóa cột trong G8.** Các phụ thuộc còn tồn tại:

- `CourseDto.type`, tham số lọc HTTP cũ và adapter/service nội bộ còn phục vụ tương thích.
- `AdmissionDto.CourseView.legacyType` vẫn dùng cho thông tin tham khảo của khóa cũ.
- Script ánh xạ và dữ liệu 16 khóa vẫn giữ giá trị cũ để đối chiếu; không lấy nó làm nguồn phân loại mới.
- Không thể chứng minh mọi client đã cài đặt đều ngừng đọc thuộc tính cũ chỉ bằng kiểm thử source hiện tại.

MCP không còn nhận `type`; biểu mẫu quản lý mới không nhập `type`. Trước khi xóa cột cần ngừng hỗ trợ API lọc cũ theo một phiên bản rõ ràng, cập nhật client có liên quan, bảo toàn dữ liệu đối chiếu và chạy lại kiểm thử. Không xóa chỉ vì giao diện chính đã chuyển sang danh mục.

## Dữ liệu cần quản lý xác nhận

Audit tại `g8-local-audit.json` xác nhận **16 khóa chưa có đối tượng**. Điều này là thiếu phân loại nghiệp vụ, không phải hỏng quan hệ database. Các mã chương trình và nhóm đối tượng đã có đầy đủ.

1. Đăng nhập SUPER_ADMIN hoặc ADMIN của chi nhánh, mở **Khóa học**.
2. Mở từng khóa có nhãn **Chưa xác nhận đủ phân loại**, chọn **Sửa**.
3. Đối chiếu chương trình hiện tại và chọn một hoặc nhiều **Đối tượng phù hợp** dựa trên nội dung, mục tiêu, độ tuổi phục vụ thực tế đã xác nhận. Không tự đổi chương trình để khớp nhóm tuổi.
4. Lưu và kiểm tra lại tag, bộ lọc và số khóa. Nếu gặp thông báo phiên bản cũ, tải lại bản ghi trước khi sửa tiếp.
5. Với khóa công khai, xác nhận trên website theo nhóm. Với khóa chưa xuất bản, kiểm tra qua quản lý/chatbot theo quyền; khóa không tự xuất hiện trên website chỉ vì đã phân loại.
6. Chạy audit lại. Mục tiêu `coursesWithoutAudiences=0` sau khi đã xác nhận hết các khóa đang giữ trong hệ thống.

G8 không tự chọn đối tượng cho 16 khóa và không xuất bản nội dung đang riêng tư.

## Chạy và kiểm tra lại

Khởi động lại `academic-service`, cập nhật/reload web quản lý. Website tiếp tục đọc backend; không có biến môi trường hay migration mới. Nếu chưa nạp G7, khởi động lại `assistant-service` và mở hội thoại mới.

Từ thư mục backend:

```powershell
node scripts\identity\audit-course-classification.cjs
node --test scripts\identity\course-classification-rules.test.cjs
mvn -f academic-service/pom.xml test "-Dtest=CourseClassificationAcceptanceTest,CourseClassificationWorkflowTest,CourseClassificationViewTest,WebsiteCatalogTest,WebsiteSecurityTest,WebsiteIntakeWorkflowTest,StudentLearningTest,StudentRouteSecurityTest,AssistantQueryTest,AdmissionPolicyWorkflowTest,AdmissionTransactionTest,ListQueryBudgetTest,StatsQueryTest"
node scripts\identity\verify-classification-browser.cjs
node scripts\website\verify-l67.cjs
```

Các script trình duyệt cần Chrome trên Windows, dependencies đã cài và các cổng fixture rảnh. `verify-l67.cjs` cần build website trước (`npm run build` tại thư mục website). Tên script giữ từ L6/L7 nhưng hiện bao gồm kiểm tra phân loại G5/G8. Không trỏ fixture vào backend đang chạy của người dùng.

Ở thư mục web quản lý: `npm run build`, `npm test`. Ở thư mục app: `npm run typecheck`, `npm test`.

## Trạng thái bàn giao

Phần code, sửa lỗi và nghiệm thu tự động G8 đã hoàn thành. **Chưa đóng nghiệm thu thực tế toàn bộ** vì còn:

- Quản lý xác nhận đối tượng của 16 khóa thật.
- Thử APK trên điện thoại với tài khoản học viên: xem lớp, nhãn chương trình/nhóm, lịch, điểm và học phí; chưa build APK mới trong G8.
- Thử các câu G7 với model thật và dữ liệu đã phân loại; kiểm tra theo role/chi nhánh. Các kiểm thử tự động không chứng minh chất lượng trả lời thực tế của Gemini.

Không cần deploy để hoàn thành các bước xác nhận local này. Bản build CMS vẫn có cảnh báo kích thước bundle; đây là việc tối ưu tải trang riêng, không phải lỗi build hay lỗi phân loại.
