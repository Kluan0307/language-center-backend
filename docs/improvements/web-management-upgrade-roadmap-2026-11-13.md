# Lộ trình nâng cấp web quản lý Language Center

Ngày lập: 13/11/2026. Trạng thái cập nhật 13/11/2026: W1–W8 đã triển khai trong source và kiểm thử tự động; nghiệm thu môi trường thật của W6–W8 còn chờ. Xem [bàn giao W6–W8, ảnh và kiểm thử](web-w6-w7-w8-completion.md). Xem [thiết kế W1](web-w1-design-and-contracts.md), [bàn giao W1–W2](web-w1-w2-completion.md) và [bàn giao W3–W5, migration/cách chạy](web-w3-w4-w5-completion.md). Chưa áp dụng migration lên MySQL đang dùng hoặc build lại APK trong đợt W3–W5.

Phạm vi: web quản lý SUPER_ADMIN, ADMIN, STAFF, TEACHER và API phục vụ các luồng đó. Giữ React/TypeScript, Ant Design 6, styled-components, FullCalendar và cấu trúc source hiện tại. Đây là đợt nâng cấp web mới, không phải làm lại G1–G10 mobile. Phần vận hành thanh toán W3–W4 tiếp nối giai đoạn 5 còn chờ của kế hoạch tối ưu thanh toán.

## 1. Định hướng sản phẩm

Mục tiêu là phần mềm vận hành trung tâm tiếng Anh: nhân viên tìm được học viên, biết khoản cần xử lý, hoàn thành thao tác và tra lại lịch sử. Giao diện chuyên nghiệp cần phân cấp thông tin, trạng thái chính xác và thao tác nhất quán, bên cạnh màu sắc và bố cục.

Triển khai tính năng và UI trong cùng từng giai đoạn. Không để toàn bộ nghiệp vụ xong rồi mới sửa UI, cũng không đổi hết màu/layout trước khi xác định luồng làm việc.

Giữ dữ liệu, phân quyền và nghiệp vụ đã nghiệm thu. Không thêm LMS, chat, lương, CRM phức tạp hoặc ngân hàng production vào đợt này. Không tự xác nhận giao dịch hoặc giả lập số liệu để dashboard đầy hơn.

## 2. Tham khảo sản phẩm thực tế

Tham khảo trang sản phẩm, bài cập nhật giao diện và tài liệu công khai; không có tài khoản để kiểm thử toàn bộ back office của các sản phẩm này. Những lựa chọn áp dụng bên dưới là đề xuất cho Language Center, không phải khẳng định mọi sản phẩm tham khảo đều có đúng bố cục/token đó.

| Nguồn chính thức | Điểm tham khảo được | Áp dụng cho dự án |
|---|---|---|
| [TutorCruncher: đợt làm mới UI](https://tutorcruncher.com/blog/tutorcrunchers-new-ui) | Phân cấp thông tin, điều hướng, dashboard, lịch và hồ sơ được cải thiện trong cùng đợt làm mới | Chốt các mẫu trang chung rồi triển khai theo từng luồng |
| [TutorCruncher: Administrator Dashboard](https://help.tutorcruncher.com/en/articles/14182645-managing-the-administrator-dashboard) | Các panel theo hoạt động/công việc, gồm việc đến hạn và nội dung liên quan người quản lý | Dashboard có hàng đợi công việc mở được tới đúng dữ liệu; chưa cần tính năng tự kéo/thả panel |
| [Teachworks FAQ](https://www.teachworks.com/faq) | Lịch có bộ lọc/view, liên hệ giữa lịch và thu phí, tổng quan số liệu và khoản quá hạn | Giữ ngữ cảnh lớp/học viên khi chuyển màn hình; không sao chép mô hình tính phí theo giờ vì dự án đang có billing root riêng |
| [Jackrabbit Class: Class Management](https://www.jackrabbitclass.com/features/class-management/) | Lịch hoạt động, phòng/giáo viên, điểm danh và học bù trong quản lý lớp | Kết nối lớp–buổi–điểm danh và thể hiện xung đột rõ ràng |
| [Ant Design ProComponents](https://pro-components.antdigital.dev/en-US/index.html) | Mẫu tổ chức trang bảng/tìm kiếm, form và chi tiết | Tham khảo cách bố trí với Ant Design hiện có; chưa yêu cầu cài ProComponents hoặc thay framework |

Không sao chép thương hiệu, ảnh, code hoặc toàn bộ nghiệp vụ của sản phẩm khác. Không dùng trang marketing làm bằng chứng về quyền hoặc tính đúng đắn nghiệp vụ trong hệ thống của mình.

## 3. Nền tảng hiện có và khoảng trống

Đã có `ManagementLayout`, theme xanh #2563EB, Sidebar/BranchSwitcher, `TableActions`, useRemoteResource, các form tài chính có recovery, dashboard theo nhóm role và Time-grid. Cần phát triển từ các phần này.

| Hiện có | Cần cải thiện |
|---|---|
| Header hiển thị phạm vi từ tài khoản | Nhãn phải phân biệt chi nhánh đang làm việc với phạm vi thực sự của trang/bộ lọc; SUPER_ADMIN không luôn ghi “Toàn hệ thống” khi đang xem một chi nhánh |
| Một số bảng đã dùng TableActions | Áp dụng đồng nhất; giảm nút cùng cấp, UUID dài và cột không cần trong lần xem đầu |
| Học phí và phiếu thu có tìm kiếm/liên kết | Hoàn thiện điều hướng hai chiều tới học viên, giao dịch online và yêu cầu hỗ trợ |
| Trang OnlinePayments có đơn, sự kiện và đối soát | Thiếu ngữ cảnh học viên/lớp trên bảng, tìm kiếm thuận tiện và cách phân cấp bằng chứng kỹ thuật |
| Trang hỗ trợ có trạng thái, phản hồi và ghi chú nội bộ | Chưa có liên kết đơn online UUID theo hợp đồng hiện tại; cần bổ sung đúng kiểu tham chiếu và quyền |
| Hồ sơ học viên có tài khoản ứng dụng | Cần trang tổng quan học tập/tài chính/hỗ trợ để giảm mở nhiều trang |
| Dashboard có thống kê, công nợ và quá hạn | Cần ưu tiên việc cần xử lý, liên kết có bộ lọc và thông tin phù hợp từng role |
| FullCalendar Time-grid đã hoạt động | Làm rõ bộ lọc, legend, chi tiết buổi và các bước điều phối; không cần thay thư viện |

## 4. Chuẩn giao diện đề xuất

### Nhận diện và bố cục

- Giữ màu chính #2563EB. Nền trung tính, bề mặt trắng, viền xám nhạt; giảm dùng viền/xuất hiện màu xanh ở mọi khối.
- Font nền 14px, nội dung quan trọng 16px, tiêu đề trang 24–28px; số tiền và số liệu dùng chữ số thẳng hàng nếu font hỗ trợ.
- Khoảng cách dùng thang 4/8/12/16/24/32px; radius chủ đạo 8–12px; bóng nhẹ cho lớp nổi, không dùng shadow dày cho mọi card.
- Sidebar khoảng 240–256px, giữ cố định khi cuộn nội dung. Trên màn hình thấp, menu tự cuộn trong vùng riêng để mọi mục đều truy cập được.
- Header gọn: breadcrumb/tên trang, phạm vi, một hành động chính. Mô tả dài chuyển sang trợ giúp phù hợp.
- Kiểm tra ở 1366×768, 1920×1080 và tablet khoảng 1024px. Khi hẹp phải xếp lại filter, không tạo cuộn ngang cho toàn trang.

### Mẫu trang danh sách

Thứ tự: **tiêu đề/phạm vi → tổng quan ngắn khi cần → tìm kiếm/bộ lọc → bảng → phân trang**.

- Mặc định hiển thị các filter dùng thường xuyên; filter ít dùng nằm trong “Bộ lọc thêm”. Hiển thị điều kiện đang áp dụng và nút xóa lọc.
- Search chạy phía server khi dataset phân trang; không lọc riêng trang đang tải rồi gọi là tìm kiếm toàn hệ thống.
- Lưu filter/sort/page trên URL trong phạm vi API hỗ trợ. Đổi filter về trang đầu; mở và đóng chi tiết không làm mất ngữ cảnh danh sách.
- Cột đầu là đối tượng nhận diện: mã ngắn + tên; tiền căn phải; ngày giờ nhất quán; một hành động thường dùng và menu thao tác phụ.
- Không giấu hành động quan trọng hoàn toàn sau hover. Icon cần tên truy cập/tooltip; trạng thái luôn có chữ, không chỉ dựa vào màu.
- UUID đầy đủ ở phần chi tiết/sao chép, không kéo giãn bảng. Mã cắt ngắn chỉ để hiển thị, không được coi là khóa duy nhất.

### Mẫu trang chi tiết và form

- Drawer cho xem nhanh; hồ sơ học viên/lớp có nhiều nghiệp vụ nên có trang chi tiết riêng. Giữ link/route cũ tương thích.
- Chi tiết mở bằng phần tóm tắt và hành động theo trạng thái; các nhóm lớn dùng tab. Lịch sử kỹ thuật và thông tin phụ mở khi cần.
- Form chia nhóm có ý nghĩa, nhãn rõ, validate tại trường và có trạng thái đang lưu. Không dùng nhiều modal lồng nhau.
- Modal xác nhận phải nêu đối tượng và hệ quả. Xung đột version phải cho tải lại dữ liệu, không tự ghi đè.
- Chuẩn hóa loading, trống, lỗi, mất quyền và thành công. Phân biệt “không có dữ liệu” với “không tải được dữ liệu”.

### Vai trò

| Role | Nội dung cần nổi bật |
|---|---|
| SUPER_ADMIN | Toàn hệ thống/chi nhánh, quản trị viên, phân công, tài chính và ngoại lệ vận hành |
| ADMIN | Chi nhánh được phân công, học vụ, nhân sự theo quyền, điều chỉnh và đối soát |
| STAFF | Tuyển sinh, học viên, đăng ký, thu tiền, tiếp nhận hỗ trợ theo quyền hiện có |
| TEACHER | Lịch dạy, lớp được phân công, điểm danh, điểm và tài liệu; không hiển thị tài chính quản trị |

Không mở quyền mới chỉ vì đưa một nút lên giao diện. Dùng đúng ma trận quyền hiện tại, backend kiểm tra cuối cùng. ADMIN đa chi nhánh chỉ có chế độ tổng hợp khi endpoint thực sự hỗ trợ tập chi nhánh được giao.

## 5. Tám giai đoạn triển khai song song nghiệp vụ và UI

### W1 — Khảo sát luồng và chốt thiết kế mẫu

**Nghiệp vụ:** lập inventory route/API/quyền, tái hiện các luồng tìm học viên, thu phí, đối soát, hỗ trợ và lịch dạy. Phân biệt lỗi đã tái hiện với ý tưởng cải tiến.

**UI:** chốt token, cấu trúc header, filter, bảng, drawer và form. Tạo wireframe trong tài liệu cho bốn màn hình: giao dịch online, học phí, hồ sơ học viên và dashboard STAFF. Không đưa dữ liệu giả vào runtime để minh họa.

**Đầu ra:** bản đồ màn hình, ma trận role, hợp đồng cần bổ sung, design brief và danh sách thay đổi ưu tiên. Xác nhận cách hiển thị phạm vi chi nhánh.

**Nghiệm thu:** mỗi màn hình có mục tiêu, hành động chính và đường quay lại; phân biệt rõ đơn, phiếu thu và công nợ; không còn quyết định UX quan trọng chưa được ghi nhận.

### W2 — Nâng nền UI và áp dụng trên trang giao dịch online

**Nghiệp vụ:** giữ API/route hiện có, chuẩn hóa trạng thái tải/lỗi và bảo toàn lựa chọn chi nhánh khi điều hướng. Chốt scope header với filter thật, hủy request cũ khi đổi phạm vi để tránh hiển thị dữ liệu nhầm.

**UI:** nối theme với ConfigProvider Ant Design và styled-components; nâng ManagementLayout, PageHeader, Filters, TableActions, StateView và DetailSection theo tên/cấu trúc hiện có. Áp dụng hoàn chỉnh trước cho OnlinePayments.

**Đầu ra:** bộ thành phần dùng chung và một trang mẫu hoạt động thật; không thay đồng loạt toàn bộ trang trước khi mẫu đạt.

**Nghiệm thu:** trang mẫu gọn ở 1366px, hỗ trợ keyboard/focus, không đổi quyền và không mất trạng thái khi mở/đóng drawer. Không cài thêm bộ UI lớn chỉ để thay các component sẵn có.

### W3 — Hoàn thiện luồng học phí, giao dịch online và phiếu thu

**Nghiệp vụ:** bổ sung tìm kiếm/lọc server-side theo các trường khả thi: học viên, mã giao dịch, lớp và chi nhánh. Bổ sung metadata/API theo nhu cầu thực tế. Liên kết hai chiều học phí ↔ giao dịch ↔ phiếu thu và hồ sơ học viên. Đối soát vẫn kiểm chữ ký/version/idempotency; không thêm nút tự xác nhận đã trả tiền.

**UI:** bảng đơn có học viên, lớp/khóa, tiền, kết quả và thời gian; drawer ưu tiên kết quả và bước tiếp theo. Đưa bằng chứng callback/query vào nhóm chi tiết kỹ thuật. Học phí và phiếu thu dùng cùng thuật ngữ, định dạng và bộ lọc.

**Đầu ra:** quy trình nhân viên tìm và xử lý được giao dịch từ thông tin học viên, không cần nhớ UUID; thu trực tiếp và online phân biệt rõ.

**Nghiệm thu:** từ học viên tới đúng đơn/phiếu và quay lại giữ bộ lọc; STAFF chỉ tra cứu theo quyền, ADMIN/SUPER_ADMIN đối soát theo phạm vi. Giao dịch thành công không cộng hai lần vào doanh thu; hoàn/đảo phiếu không bị hiểu là VNPAY đã hoàn về ngân hàng.

### W4 — Hàng đợi hỗ trợ và liên kết giao dịch

**Nghiệp vụ:** hỗ trợ gắn đơn online UUID. Hiện resourceId của ticket dùng số: thiết kế extension tương thích, ví dụ resourceKey string mới hoặc trường orderId riêng; không ép UUID thành số hay giả mượn tuitionId. Kiểm ownership/chi nhánh qua API của service sở hữu dữ liệu. Bổ sung tìm kiếm theo học viên, loại và trạng thái trong phạm vi cần dùng.

**UI:** danh sách ưu tiên yêu cầu mới/đang xử lý, chi tiết có hội thoại, tài nguyên liên quan và lịch sử trạng thái. Phân biệt rõ phản hồi công khai với ghi chú nội bộ trước khi gửi. Mặc định trả lời cho học viên; ghi chú nội bộ là lựa chọn có nhãn rõ.

**Đầu ra:** học viên báo lỗi từ mobile, nhân viên mở đúng khoản/đơn để xử lý và trả lời; không bắt chép lại nhiều mã.

**Nghiệm thu:** link tới đúng tài nguyên và giữ filter, không lộ ghi chú nội bộ, không truy cập chéo chi nhánh. Tiếp nhận ticket không tự đổi học phí, điểm hoặc trạng thái ngân hàng.

### W5 — Hồ sơ học viên và đăng ký học xuyên suốt

**Nghiệp vụ:** tổng hợp thông tin đã có: đăng ký hiện tại, lịch sử chuyển/bảo lưu, kết quả đã công bố, công nợ, giao dịch và hỗ trợ. Có API tổng hợp hoặc tải từng khối có kiểm quyền; tránh hàng chục request riêng cho từng dòng. Giữ công cụ cấp/khóa/reset tài khoản ứng dụng hiện có.

**UI:** trang hồ sơ có header nhận diện, trạng thái và lớp hiện tại; các nhóm Tổng quan, Học tập, Tài chính, Hỗ trợ, Lịch sử. Đánh giá đầu vào vẫn được giữ đúng chương trình/trình độ, không làm mất dữ liệu khi gom tab. Form thêm/sửa và flow đăng ký chia bước khi có phụ thuộc thực sự.

**Đầu ra:** nhân viên làm việc chủ yếu trong hồ sơ của đúng học viên, có lối đi sang lớp và tài chính; những khối không có quyền không được tải hoặc hiển thị.

**Nghiệm thu:** chuyển lớp không nhân đôi học phí; chưa có điểm không hiện 0 giả; khóa tài khoản không xóa hồ sơ. API và link tài khoản mobile đang dùng vẫn tương thích.

### W6 — Không gian học vụ và giảng dạy

**Nghiệp vụ:** nối lớp ↔ buổi ↔ điểm danh ↔ bài kiểm tra/điểm ↔ tài liệu theo quyền. Rà lại hiển thị buổi bù, hủy và dạy thay; giải thích điều kiện không được chỉnh sửa. Không mở drag/drop lịch trước khi có thiết kế xung đột, phân quyền và rollback đầy đủ.

**UI:** cải thiện Time-grid hiện có với filter, legend, nhận diện buổi và panel chi tiết. Trang lớp ưu tiên sĩ số, lịch, giáo viên, danh sách học viên và tiến độ. Bảng điểm/điểm danh giữ tên học viên rõ khi cuộn. TEACHER có workspace tập trung vào buổi sắp dạy và công việc được phép.

**Đầu ra:** luồng giáo viên dễ thực hiện từ lịch dạy tới điểm danh/điểm/tài liệu; quản lý theo dõi cùng nguồn dữ liệu.

**Nghiệm thu:** lịch đúng múi giờ và lấy đủ trang; không đổi ngày do trình duyệt. Không lộ điểm nháp/tài liệu chưa chia sẻ; dạy thay chỉ cấp quyền đúng buổi; STAFF không được sửa lịch nếu ma trận hiện tại cấm.

### W7 — Dashboard theo công việc và báo cáo rõ nghĩa

**Nghiệp vụ:** tạo các hàng đợi có dữ liệu và route đích: hỗ trợ mới, đơn cần đối soát, học phí quá hạn, việc học vụ thực sự có trạng thái theo dõi. Nếu chưa có trạng thái “đã duyệt/chưa công bố/chưa điểm danh” đáng tin thì thiết kế API trước, không tự suy từ trang dữ liệu thiếu. Rà định nghĩa doanh thu/thu ròng, công nợ và mốc thời gian.

**UI:** 4–6 chỉ số quan trọng, khối “Cần xử lý”, lịch/buổi hôm nay và lối tắt theo role. Các phân tích sâu chuyển sang Báo cáo. Bảng/biểu đồ có cùng filter, chú giải và thời điểm cập nhật; biểu đồ chỉ dùng khi giúp ra quyết định.

**Đầu ra:** SUPER_ADMIN thấy toàn chuỗi hoặc phạm vi đã chọn; ADMIN thấy chi nhánh được giao; STAFF thấy việc tuyển sinh/thu/hỗ trợ; TEACHER thấy lịch và lớp của mình.

**Nghiệm thu:** bấm một cảnh báo mở đúng danh sách đã lọc; số liệu toàn bộ không được tính từ một trang. Thống kê 0 khác với không có quyền/chưa tải; tổng thu không cộng đơn PENDING hoặc cộng trùng phiếu online.

### W8 — Hoàn thiện phần còn lại, kiểm thử và bàn giao

**Nghiệp vụ:** áp dụng mẫu đã chốt cho tài khoản, quản trị viên, phân công chi nhánh, nhân sự, chương trình/khóa/phòng/lead và các trang còn lại. Hoàn thiện thông báo xung đột, chặn double submit và recovery đúng các mutation tài chính hiện hữu. Không thêm thao tác hàng loạt có hậu quả nghiệp vụ khi chưa có hợp đồng rõ.

**UI:** chuẩn hóa form/table/action trên các trang còn lại; rà header cố định, overflow, dữ liệu dài, keyboard, trạng thái trống và cấu hình chưa khả dụng. Dọn icon/template và nội dung phát triển nếu còn; giữ nhãn sandbox và cảnh báo nghiệp vụ thật.

**Đầu ra:** toàn web có trải nghiệm nhất quán; README, API list, ảnh trước/sau phù hợp và hướng dẫn theo role được cập nhật. Rà bundle/chunk trước khi đổi cấu hình build; chỉ tối ưu phần tải chậm có bằng chứng.

**Nghiệm thu:** build/typecheck, test phù hợp, API và UI theo bốn role, nhiều chi nhánh, URL mở trực tiếp, back/refresh, dữ liệu nhiều trang và lỗi dependency. Hồi quy app mobile/APK với các API thay đổi. Các bài kiểm thử giữ lại; không xóa test để báo build sạch.

## 6. Thứ tự và cách chia lượt triển khai

**W1 → W2 → W3 → W4 → W5 → W6 → W7 → W8.**

W1–W2 chốt một mẫu UI dùng thật. W3–W4 ưu tiên vấn đề thanh toán/hỗ trợ đã phát sinh sau khi có app. W5–W6 mở rộng trải nghiệm xuyên suốt học viên/học vụ. W7 hoàn thiện dashboard khi các số đếm và link đích đã đáng tin. W8 đưa các trang còn lại về cùng chuẩn và nghiệm thu toàn hệ thống.

Mỗi W là một lượt triển khai độc lập, có báo cáo thay đổi, API/migration nếu có, kiểm thử và phần còn chờ. Nếu W quá lớn, tách nội bộ thành hợp đồng backend trước, UI sau, nhưng phải nghiệm thu chung luồng sử dụng rồi mới đánh dấu hoàn thành.

Không đưa ước lượng ngày cố định khi chưa chốt W1 và mức thay đổi backend. Các phase thanh toán W3–W4 có rủi ro nghiệp vụ cao hơn việc đổi layout; không gom hết thành một lần sửa UI.

## 7. Quy tắc bàn giao mỗi giai đoạn

1. Đọc code/API thực tế trước khi sửa; tài liệu cũ có thể là ảnh chụp lịch sử.
2. Giữ đường dẫn và hợp đồng client cũ; thay đổi API phải kiểm cả web lẫn APK đã cài. Migration nếu cần là cộng thêm, có hướng dẫn/backfill; không reset database.
3. Filter chi nhánh và quyền phải được kiểm ở server; query/search/count/export cùng phạm vi. Không lưu cache dữ liệu người khác khi đổi account/chi nhánh.
4. Tách rõ ba nhóm: lỗi đã sửa, tính năng vừa bổ sung, UI vừa chuẩn hóa. Chỉ đánh dấu PASS với bằng chứng đã chạy; không gọi mock UI là E2E thật.
5. Ảnh/wireframe minh họa dùng trong tài liệu; runtime nhận dữ liệu thật. Giữ thông báo lỗi/trống/hết quyền hữu ích, bỏ nhãn phát triển và thông tin kỹ thuật không giúp người dùng quyết định.
6. Không triển khai production, gửi thông báo thật hoặc thực hiện giao dịch/hoàn tiền mới chỉ để chụp màn hình đẹp.

Mốc kiểm thử gần nhất trước đợt này: web 96 test, backend 503 test và mobile 80 test đạt ở G10. Đây là baseline hồi quy, không phải cam kết số lượng hay coverage cho các W chưa triển khai.

## 8. Prompt dùng cho từng lượt

> Thực hiện giai đoạn Wn trong `docs/improvements/web-management-upgrade-roadmap-2026-11-13.md`. Đọc source và API hiện tại trước; triển khai cả phần nghiệp vụ và UI trong phạm vi Wn, giữ Ant Design + styled-components và component dùng chung. Không reset database, không nới quyền, giữ tương thích API với mobile/APK. Chạy kiểm tra phù hợp và cập nhật báo cáo hoàn thành, ghi rõ giới hạn chưa nghiệm thu. Không tự chuyển sang W tiếp theo.

W1–W8 đã triển khai source và kiểm thử tự động. Tiếp theo là nghiệm thu W6–W8 bằng dữ liệu MySQL thật, kiểm tra phân công đa chi nhánh, thao tác ghi, thanh toán sandbox và APK trên thiết bị theo [bàn giao W6–W8](web-w6-w7-w8-completion.md). Không tự xem UI fixtures là E2E thật. W6–W8 cần khởi động lại academic/finance, không có migration mới; nếu chưa áp dụng W3–W5 thì làm theo bàn giao cũ trước.
