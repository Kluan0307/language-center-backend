# U1–U7 — Chuẩn hóa giao diện website

Ngày rà soát: **19/11/2026**. Trạng thái cập nhật **20/11/2026: U1–U7 hoàn thành phần local tự động; kiểm tra điện thoại thật khi có thiết bị**. [Bàn giao U1–U3, kiểm chứng và ảnh sau sửa](u1-u3-handoff.md). Tiếp nối D1–D8, giữ nguồn dữ liệu CMS, bộ nhận diện xanh và logo hiện tại. Phạm vi website công khai `language-center-website`, không thay UI web quản lý hoặc app trong đợt này.

[Bàn giao U4–U6, 207 kiểm tra và ảnh sau sửa](u4-u6-handoff.md). Build, TypeScript và lint toàn website đạt. U7 đã nghiệm thu bằng service thật/MySQL bản sao và Chrome/Edge; điện thoại thật còn cần kiểm tra khi có thiết bị.

[Bàn giao U7, báo cáo Chrome/Edge và giới hạn kiểm chứng](u7-handoff.md).

## Cơ sở kiểm tra

Đã mở website đang chạy tại `http://127.0.0.1:3000`, đọc mã nguồn và dùng Chrome đo **14 tổ hợp trang/kích thước**:

- Trang chủ: 1440, 1280, 1024, 390, 320px.
- Danh sách khóa, chi tiết MG2 và tư vấn: 1440 và 390px.
- Danh sách chương trình/chi nhánh: 1440px; giáo viên: 390px.

Chỉ đọc trang, không gửi form và không thay đổi database, cấu hình hoặc service người dùng. Báo cáo gốc: `website-ui-audit.json`, `website-ui-font-audit.json`; ảnh trước sửa trong `website-ui-before/`.

## Những vấn đề đã xác nhận

| Mức | Phát hiện | Bằng chứng / tác động |
|---|---|---|
| P1 | Font trong cùng tiêu đề không thống nhất | Body dùng Arial/Segoe UI; phần nhấn dùng Georgia. Chrome thực tế dùng thêm Times New Roman cho một số glyph trong phần italic. H1 trang tư vấn có Arial + Georgia + Times New Roman. |
| P1 | Chưa có font thương hiệu được nạp chủ động | Không thấy `next/font` trong layout hoặc font WOFF/TTF local trong public; hiện phụ thuộc font hệ điều hành. Không kết luận có request tải font thất bại. |
| P1 | Ô nhập bị đậm theo label | Chrome dùng Arial-BoldMT cho input Họ tên. CSS `font: inherit` và font-weight của label làm kiểu chữ input/placeholder không tách biệt. |
| P1 | Các cỡ chữ nhỏ quá nhiều | Menu chính 13px; label/input lọc mobile 12–13px; trường form 14px; metadata quan trọng về lịch/trạng thái có nơi 11–12px. |
| P1 | Khung học phí sticky bị header che | Ở trang MG2, desktop 1440px sau cuộn: header bottom ≈143.4px nhưng aside top=110px. Chênh khoảng **33px**. |
| P1 | CSS ghi đè qua nhiều đợt | `globals.css` có luật gốc, media query, lớp L7 và D6 ghi đè cùng selector; headerInner lần lượt có các giá trị 96/86/76px ở các lớp luật. Sửa một nơi dễ tạo khác biệt tại breakpoint khác. |
| P2 | Hero trang chủ quá lớn so với nội dung tác vụ | H1 tại 1440px ≈79.2px, cao ≈335.8px; ở 320/390px vẫn 47px, cao ≈210.5px. Khoảng chữ âm và line-height sát làm dấu tiếng Việt khó cân đối. |
| P2 | Trang danh sách/tư vấn dùng headline như landing hero | H1 desktop 64px; danh sách khóa mới bắt đầu thẻ ở gần cuối màn hình 1000px. Form tư vấn cũng bị đẩy xuống bởi phần mở đầu lớn. |
| P2 | Khoảng cách và phân cấp chưa có một chuẩn | Một số vùng rộng và thoáng quá mức trong khi thông tin trong card/form lại nhỏ, tạo cảm giác phần lớn–chữ nhỏ. |
| P2 | Icon bằng ký tự Unicode không đồng bộ | Chevron trong menu có glyph từ Cambria Math; các dấu `↗`, `→`, `⌄` đang trộn với chữ và icon khác. |

Chưa ghi nhận tràn ngang toàn trang trong 14 trường hợp đã đo. Việc kiểm tra zoom 200%, trình duyệt/điện thoại khác và các trường hợp tên cực dài vẫn cần thực hiện ở U7.

## Định hướng giao diện

Một website trung tâm ngoại ngữ dễ đọc, gọn và rõ thao tác: cùng một hệ font sans hỗ trợ đầy đủ tiếng Việt, tiêu đề vừa phải, menu rõ, thẻ khóa dễ so sánh, form tư vấn dễ hoàn thành. Dùng màu xanh hiện tại làm màu nhấn. Giảm chữ nghiêng serif, chữ hoa cách ký tự quá rộng, nội dung trang trí và mũi tên dư thừa. Không đổi giao diện bằng cách tiếp tục chồng một khối override mới cuối CSS.

### Thông số thiết kế đề xuất

| Thành phần | Chuẩn hướng tới |
|---|---|
| Font | Một font sans có đủ tiếng Việt và ký hiệu tiền tệ; có các weight thật 400/500/600/700, nạp local WOFF2 hoặc cơ chế Next tương đương đã kiểm tra theo version hiện tại |
| Body / nội dung form | 16px tương đương `1rem`; line-height khoảng 1.5–1.65 |
| Label, menu, thông tin học phí/lịch/trạng thái | 14–16px; trường nhập liệu 16px; label 500, input 400 |
| Metadata phụ | 12–13px khi thật sự phụ; không dùng cho giá, trạng thái hay hướng dẫn chính |
| H1 trang chủ | Desktop khoảng 56–64px; mobile 32–38px, dùng clamp/rem và kiểm tra với nội dung tiếng Việt thực tế |
| H1 trang danh sách/chi tiết/form | Desktop 36–44px; mobile 28–32px; không cố định xuống dòng bằng `<br>` ở mọi màn hình |
| H2 / tên card | H2 28–32px desktop, 24–28px mobile; card khoảng 20–22px |
| Nội dung chính | Container tối đa khoảng 1200px; gutter mobile 16–20px, desktop 24–32px |
| Khoảng cách | Token 4/8/12/16/24/32/48/64/80; khoảng giữa section theo loại trang, không dùng một cỡ cho tất cả |
| Nút và control | Chiều cao thao tác tối thiểu 44px; nút chính/input hướng tới 48px, bo góc nhất quán |
| Header và sticky | Header desktop hướng tới 112–128px cho hai tầng; mobile 68–76px; aside và anchor lấy offset từ cùng hệ token/đo chiều cao |

Các số trên là mục tiêu thiết kế, không phải thay toàn bộ bằng một loạt `!important`. Chốt trên hai trang mẫu trước khi áp dụng toàn website.

## U1 — Font và bộ quy chuẩn nền

1. Chọn một font sans có đủ Vietnamese glyph và các weight cần thiết; so sánh trên trang chủ và form tư vấn. Đọc hướng dẫn font của Next.js được cài trong repo trước khi triển khai.
2. Đưa font về một nguồn được nạp chủ động và có fallback rõ ràng. Kiểm tra các chữ ă/â/ê/ô/ơ/ư/đ cùng dấu, chữ đậm và ký hiệu ₫.
3. Thay việc trộn Arial/Georgia/Times ở tiêu đề; xác định dùng màu/weight làm nhấn thay cho serif italic.
4. Tách kiểu label, input, placeholder, button; loại các weight 550/650/750 khi font mới không cung cấp đúng weight đó.
5. Tạo token typography, màu, spacing, radius, control và container; dùng rem cho chữ, hạn chế letter-spacing âm.
6. Phân loại CSS hiện có, xác định phần thay thế và phần giữ, chuẩn bị gỡ luật trùng. Giữ ảnh trước sửa để đối chiếu.

**Đầu ra:** font và token thống nhất; trang chủ/form mẫu không còn glyph fallback ngoài ý muốn hoặc input đậm theo label. Chưa dàn lại toàn bộ trang.

## U2 — Header, điều hướng và khung bố cục

1. Cân lại logo, tên thương hiệu, utility nav, CTA và hàng menu chương trình; giảm chiều cao/khoảng trống dư.
2. Chuẩn hóa font/weight và vùng bấm của menu; dùng SVG chevron cho dropdown, bỏ mũi tên trang trí ở nút/link.
3. Chuẩn hóa breakpoint desktop/tablet/mobile và trạng thái dropdown. Giữ Escape, focus, click ngoài và active filters.
4. Sửa sticky aside, anchor offset và thanh CTA mobile theo chiều cao header; kiểm tra trực tiếp khi cuộn, không chỉ chụp đầu trang.
5. Gom luật layout/header về vị trí sở hữu rõ ràng; bỏ các override cũ đã thay, không thêm một bộ selector cạnh tranh.

**Đầu ra:** khung trang cân đối, menu không chật và khung học phí không bị header che ở các breakpoint cần hỗ trợ.

## U3 — Trang chủ

1. Thu gọn headline, giảm xuống dòng cưỡng ép; điều chỉnh line-height/letter-spacing để dấu tiếng Việt thoáng.
2. Cân lại tỷ lệ phần chữ–ảnh; giảm chiều cao ảnh và khoảng cách trên mobile, đưa hành động chính lên sớm hơn.
3. Chuẩn hóa nút chính/phụ, phần chọn nhóm, thẻ chương trình và các section giới thiệu theo một nhịp khoảng cách.
4. Giảm chữ nhỏ, badge/caption và chữ trang trí không giúp khách chọn khóa. Ảnh vẫn có mô tả/nguồn phù hợp.
5. Kiểm tra chiều rộng 320/390/768/1024/1280/1440; không để bố cục desktop chỉ thu nhỏ nguyên trạng xuống điện thoại.

**Đầu ra:** trang chủ gọn hơn, phân cấp chữ rõ, nhìn thấy thao tác tìm khóa/tư vấn thuận tiện.

## U4 — Danh sách và thẻ khóa học

1. Thu gọn phần mở đầu để bộ lọc và khóa học xuất hiện sớm trên màn hình laptop.
2. Làm lại grid bộ lọc: label rõ, font input 16px, control đồng đều, hàng nút hợp lý; mobile có thể chia nhóm lọc để tránh form chiếm cả màn hình.
3. Chuẩn hóa card: tên, mã, chương trình, đối tượng, thời lượng, giá, CTA. Nội dung đủ dòng hợp lý, hàng giá/nút dễ so sánh giữa các thẻ.
4. Cân chiều cao card khi tiêu đề dài, thiếu ảnh, nhiều nhóm đối tượng hoặc chưa công bố giá; không để vùng ảnh trống lớn chỉ để giữ chỗ.
5. Áp dụng cùng quy chuẩn cho danh sách chương trình/giáo viên/chi nhánh, với nội dung đặc thù riêng; giữ phân trang và trạng thái lọc.

**Đầu ra:** danh sách dễ quét và so sánh; tên dài/thiếu ảnh không làm lệch giá và nút.

## U5 — Trang chi tiết

1. Cân chiều rộng bài giới thiệu và sidebar; dùng cùng chuẩn H1/H2/body, hạn chế đoạn quá dài theo chiều ngang.
2. Chuẩn hóa block điều kiện đầu vào, đối tượng, mục tiêu, nội dung và lịch tuyển sinh.
3. Giá/thời lượng/lịch rõ hơn; card lớp chia dòng và bố trí nút theo màn hình, không ép một hàng khi thiếu chỗ.
4. Kiểm tra vị trí sticky sau thay đổi header, các anchor và thanh tư vấn dưới mobile có safe area, không che nội dung/footer.
5. Áp dụng tương ứng cho chi tiết chương trình, giáo viên và chi nhánh; kiểm tra nội dung thực tế do CMS trả về.

**Đầu ra:** trang chi tiết có cấu trúc dễ đọc và CTA dễ thấy, không chồng lấn khi cuộn.

## U6 — Tư vấn và các trạng thái chung

1. Thu gọn tiêu đề mở đầu, đưa form lên sớm; cân phần form và cột hướng dẫn bên cạnh.
2. Tách label/placeholder/giá trị nhập; thống nhất font 16px cho input/select, chiều cao, spacing, required/optional, lỗi và disabled.
3. Sắp xếp nhóm thông tin cá nhân và nhu cầu học rõ ràng; giữ thứ tự và ngữ cảnh chương trình–chi nhánh–khóa–lớp hiện có.
4. Cân các dòng consent, thông tin riêng tư, nút gửi, trạng thái đang gửi/thành công/lỗi trên mobile.
5. Đồng bộ footer, breadcrumbs, trạng thái rỗng/lỗi/tải, trang giới thiệu và chính sách với hệ chữ mới.

**Đầu ra:** form dễ nhập, lỗi dễ nhận biết; không làm thay đổi payload/API hoặc tạo lead thiếu ngữ cảnh.

## U7 — Nghiệm thu giao diện và hồi quy

1. So sánh ảnh trước/sau cho các trang chính tại 320/360/390/768/1024/1280/1440; kiểm tra thêm laptop 1366×768.
2. Đo font thực tế, kích thước chữ/control, tràn ngang, cắt chữ, card và vị trí sticky. Kiểm tra zoom 200% và nội dung dài/nhiều tag/không có ảnh.
3. Kiểm tra bàn phím, focus, Escape, menu mobile, bàn phím điện thoại và trạng thái loading/error. Thử trình duyệt/thiết bị thật khi có.
4. Chạy TypeScript/production build và cập nhật các selector nghiệm thu nếu cấu trúc DOM thay đổi; giữ đúng yêu cầu chức năng, không bỏ assertion để lấy kết quả đạt.
5. Chạy lại luồng nhóm → khóa → lớp → tư vấn → lead trên MySQL bản sao; kiểm tra phân trang, bộ lọc và ngữ cảnh sau thay đổi UI.
6. Tổng hợp báo cáo và ảnh. Tách rõ kiểm tra Chrome mô phỏng với điện thoại thật; không deploy trong giai đoạn này.

**Tiêu chí đóng:** font Việt nhất quán, chữ tác vụ đủ lớn, không bị header/CTA che, bố cục cân đối trên các màn hình đã chọn, menu/form/bộ lọc hoạt động đúng, build đạt và có ảnh trước/sau.

## Thứ tự thực hiện đề xuất

Làm **U1 + U2 trước**, vì mọi trang đang phụ thuộc vào font, token và header. Sau đó U3 → U4 → U5 → U6; cuối cùng U7. Mỗi giai đoạn cần tự kiểm tra phần vừa sửa trước khi chuyển tiếp. Không cần xóa/nạp lại dữ liệu, đổi backend, đổi logo hoặc thêm thư viện UI để thực hiện lộ trình này.
