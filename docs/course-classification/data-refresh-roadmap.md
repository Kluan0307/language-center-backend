# D1–D8 — Làm mới dữ liệu và cập nhật website

Trạng thái ngày 17/11/2026: **D1–D8 đã hoàn thành phần code/local và nghiệm thu tự động**. Xem [bàn giao D1–D4](d1-d4-handoff.md) và [bàn giao D5–D8](d5-d8-handoff.md). Website đã có nội dung xuất bản và menu theo nhóm. APK thật, model AI thật và giao dịch VNPAY sandbox mới chưa thử lại; không deploy. Không xóa cột `type`.

## Mục tiêu và phạm vi đề xuất

Thay bộ dữ liệu nghiệp vụ cũ bằng một bộ mẫu nhất quán để kiểm tra web quản lý, app học viên, chatbot và website. Website chỉ hiển thị các bản ghi đã xuất bản và đủ điều kiện công khai.

Đề xuất giữ tài khoản, mật khẩu, hồ sơ nhân sự, cơ cấu công ty, bốn chi nhánh, phân công tài khoản–chi nhánh và cấu hình. Giữ các mã chương trình/đối tượng chuẩn; rà soát danh mục trình độ trước khi tái sử dụng. Không tái sử dụng lịch sử đăng nhập, hội thoại hoặc tác vụ nền đang tham chiếu dữ liệu bị xóa.

Thay dữ liệu học viên, khách tiềm năng, khóa/lớp, lịch, ghi danh, đánh giá, điểm, điểm danh, học phí, phiếu thu, đơn thanh toán, thông báo, hỗ trợ gắn với dữ liệu cũ và bản xuất bản website. Với tài khoản STUDENT giữ lại, lập ánh xạ rõ sang hồ sơ học viên mới. Hồ sơ có liên kết account_id duy nhất; không dò ghép bằng trùng tên.

Phạm vi giữ tài khoản/nhân sự/chi nhánh/phân quyền và thay dữ liệu nghiệp vụ đã được áp dụng theo yêu cầu thực hiện D1–D4. Cấu trúc bảng giữ nguyên.

## Phân loại và menu website

Chương trình: GE, COMMUNICATION, IELTS, TOEIC. Đối tượng: PRESCHOOL, PRIMARY, SECONDARY, HIGH_SCHOOL, ADULT. Mỗi khóa có một chương trình và một hoặc nhiều đối tượng; một khóa nhiều đối tượng vẫn là một bản ghi.

| Mục menu | Điều kiện truy vấn |
|---|---|
| Tiếng Anh Mẫu giáo | audience=PRESCHOOL |
| Tiếng Anh Tiểu học | audience=PRIMARY |
| Tiếng Anh THCS | audience=SECONDARY |
| Tiếng Anh Người lớn | audience=ADULT; dropdown có thể chia tổng quát/giao tiếp nếu đã xuất bản |
| IELTS | chương trình IELTS qua slug xuất bản hiện có; kết hợp được bộ lọc đối tượng |
| TOEIC | chương trình TOEIC qua slug xuất bản hiện có; kết hợp được bộ lọc đối tượng |
| Giới thiệu | Trung tâm, chi nhánh, giáo viên đã có hồ sơ công khai |
| Tư vấn ngay | Form tư vấn có ngữ cảnh chương trình/đối tượng/khóa/lớp |

THPT vẫn là nhóm chuẩn, truy cập được qua trang tất cả khóa và bộ lọc; có thể thêm mục menu khi có nhu cầu. Không xóa THPT chỉ vì ảnh tham khảo không có mục này. Ảnh dùng để tham khảo bố cục; màu sắc và logo theo thương hiệu hiện tại.

## D1 — Chốt phạm vi và thiết kế bộ mẫu

- Kiểm kê toàn bộ table, khóa ngoại và quan hệ logic xuyên service; đánh dấu từng bảng GIỮ/LÀM MỚI/DỌN THAM CHIẾU.
- Lập manifest tài khoản giữ lại, profile, chi nhánh và các mã nghiệp vụ. ID nội bộ vẫn theo schema; mã hiển thị ngắn và dễ đọc.
- Đề xuất quy mô: giữ bốn chi nhánh, hai admin phụ trách hai chi nhánh/người, tám staff; giữ nhân sự hiện có, bổ sung chỉ khi lịch mẫu yêu cầu. Mỗi chi nhánh khoảng 100 học viên, 20 lead, 8–10 lớp với sĩ số hợp lệ.
- Chuẩn bị khoảng 16–20 khóa phủ bốn chương trình và năm đối tượng. Có khóa nhiều đối tượng, không sao chép khóa chỉ để xuất hiện ở nhiều menu. Quyết định rõ khóa chung và khóa riêng chi nhánh.
- Chốt ma trận khóa–chương trình–đối tượng–trình độ–học phí–thời lượng–chi nhánh. Không suy đối tượng từ tên hay `type` của bộ cũ.

Đầu ra: danh sách table và ma trận dữ liệu duyệt được; chưa xóa database.

## D2 — Chuẩn bị công cụ reset và sao lưu

- Viết công cụ có chế độ dry-run, chỉ cho phép database local đã kiểm tra tên/host và liệt kê số bản ghi sắp thay.
- Backup dữ liệu và schema vào vùng lưu riêng, kiểm tra khả năng phục hồi trên database tạm.
- Dựng thứ tự xóa từ bảng phụ thuộc về bảng gốc. Ưu tiên DELETE có thứ tự và transaction cho dữ liệu InnoDB; không dùng TRUNCATE như thể có thể rollback. Không dựa vào tắt khóa ngoại để che lỗi liên kết.
- Xác định cách bảo toàn hoặc căn lại bộ sinh mã theo cơ chế dự án. Không ép mọi ID bắt đầu từ 1 hoặc tái dùng ID cũ khi còn tham chiếu ngoài database.
- Chuẩn bị dừng các service ghi dữ liệu và jobs thông báo/thanh toán khi chạy reset. Phiên và hội thoại cũ phải được hết hiệu lực trước khi truy cập dữ liệu mới.

Đầu ra: script reset, backup/restore và báo cáo thử phục hồi.

## D3 — Xây bộ seed đồng bộ

- Sinh dữ liệu theo manifest, dùng định danh xác định để ánh xạ quan hệ; chạy lại không chèn trùng hoặc cộng dồn.
- Khóa mới có program_id, đối tượng, classification_revision phù hợp và thời lượng rõ đơn vị; `type` để trống khi không cần tương thích.
- Trình độ theo từng chương trình, chính sách đầu vào và snapshot lớp được tạo nhất quán. Có các trường hợp OPEN_ENTRY và REQUIRES_ASSESSMENT; đánh giá đầu vào xác nhận phù hợp trước ghi danh ở trường hợp cần đánh giá.
- Tạo phòng, phân công giáo viên, lớp và buổi học không trùng phòng/giáo viên/học viên; tôn trọng chi nhánh, sức chứa và thời lượng. Có lớp đang học, sắp mở và đã kết thúc.
- Tạo học viên và ghi danh; liên kết tài khoản STUDENT được giữ với hồ sơ mới. Điểm, điểm danh và lịch chỉ xuất hiện trong thời gian học hợp lệ.
- Học phí khớp ghi danh; phiếu thu, hoàn/hủy và công nợ tuân theo luồng tài chính. Không tạo giao dịch VNPAY thành công giả dưới danh nghĩa giao dịch thật hoặc gọi nhà cung cấp để seed.
- Dùng API/service nghiệp vụ hoặc cơ chế seed nội bộ có kiểm tra bất biến; SQL trực tiếp chỉ dùng khi đã đối chiếu toàn bộ trạng thái dẫn xuất và side effects.
- Lead, lịch hẹn, điểm đã công bố và thông báo mẫu có ngữ cảnh hợp lý. Không gửi SMS, push hoặc email thật trong quá trình seed.

Đầu ra: seed và manifest quan hệ, tổng số dự kiến, ngày tham chiếu cùng múi giờ Asia/Ho_Chi_Minh.

## D4 — Diễn tập rồi áp dụng local

- Khôi phục bản sao vào database riêng; chạy reset + seed + audit trước.
- Kiểm tra số lượng, mã trùng, FK, liên kết học viên–account, số tiền, sĩ số, lịch và snapshot.
- Khi phạm vi D1 và báo cáo diễn tập đã được chấp thuận, tạo backup mới nhất, dừng jobs/service ghi và áp dụng trên database local.
- Nếu một bước lỗi, phục hồi theo phương án đã thử; không để các service tiếp tục ghi vào bộ dữ liệu nửa cũ nửa mới.
- Kiểm tra tài khoản được giữ và đăng nhập lại sau reset. Đối soát bộ sinh mã bằng thao tác tạo mới hợp lệ trên database diễn tập.

Đầu ra: database mới đầy đủ, báo cáo trước/sau, hướng dẫn phục hồi. Giữ nguyên cấu trúc bảng.

## D5 — Nội dung và xuất bản website

- Soạn nội dung demo rõ nguồn gốc cho chương trình, khóa, chi nhánh và giáo viên; không bịa chứng nhận, thành tích, cam kết đầu ra, hotline hoặc địa chỉ của một trung tâm có thật.
- Xuất bản theo phụ thuộc: chương trình và chi nhánh trước, khóa sau, lớp tuyển sinh sau cùng. Khóa chung có thể dùng ở nhiều chi nhánh nhưng không nhân bản để đếm số lượng.
- CMS lấy chương trình/đối tượng từ khóa gốc; không tạo thêm phân loại độc lập trong nội dung marketing.
- Website chỉ nhận giá được phép công khai, lớp còn tuyển, hồ sơ giáo viên được phép xuất bản. Dữ liệu học viên, điểm, công nợ và tài khoản luôn riêng tư.

Đầu ra: đủ khóa công khai để từng mục menu có kết quả thật từ backend.

## D6 — Menu và trang duyệt khóa

- Cập nhật menu theo bảng trên, đồng bộ logo/màu sắc; thiết kế desktop và menu mobile, hỗ trợ bàn phím và focus.
- Dùng các route/bộ lọc hiện có trước; chỉ thêm trang giới thiệu nhóm khi cần nội dung riêng. Không tạo một danh sách khóa hardcode khác nguồn CMS.
- Dropdown Người lớn, IELTS, TOEIC chỉ trỏ đến chương trình/khóa đã xuất bản; chương trình dùng slug công khai, không gán cứng ID database vào menu.
- Giữ được tổ hợp nhóm + chương trình + chi nhánh + học phí, phân trang và thao tác xóa bộ lọc. Không giấu mất bộ lọc đang áp dụng khi người dùng chuyển trang.
- Trang chi tiết thể hiện đối tượng, chương trình, thời lượng, điều kiện đầu vào, giá theo quyền công khai và các lớp đang tuyển. Một khóa không lặp lại vì có nhiều nhóm.
- Form tư vấn giữ ngữ cảnh lựa chọn, validate lại khóa/lớp/chi nhánh tại backend và tạo lead đúng phạm vi.

Đầu ra: website hiển thị bộ mẫu mới theo cách điều hướng dễ hiểu trên máy tính và điện thoại.

## D7 — Nghiệm thu xuyên hệ thống

- Đi theo một khóa từ quản lý → CMS → menu nhóm → trang chi tiết → tư vấn → lead trong web quản lý.
- So sánh kết quả bộ lọc và số khóa giữa quản lý, website, chatbot; giải thích khác biệt do trạng thái xuất bản và phạm vi quyền.
- Kiểm tra bốn role web và nhiều chi nhánh; không để reset làm sai membership hoặc mở quyền.
- Dùng tài khoản STUDENT được liên kết mới để kiểm tra lớp, lịch, điểm, điểm danh, học phí và phiếu thu.
- Kiểm tra khóa nhiều đối tượng, nhóm rỗng, khóa đóng, lớp đầy, chi nhánh ngừng hoạt động, nguồn chưa xuất bản và backend mất kết nối.
- Chạy lại audit G8, test liên quan, build và kiểm tra Chrome responsive. Kiểm tra thanh toán sandbox riêng; seed không thay thế IPN/đối soát thực tế.

Đầu ra: báo cáo đạt/lỗi, dữ liệu demo có thể dùng xuyên các ứng dụng.

## D8 — Bàn giao và tái tạo

- Ghi tài khoản mẫu, ma trận phân quyền, bộ dữ liệu, cách chạy và các kịch bản demo.
- Cung cấp lệnh tái tạo có kiểm tra môi trường, seed có phiên bản và hướng dẫn khôi phục. Lưu backup ngoài tài liệu công khai.
- Tách trạng thái nghiệm thu source/browser, model thật, APK thật và thanh toán sandbox; không đánh dấu đã thử nếu chưa thực hiện.
- Chốt các bước reset dữ liệu ngoài phạm vi thanh toán nhà cung cấp; lịch sử VNPAY sandbox bên ngoài không tự bị xóa khi database local bị làm mới.

## Tiêu chí đóng kế hoạch

- Không khóa mới nào thiếu chương trình hoặc nhóm đối tượng; không FK/quan hệ logic mồ côi.
- Không trùng mã, số đếm khóa, xung đột lịch hoặc sai công nợ/sĩ số.
- Tài khoản giữ lại đăng nhập và liên kết đúng hồ sơ mới.
- Các mục menu truy vấn đúng dữ liệu đã xuất bản; không hiện bản ghi thử/riêng tư ngoài danh sách được phép.
- Form tư vấn tạo lead đúng ngữ cảnh, web/app/chatbot đọc thống nhất theo quyền.
- Có báo cáo kiểm thử và phương án phục hồi đã thử.
