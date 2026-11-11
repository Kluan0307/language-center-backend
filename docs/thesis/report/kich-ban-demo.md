# Phụ lục A — Kịch bản demo15–20 phút

**Mốc hiện hành11/11:** [23route, Time-grid và chức năng](../../current-state.md), [bằng chứng335BE/96FE/86tích hợp và phục hồi](../../reviews/2026-11-11-phase-5-completion.md). Chọn đúng bộ dữ liệu và ngày khi demo; gói mới cần đọc qualification đi kèm. Khảo sát/mẫu khoa chưa được tự điền.

Cập nhật10/11/2026 theo fixture thật G17/G18. Mặc định trình bày **dữ liệu đã nghiệm thu**, không bấm lại các thao tác tạo/thu trên trạng thái đã hoàn thành. Kịch bản là hướng dẫn diễn tập; chưa ghi nhận một buổi bảo vệ liên tục có Hội đồng hoặc video chính thức.

## Chuẩn bị

Đọc [hướng dẫn mở runtime](../../../deployment/acceptance-g17-g18.md). Backend root chứa scripts/acceptance; dùng runtime.ps1 -Action start -Service all -RunDir với thư mục lượt thử. Chờ đủ service; mở **http://127.0.0.1:25173**, Gateway28080. Không dùng bộ18080 hoặc tài khoản staff_hcm/admin_hcm tự đặt trong fixture mô phỏng cũ.

Lượt đã kiểm20261110-0365823a lưu trong storage/acceptance. fixture.json có ID/tên mới: A branch34,B35,lớp53,học viênA52/B53,enrollment43/44,tuition20/21. Ưu tiên đọc fixture để không nhầm sau lượt mới. Tài khoản mới/mật khẩu nằm local tại private-session.json/accounts; nhóm admin/staff/teacher, multiAdmin1 là Admin hai chi nhánh của lượt này. Không chiếu file/token/password trước Hội đồng. Super Admin dùng tài khoản bản sao nguồn, không công bố thông tin xác thực trong slide.

Dùng **cửa sổ/profile riêng** cho từng role; các tab cùng cửa sổ có thể dùng chung context/storage tùy cách đăng nhập, không giả định ba tab luôn là ba phiên độc lập. Ngày báo cáo chọn fixture.today (10/11/2026), scope chi nhánhA; dữ liệu QA ở chi nhánh khác.

## Lộ trình demo mặc định

| Phần | Thời gian | Thao tác và điều cần thấy |
|---|---|---|
| Giới thiệu |1 phút|Nêu4 role, chuỗi quản lý, giả định và shared schema; Super Admin xem chi nhánh/phân công, không đổi cấu hình|
| Hồ sơ/tuyển sinh |3 phút|Staff mở học viênA và lịch sử từ Lead; xem đầu vàoB1, enrollment. B đầu vàoA2 dùng ngoại lệ đãCONSUMED. Giải thích tập level chấp nhận, không ngưỡng70 hoặc hạn7ngày|
| Học phí |3 phút|Staff mở tuitionA:2phiếu1M+2M, PAID/dư0; B chưa thu/nợ3M. Xem bằng chứng retry cùng ID ở G17-08/G18-13, không bấm thu thêm trên bill đã trả hết|
| Giảng dạy |3 phút|Teacher mở lớp chính/buổi, sheet đã điểm danh và bài kiểm tra. Workshop180 phút, điểmA85/B40; không bấm hoàn tất buổi riêng nếu UI hoàn tất qua lưu sheet|
| Kết quả/học vụ |4 phút|Teacher đọc final không nút chốt/mở; Admin mở lịch sử snapshot/ngưỡng60, trạng tháiA PASS/B FAIL; đăng ký đềuCOMPLETED với refs hiện hành; lớp có closure VERIFIED|
| Báo cáo/phạm vi/kết thúc |3 phút|Admin xem báo cáo ngày/chi nhánhA:thu3M,void0,refund0,ròng3M,nợ3M. Admin nhiều chi nhánh chuyểnA→B, bảng đổi phạm vi; chỉ role đủ phân công mới có lựa chọnB|
| Tổng |17 phút|Chừa2–3 phút cho chuyển phiên hoặc giải thích lỗi|

Các màn hồ sơ/final/closure/tuition/chuyển chi nhánh đã có assertion Edge trong UI-01–11; không suy rằng mọi bước thêm dữ liệu/diễn thuyết của phụ lục đã được chạy thủ công một lượt. Hình và expected/actual nằm ở [chương 6](ch06-kiem-thu-danh-gia.md).

## Nếu muốn diễn trực tiếp hiệu chỉnh/chốt lại

Chỉ thực hiện trên bản sao thử và diễn tập trước; thao tác này đổi golden state nên không dùng cùng report86 ca làm bằng chứng của trạng thái sau sửa. Luồng đúng:

1. Admin mở lại **kết quả toàn lớp** với lý do và expectedRevision hiện tại; snapshot cũ thành REOPENED, không bị ghi đè.
2. Nếu lớp đã đóng, mở lại lớp ở Academic có lý do/revision; không chỉ đổi FINISHED bằng endpoint trạng thái cũ.
3. Teacher sửa điểm theo quyền, hoặc Admin sửa điểm đã có với lý do. Không cho Admin nhập điểm ban đầu thay Teacher.
4. Admin preview mới rồi **chốt lại từng enrollment** cần final. Giữ requestId/fingerprint/reason cho mỗi intent; không dùng fingerprint từ trước thay đổi nguồn.
5. Staff/Admin đồng bộ lại COMPLETED cho từng enrollment bằng version và finalResultId/revision mới. Không dùng refs cũ vì phải409.
6. Admin đóng lớp sau khi đủ phút, không STUDYING/RESERVED và refs khớp. Nếu có blocker thì hiển thị lý do, không xóa dữ liệu/ép trạng thái bằng SQL.

Để diễn từ tạo Lead đến thu lần đầu, chuẩn bị **lượt mới** qua run.ps1 và dùng báo cáo/HTTP evidence; runner tự đưa dữ liệu tới trạng thái cuối. Không gọi việc mở dữ liệu cuối là demo trực tiếp mọi thao tác tạo. Nếu cần seed dừng giữa chừng cho buổi bảo vệ, phải tách kịch bản và nghiệm thu riêng; hiện chưa bàn giao seed tương tác đó.

## Phương án khi sự cố

Nếu dịch vụ chưa ready hoặc lỗi mạng, kiểm status và log/requestId theo runbook. Không gửi giao dịch với key mới chỉ vì chưa thấy phản hồi; xác minh/retry cùng intent. Có thể mở ảnh/report đã lưu và nói rõ là bằng chứng lượt thử ngày 10/11, không giả vờ là phản hồi đang chạy. Không dừng dịch vụ gốc8080–8084 hoặc reset schema nguồn khi trình bày.

Kết thúc dùng runtime.ps1 -Action stop -Service all với đúng RunDir; giữ schema/file/log. Trước bảo vệ thật cần diễn tập lại trên máy và thời lượng theo mẫu khoa.

## Cập nhật sau diễn tập release G20

Đã đăng nhập UI4role và kiểm tra các bước chính trên FEbuild/JAR gói release, lặp lại sau phục hồi; thêm lịchTeacher /my-schedule, BFAIL/COMPLETED, snapshot và báo cáo tài chính. Xem [G20](../../thesis-progress/G20.md) và [checklist mở demo](../../../deployment/defense-rehearsal-checklist.md). Chưa phải một buổi nói liên tục15–20phút hoặc thử máy bảo vệ của người dùng.
