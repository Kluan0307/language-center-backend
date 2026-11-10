# Lộ trình hoàn thiện theo yêu cầu đề tài

Ngày lập: **07/11/2026**. Đề tài: **Xây dựng hệ thống quản lý trung tâm ngoại ngữ**, tập trung trung tâm tiếng Anh.

Đây là kế hoạch bổ sung sau các giai đoạn cải thiện 1–7. Ký hiệu T1–T6 bên dưới dùng riêng cho đợt hoàn thiện khóa luận. **T1/G01 đã hoàn thành phần đặc tả ngày 07/11/2026; G02–G03 đã hoàn thiện nhân sự; G04–G05 đã hoàn thiện chương trình/đánh giá ngày 08/11/2026; G06–G07 đã hoàn thiện ngày 08/11/2026; T3/G04–G07 hoàn thành triển khai, G08–G09 đã hoàn thiện thời lượng ngày08/11/2026; G10 đã hoàn thành ngày08/11/2026; G11–G12 hoàn thành ngày08/11/2026, T4 hoàn tất; G13–G14 đã hoàn thiện tài liệu ngày08/11/2026; G15–G20 chưa triển khai, T5 còn G15/G16 và phần báo cáo.** Xem [bàn giao G01](thesis-progress/G01.md) và [bộ tài liệu yêu cầu/quy tắc](thesis/README.md).

Để giao việc theo từng prompt, dùng **[kế hoạch G01–G20](../../../KE_HOACH_HOAN_THIEN.md)** tại thư mục `language-center`. File đó chia nhỏ đầy đủ T1–T6, có prompt sao chép, phụ thuộc, tiêu chí và mẫu bàn giao cho từng phiên coding.

## 1. Mục tiêu và phạm vi chốt

Hoàn thiện các yêu cầu cơ bản trong đề cương: hồ sơ học viên/giáo viên, khóa/lớp/lịch, phân lớp theo trình độ, quá trình học tập, học phí, tìm kiếm, báo cáo, toàn vẹn dữ liệu và tài liệu khóa luận.

Giữ kiến trúc, bốn role và cấu trúc code hiện có; tiếp tục dùng Ant Design và styled-components cho FE. Hoàn thiện từng nghiệp vụ xuyên database → backend → frontend → test → tài liệu. Mỗi giai đoạn cần đủ cả giao diện và API trước khi nghiệm thu.

E-learning, thi trực tuyến, điểm danh QR, cổng thanh toán, portal học viên và ứng dụng mobile thuộc hướng phát triển. Triển khai Internet là mốc vận hành riêng khi đã chọn máy chủ/domain; không phải việc ưu tiên để xử lý các thiếu sót nghiệp vụ hiện tại.

## 2. Baseline và các khoảng trống

| Yêu cầu | Đã có | Công việc còn cần làm | Giai đoạn |
|---|---|---|---|
| Hồ sơ học viên | Thông tin cá nhân, trình độ, liên hệ, người giám hộ, tìm kiếm, lịch sử đăng ký | Chuẩn hóa trình độ và liên kết thông tin đầu vào với phân lớp | T3 |
| Hồ sơ giáo viên | Tạo hồ sơ, chuyên môn/bằng cấp, xem, phân công chi nhánh, khóa tài khoản | Quản trị sửa hồ sơ, trạng thái làm việc, xử lý lớp/buổi tương lai khi ngừng làm việc | T2 |
| Khóa học | Tên, loại, trình độ, học phí, thời lượng, điều kiện đầu vào | Phân biệt đầu vào/đầu ra và xác định đơn vị thời lượng | T3–T4 |
| Lớp/lịch | Giáo viên, phòng, sĩ số, lịch tuần, buổi thực tế, nghỉ/bù/dạy thay | Đối chiếu thời lượng đã xếp với kế hoạch khóa | T4 |
| Phân lớp theo trình độ | Chọn lớp thủ công; kiểm tra chi nhánh, sĩ số, trùng lịch | Kết quả đầu vào, gợi ý/lọc lớp phù hợp, xử lý ngoại lệ có lý do | T3 |
| Theo dõi học tập | Điểm danh, điểm kiểm tra, nhận xét, bảo lưu/học lại, lịch sử | Tách kết quả tạm thời và kết quả cuối khóa; quy định đủ điều kiện hoàn thành | T4 |
| Học phí/thanh toán | Thu nhiều lần, công nợ, phiếu thu, điều chỉnh/đảo/hoàn có lịch sử | Nghiệm thu số tiền và kỳ báo cáo bằng bộ dữ liệu có kết quả kỳ vọng | T6 |
| Tìm kiếm/thống kê | Danh sách có lọc, dashboard và báo cáo từ API | Đối chiếu thống kê với dữ liệu mẫu, nhất quán thuật ngữ | T5–T6 |
| Phân quyền/toàn vẹn | Role/chi nhánh/đối tượng, transaction, version và audit ở nhiều luồng | Bổ sung kiểm tra tương ứng các nghiệp vụ mới | T2–T6 |
| Tài liệu | README, API, quyết định nghiệp vụ, Class Diagram tổng quan | Khảo sát, Use Case, ERD, Class Diagram đầy đủ, truy vết và báo cáo | T1, T5 |

Baseline kiểm thử ghi nhận ở giai đoạn trước: 195 backend tests, 72 frontend tests, 9 tests công cụ backup, các smoke/role checks và restore rehearsal. Đây là bằng chứng của lần xác minh trước, không phải kết quả chạy lại hoặc chứng minh các công việc T1–T6 đã hoàn thành. Xem [verification](verification.md).

## 3. Thứ tự triển khai và ước lượng

Ước lượng tham khảo cho một người làm tập trung, trên nền code hiện tại: **20–26 ngày làm việc**, gồm khoảng hai ngày dự phòng. Chưa có hạn nộp và lịch làm việc thực tế nên đây chưa phải cam kết thời gian. Viết báo cáo, khảo sát và cập nhật sơ đồ thực hiện song song từ T1.

| Mốc | Nội dung chính | Ước lượng | Phụ thuộc | Sản phẩm nghiệm thu |
|---|---|---:|---|---|
| T1 | Chốt yêu cầu, giả định và quy tắc nghiệp vụ | 1–2 ngày | Đề cương đã cung cấp | Ma trận yêu cầu, use case dự kiến, danh sách quy tắc cần xác nhận |
| T2 | Hoàn thiện quản lý giáo viên | 3–4 ngày | T1 | API + màn hình sửa hồ sơ/ngừng làm việc, kiểm tra phân quyền/lịch |
| T3 | Hỗ trợ phân lớp theo trình độ | 4–5 ngày | T1 | Kết quả đầu vào, lớp phù hợp, ngoại lệ có xác nhận — **hoàn thành G04–G07 ngày 08/11/2026** |
| T4 | Chuẩn hóa thời lượng và kết quả học tập | 4–5 ngày | T1, T3 đối với mô hình trình độ | Đơn vị thời lượng, tiến độ lớp, kết quả tạm/cuối khóa |
| T5 | Đồng bộ tài liệu và hoàn thiện báo cáo | 3–4 ngày tập trung, làm dần từ T1 | Sơ đồ/API chốt sau T2–T4 | Use Case, ERD, Class Diagram, đặc tả và báo cáo khớp code |
| T6 | Nghiệm thu xuyên hệ thống và chuẩn bị bảo vệ | 3–4 ngày | T2–T5 | Bộ dữ liệu demo, test report, kịch bản demo, bản release chốt |

Sau T1, bắt đầu T2. Nếu có hai luồng công việc độc lập thì T2 và thiết kế T3 có thể tiến hành song song; vẫn phải chốt API và dữ liệu trước khi tích hợp.

## 4. T1 — Chốt yêu cầu và nghiệp vụ

### Công việc

- Gán mã yêu cầu: RQ01 học viên, RQ02 giáo viên, RQ03 khóa học, RQ04 lớp học, RQ05 phân lớp, RQ06 lịch học, RQ07 theo dõi học tập, RQ08 học phí, RQ09 tìm kiếm/báo cáo, RQ10 phân quyền/toàn vẹn, RQ11 tài liệu thiết kế, RQ12 kiểm thử/báo cáo khóa luận.
- Lập ma trận mỗi yêu cầu → use case → role → màn hình/API → dữ liệu → test case → bằng chứng. Phân biệt đã có, cần bổ sung, ngoài phạm vi và chưa xác minh.
- Ghi rõ chính sách xóa/ngừng hoạt động: hồ sơ đã có lịch sử phải bảo toàn liên kết; dữ liệu chưa phát sinh nghiệp vụ mới được xem xét xóa theo ràng buộc.
- Chốt đầu vào/đầu ra khóa học, cách xác nhận trình độ, đơn vị thời lượng, điều kiện đạt cuối khóa và quyền duyệt ngoại lệ.
- Lập bảng câu hỏi khảo sát và mô tả quy trình hiện tại của trung tâm. Chỉ ghi là kết quả khảo sát khi có dữ liệu thực tế; các quyết định tự đề xuất phải ghi là giả định.

### Hoàn thành khi

- Mỗi yêu cầu trong đề cương có mã và cách nghiệm thu cụ thể.
- Các quyết định cần thiết để triển khai T2–T4 được ghi thành quy tắc hoặc giả định có thể kiểm chứng.
- Có một luồng nghiệp vụ chung: tiếp nhận → đánh giá đầu vào → phân lớp → ghi danh → thu phí → học/điểm danh → đánh giá → báo cáo.

## 5. T2 — Hoàn thiện quản lý giáo viên

### Backend và dữ liệu

- `auth-service`: bổ sung luồng quản trị xem/sửa họ tên, liên hệ, chuyên môn, bằng cấp, kinh nghiệm, chứng chỉ và thông tin làm việc. Dùng version để ngăn ghi đè hồ sơ cũ, ghi người sửa và thời điểm/lý do khi cần.
- Tách trạng thái làm việc của hồ sơ với trạng thái đăng nhập. Khi ngừng làm việc, giáo viên không được nhận phân công mới; chính sách khóa đăng nhập phải được xác định rõ.
- Trước ngừng làm việc/thu hồi phân công, kiểm tra lớp và buổi thực tế tương lai, gồm cả dạy thay. Hiển thị các việc cần bàn giao, chặn ngừng khi chưa xử lý hết theo chính sách đã chốt.
- Phối hợp Academic qua API hiện có/bổ sung phù hợp, giữ ranh giới service. Kiểm tra trạng thái lại khi phân công để tránh người dùng gửi request cũ hoặc thao tác đồng thời vượt ràng buộc.
- Giữ lịch sử giảng dạy, điểm và điểm danh; quyết định rõ cách hiển thị tên hiện tại và tên đã chụp tại thời điểm phát sinh.
- Super Admin thao tác toàn hệ thống; Admin chỉ thao tác trong phạm vi được phép. Hồ sơ giáo viên nhiều chi nhánh phải xét quyền trên toàn bộ phạm vi bị ảnh hưởng khi thay đổi thuộc tính dùng chung.

### Frontend

- Mở rộng trang giáo viên hiện đang dùng `AccountManagement fixedRole="TEACHER"`: xem/sửa hồ sơ, trạng thái làm việc, phản hồi validation và xung đột phiên bản.
- Có thông tin các lớp/buổi cần bàn giao trước ngừng làm việc; dẫn đến màn hình xử lý tương ứng.
- Staff giữ quyền tra cứu theo phạm vi; không được sửa thông tin nhân sự chỉ bằng cách gọi API trực tiếp.

### Hoàn thành khi

- Tạo giáo viên → sửa chuyên môn/liên hệ → reload vẫn đúng.
- Admin ngoài phạm vi và Staff bị từ chối cập nhật.
- Không ngừng giáo viên còn buổi tương lai chưa bàn giao; sau bàn giao thì thực hiện được.
- Giáo viên ngừng làm việc không được phân công mới; dữ liệu lớp cũ còn nguyên.

## 6. T3 — Phân lớp theo trình độ

### Backend và dữ liệu

- `academic-service`: chuẩn hóa trình độ theo chương trình, phân biệt trình độ hiện tại, điều kiện đầu vào và mục tiêu đầu ra của khóa. Không so sánh bằng thứ tự chuỗi hoặc tự quy đổi IELTS/TOEIC/CEFR khi chưa có chính sách.
- Bổ sung bản ghi đánh giá đầu vào: học viên, chương trình, phương thức đánh giá, kết quả/trình độ, ngày đánh giá, nguồn/người đánh giá và người ghi nhận. Điểm/thang điểm chỉ bắt buộc khi phương thức đánh giá sử dụng điểm.
- Hỗ trợ nhân viên ghi nhận kết quả được xác nhận từ bài kiểm tra/phỏng vấn/chứng chỉ; không yêu cầu xây nền tảng thi trực tuyến để hoàn thành chức năng này.
- Gợi ý/lọc lớp dựa trên đầu vào đã chốt, chương trình, chi nhánh, trạng thái, sĩ số và lịch. Cần phân biệt phù hợp, chưa đủ dữ liệu và cần ngoại lệ.
- Khi lưu ghi danh, backend kiểm tra lại điều kiện, không chỉ dựa vào lựa chọn trên FE. Trường hợp thiếu đánh giá hoặc ngoài mức phù hợp phải theo luồng ngoại lệ đã chốt; mặc định đề xuất quản lý xác nhận và lưu lý do.
- Lưu đánh giá/điều kiện dùng tại lúc ghi danh để việc sửa trình độ sau này không làm mất căn cứ quyết định cũ. Bổ sung quyền và xử lý xung đột cho xác nhận ngoại lệ.

### Frontend

- Trong hồ sơ học viên: xem/ghi nhận đánh giá đầu vào và lịch sử.
- Trong form ghi danh: hiển thị trình độ học viên, đầu vào/đầu ra khóa, lớp gợi ý, sĩ số, lịch và lý do chưa phù hợp.
- Khi chọn ngoại lệ: hiện trạng thái/lý do và bước xác nhận của quản lý. Không mở quyền đọc tài chính/CRM cho Teacher để phục vụ nhập đánh giá.

### Hoàn thành khi

- Học viên có đầu vào phù hợp thấy lớp phù hợp và ghi danh được.
- Học viên chưa có đánh giá được hướng dẫn bổ sung hoặc xử lý ngoại lệ rõ ràng.
- Không thể bỏ qua xác nhận bằng cách gọi API trực tiếp.
- Ghi danh vẫn chặn lớp đầy, trùng lịch và sai chi nhánh.
- Khóa có mục tiêu IELTS 6.5 được xử lý theo điều kiện đầu vào cấu hình, không mặc định yêu cầu học viên đã đạt 6.5.

## 7. T4 — Thời lượng và kết quả học tập

### Thời lượng

- Chốt đơn vị; đề xuất lưu số phút dự kiến và hiển thị giờ học để hỗ trợ buổi 90/120 phút. Đánh giá tác động trước khi thay kiểu/trường `duration` hiện tại.
- Dữ liệu `duration` cũ chưa biết đơn vị phải được rà soát; không tự nhân/chia thành giờ hoặc buổi. Migration có bảng đối chiếu hoặc cơ chế đánh dấu chưa xác minh.
- Hiển thị thời lượng dự kiến của khóa, thời lượng buổi đã xếp và thời lượng buổi đã hoàn tất của lớp. Buổi hủy không cộng; buổi học bù không cộng trùng với buổi nguồn đã hủy.
- Chênh lệch kế hoạch có cảnh báo; chính sách chặn hoàn thành lớp cần chốt ở T1.

### Kết quả học tập

- `teaching-service`: xác định bài bắt buộc, trọng số, ngưỡng đạt và điều kiện đủ dữ liệu để xét cuối khóa. Nếu dùng chuyên cần thì lấy đúng buổi/khoảng học tại Academic.
- Phân biệt: chưa đủ dữ liệu, đánh giá tạm thời, đủ điều kiện xét cuối khóa, đạt và chưa đạt. Điểm của một bài đã chấm không tự trở thành kết quả hoàn thành khóa.
- Dùng các loại kết quả/đạt phù hợp chương trình; ngưỡng hiện tại 50% chỉ được giữ làm mặc định khi chính sách chốt cho phép, không coi là chuẩn chung cho mọi chương trình.
- Làm rõ liên hệ giữa kết quả học thuật, trạng thái đăng ký `COMPLETED` và trạng thái kết thúc lớp. Chốt quyền xác nhận, điều kiện và lịch sử xác nhận/mở lại khi sửa điểm.
- Khóa dữ liệu hoặc yêu cầu mở lại có lý do khi sửa điểm/quy tắc ảnh hưởng kết quả đã chốt; không thay đổi kết quả cuối khóa âm thầm.

### Frontend

- Trang khóa/lớp ghi đơn vị rõ ràng và hiển thị tiến độ thời lượng.
- Trang bảng điểm/báo cáo hiển thị bài còn thiếu, điểm tạm thời, điều kiện chuyên cần nếu có và kết quả cuối khóa.
- Từ hồ sơ học viên có đường dẫn/xem tổng hợp lớp, chuyên cần và kết quả theo quyền; ưu tiên tận dụng các màn hình hiện có.

### Hoàn thành khi

- Hai buổi 90 phút hiển thị đúng ba giờ; hủy và học bù không làm tăng sai tổng.
- Thiếu bài bắt buộc không bị hiển thị là đạt cuối khóa.
- Tính đúng kết quả với bài khác thang điểm/trọng số, điểm 0 và bài chưa có điểm.
- Chỉ người đủ quyền xác nhận/mở lại; lịch sử cho biết ai thay đổi và vì sao.

## 8. T5 — Tài liệu và báo cáo khóa luận

### Bộ tài liệu cần bàn giao

- Khảo sát/quy trình nghiệp vụ, phạm vi và bảng giả định đã xác nhận.
- Use Case tổng quan và theo role; đặc tả các use case chính với tiền điều kiện, luồng chính, ngoại lệ, hậu điều kiện và quy tắc quyền.
- ERD bám schema cuối: PK/FK/unique/nullability, quan hệ, từ điển dữ liệu và bảng audit. Phân biệt ràng buộc database với tham chiếu ID qua service.
- Class Diagram đủ các lớp/thuộc tính/quan hệ quan trọng; mức chi tiết phải phù hợp báo cáo, không cần đưa toàn bộ lớp framework.
- Activity/Sequence cho ghi danh–tạo học phí, thu tiền, phân lớp ngoại lệ, bàn giao giáo viên và chốt kết quả.
- Ma trận truy vết RQ01–RQ12, test plan/report, hướng dẫn chạy/sử dụng, dữ liệu mẫu và kịch bản demo.
- Các chương báo cáo: tổng quan/khảo sát, cơ sở lý thuyết, phân tích, thiết kế, triển khai, kiểm thử/đánh giá, kết luận/hướng phát triển; định dạng theo mẫu của khoa.

### Đồng bộ tài liệu hiện có

- Hợp nhất API/README đang có các đoạn mô tả cũ mâu thuẫn với phần cập nhật: số API, convert lead/version, hoàn tiền và health.
- Mỗi API mô tả đúng body, role, phạm vi chi nhánh, lỗi nghiệp vụ và ví dụ hiện hành. Đánh dấu phần lịch sử thay vì để thành hướng dẫn chạy hiện tại.
- Nêu kiến trúc hiện tại dùng chung MySQL/schema nhưng có ranh giới service trong code; mô tả đúng triển khai thực tế.
- Cập nhật test count từ kết quả cuối, không dùng tổng test cũ để chứng nhận tính năng mới.

### Hoàn thành khi

- Mỗi yêu cầu đề cương truy được đến một chức năng và bằng chứng nghiệm thu.
- Các sơ đồ, UI, API và database không mâu thuẫn ở luồng nghiệp vụ chính.
- Các mục chưa khảo sát/kiểm chứng được ghi đúng trạng thái; báo cáo không tuyên bố đã triển khai Internet hoặc kiểm thử tải nếu chưa thực hiện.

## 9. T6 — Nghiệm thu và bảo vệ

### Dữ liệu và kiểm tra

- Chuẩn bị dữ liệu demo riêng với bốn role, ít nhất hai chi nhánh để thử phạm vi, các chương trình tiếng Anh, lớp có lịch/sĩ số và học viên với nhiều mức đầu vào.
- Có lớp còn chỗ/đầy, lịch trùng, giáo viên cần bàn giao, học viên thiếu đầu vào, học viên đóng một phần/đủ, có học bù và thiếu điểm bắt buộc.
- Ghi kết quả mong đợi trước khi chạy. Ví dụ học phí 3.000.000, thu hai lần 1.000.000 và 2.000.000: sau mỗi lần phải khớp phiếu thu, đã đóng, công nợ và báo cáo đúng kỳ.
- Test backend tập trung vào ràng buộc/quyền/transaction/concurrency của nghiệp vụ mới; FE kiểm tra validation, mất mạng, stale version và trạng thái kết quả. Chạy regression tương ứng các service bị ảnh hưởng.
- Nghiệm thu trên MySQL qua FE → gateway → service; thử URL/API trái quyền, reload, token hết hạn, ghi đồng thời và timeout sau commit ở các luồng quan trọng.
- Kiểm tra tìm kiếm/lọc/phân trang, trạng thái rỗng/lỗi, màn hình desktop/mobile; ghi nhận phản hồi người dùng nếu có điều kiện.
- Backup SQL/file và diễn tập restore schema cuối sau các migration mới. Giữ dữ liệu gốc; chỉ dọn dữ liệu thử trong phạm vi đã được cho phép.

### Demo dự kiến 15–20 phút, điều chỉnh theo quy định bảo vệ

1. Super Admin/Admin: giới thiệu quyền và phạm vi; tạo/sửa giáo viên, khóa/lớp/lịch.
2. Staff: tiếp nhận học viên, ghi nhận đầu vào, chọn lớp phù hợp và ghi danh.
3. Staff: thu một phần rồi thu đủ, xem phiếu và số dư.
4. Teacher: xem lớp/buổi được giao, điểm danh và nhập điểm.
5. Admin: đối chiếu kết quả học tập, xử lý đủ điều kiện cuối khóa và xem báo cáo.
6. Minh họa một ngoại lệ có giá trị: không cho lớp đầy/trùng lịch, thiếu điểm chưa đạt cuối khóa hoặc chặn truy cập ngoài chi nhánh.

### Điều kiện chốt bản bảo vệ

- T2–T5 đạt tiêu chí, không còn lỗi chặn luồng demo, sai quyền hoặc sai số tiền/kết quả ở bộ test nghiệm thu.
- Có test report ghi ngày, môi trường, dữ liệu, expected/actual và giới hạn; các test bắt buộc đều đạt.
- Có source chốt, migration, hướng dẫn chạy, backup/demo, bản build và báo cáo cuối khớp nhau.
- Chạy thử demo trọn vẹn theo vai trò, khởi động lại/reload vẫn đúng dữ liệu.

## 10. Checklist theo dõi

- [x] T1: ma trận yêu cầu và chính sách nghiệp vụ đã chốt ở mức giả định triển khai G01-v1; chưa thay thế khảo sát thật. [Bằng chứng và bàn giao](thesis-progress/G01.md).
- [x] T2: sửa hồ sơ/ngừng làm việc/bàn giao giáo viên hoàn chỉnh cả BE và FE. [G02](thesis-progress/G02.md) · [G03](thesis-progress/G03.md).
- [ ] T3: đầu vào, lớp phù hợp và ngoại lệ được kiểm tra tại backend.
- [x] T4: thời lượng có đơn vị, kết quả tạm/cuối khóa và xác nhận có lịch sử.
- [ ] T5: Use Case, ERD, Class Diagram và báo cáo khớp hiện trạng.
- [ ] T6: nghiệm thu MySQL/UI, dữ liệu demo, backup/restore và bản bảo vệ chốt.

Mỗi lần hoàn thành một mốc, ghi ngày, thay đổi thực tế và liên kết bằng chứng. Các mục chưa làm không được đánh dấu chỉ vì đã có thiết kế hoặc API.
