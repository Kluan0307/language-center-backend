# Chương 5 — Cài đặt và môi trường chạy

## 5.1. Tổ chức mã nguồn

Backend có parent pom.xml và năm module api-gateway/auth-service/academic-service/teaching-service/finance-service. Java source của từng module theo package com.example.<service>; tài liệu ở docs, công cụ ở scripts. Frontend cùng cấp thư mục backend ngoài cùng; entry/route ở src/App.tsx, API client/service và component/page theo cấu trúc đã có. Không tạo cấu trúc src/routes hoặc module TeacherManagement mới chỉ để khớp sơ đồ.

Teacher route dùng AccountManagement fixedRole=TEACHER cho quản trị và TeacherDirectory cho Staff. SessionManagement tích hợp trong ScheduleManagement. Sidebar và route guard phản ánh4 role; quyền cuối cùng vẫn được service kiểm. Hướng dẫn hiện hành ở [README backend](../../../README.md) và [README FE](../../../../../language-center-web-frontend/README.md).

## Giao diện cập nhật11/11

Có trang /administrators và /programs riêng cho Super Admin; lịch học/lịch dạy dùng FullCalendar Weekly Time-grid, ngày hoặc danh sách. Bộ lọc và ngày lưu URL, lịch lấy buổi thực tế thay vì tự lặp lịch mẫu. Đã dọn các trang mock không reachable; lịch sử trình bày trước/sau theo nhãn nghiệp vụ. Xem [23route và mốc kiểm thử](../../current-state.md).

## 5.2. Cấu hình

| Thành phần | Cổng mặc định local | Cổng nghiệm thu riêng |
|---|---|---|
| Gateway |8080|28080|
| Auth |8081|28081|
| Academic |8082|28082|
| Teaching |8083|28083|
| Finance |8084|28084|
| Frontend |5173|25173|

DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET cần cấu hình đúng môi trường; không ghi secret trong báo cáo. application.properties import .env.local theo cấu hình module hiện tại, Maven không tự đọc mọi file .env bất kỳ. CORS/origin và service URLs phải cùng bộ runtime. Hướng dẫn [nghiệm thu/demo](../../../deployment/acceptance-g17-g18.md) dùng PowerShell từ backend root và schema/file riêng.

Runtime nghiệp vụ dùng ngày Asia/Ho_Chi_Minh. Môi trường thử prod/ddl-auto=validate cần schema phù hợp migration V001–V013; không đổi sang update để bỏ qua schema thiếu. Giữ nguồn, backup trước và chạy migration trên bản sao theo runbook. Không reset database hoặc xóa tài khoản nguồn để demo.

## 5.3. API và hợp đồng

Kiểm kê controller hiện tại có173cặp method/path chính: Auth19,Academic114,Teaching28,Finance12; ngoài ra3alias và2diagnostics. [Danh sách](../../api-list.md) và [contract/body mẫu](../../api-contracts.md) mô tả quyền và các giới hạn. Các route nội bộ cũng cần token/phạm vi; không suy /api/v1 là API công khai.

Ví dụ giao thức quan trọng: PUT /api/v1/teaching/grades; POST /api/v1/teaching/classes/{classId}/final-results/{enrollmentId}/confirm với requestId/fingerprint/reason; reopen dùng expectedRevision. Completion Academic cần finalResultId/revision hiện hành. Giao tiếp HTTP thường truyền Bearer token của người gọi; Gateway không cấp role mới.

## 5.4. Môi trường demo và file

Bộ86 ca tạo fixture qua API trên bản sao lc_rehearsal_* và thư mục documents riêng; tài khoản mới có mật khẩu ngẫu nhiên/đổi bắt buộc. fixture.json lưu ID/tên, private-session.json chỉ dùng local, không đưa vào phụ lục. PDF/revision cần cả file storage; backup SQL đơn lẻ không đủ phục hồi.

Chạy lại theo [run.ps1](../../../scripts/acceptance/run.ps1) sau khi build JAR/cài dependency FE. Script chờ đủ5readiness, chạy các phase rồi dừng runtime riêng. Các schema/file/bằng chứng giữ để đối chiếu; không tự DROP. Có thể khởi động lại runtime đó để xem demo đã hoàn thành như phụ lụcA.

## 5.5. Mức sẵn sàng triển khai

G17/G18 xác minh JAR/source local qua Gateway/MySQL/Edge, không chứng nhận gói g20-final-release cũ. G20 phải đóng gói lại tài liệu/hình mới, kiểm manifest, restore schema/file cuối và chạy đúng release. Internet/TLS/reverse proxy, secret rotation, kiểm tải và mục tiêu RPO/RTO cần môi trường và nghiệm thu riêng; không gọi danh sách cấu hình là bằng chứng production đã hoạt động.

## Cập nhật sau nghiệm thu G20

G20 đã hoàn thành release local900artifact: kiểm tra86ca trên JAR/FEbuild, phục hồi dữ liệu/file và diễn tập4role trước/sau restore đạt. Các mục cần xác minh release nêu ở bản thảo trước đã có [bằng chứng G20](../../thesis-progress/G20.md); bản nộp, người trình bày/máy bảo vệ, Internet/TLS/load vẫn chưa được xác nhận. Không thay kết quả khảo sát còn thiếu bằng kết quả kiểm thử kỹ thuật.
