# D5–D8 — Website đồng bộ bộ dữ liệu mới

Hoàn tất phạm vi code/local và nghiệm thu tự động ngày **17/11/2026**. Không deploy. Bộ dữ liệu nghiệp vụ D4 vẫn được giữ; các lead và thay đổi trạng thái phục vụ kiểm thử chỉ nằm trên database bản sao đã dọn.

## D5 — Nội dung đã xuất bản local

Đã xuất bản bằng API CMS với tài khoản SUPER_ADMIN, theo thứ tự cha → con và có backup trước khi chạy:

| Nguồn | Đã công khai |
|---|---:|
| Chương trình | 4 |
| Chi nhánh | 4 |
| Khóa học | 20 |
| Lớp sắp mở còn chỗ | 4 |
| Hồ sơ giáo viên | 8 |

Đây là 32 dòng `website_publications` và 8 hồ sơ `website_teacher_profiles`. Không xuất bản lớp đang học, lớp đã hết lịch, dữ liệu học viên, điểm, học phí cá nhân hoặc tài khoản. Giá khóa học được phép công khai; hồ sơ giáo viên không hiển thị bằng cấp chưa được xác minh (`showQualification=false`). Không thêm thành tích, chứng nhận hoặc cam kết điểm thi.

Nội dung phù hợp bộ mẫu local, lấy tên/giá/thời lượng/chương trình/đối tượng từ hồ sơ nghiệp vụ hiện tại. Chi tiết lớp dùng snapshot chương trình của lớp. Danh mục không được sao chép thành danh sách khóa hardcode ở frontend.

`publish-refresh.cjs` có chế độ xem kế hoạch mặc định; `--apply-local` mới thực hiện xuất bản. Chạy lại hai lần trên bản sao MySQL đã được kiểm chứng: mỗi lần giữ nguyên cả 40 đối tượng, không thêm dòng lịch sử chỉnh sửa. Không đổi slug đang công khai một cách âm thầm.

## D6 — Menu và quy trình duyệt khóa

Menu chính:

`Mẫu giáo · Tiểu học · THCS · Người lớn ▾ · IELTS ▾ · TOEIC ▾ · Giới thiệu ▾`

- Mẫu giáo/Tiểu học/THCS mở bộ lọc đối tượng chuẩn. Người lớn có lựa chọn tổng quát/giao tiếp theo chương trình đã công khai.
- IELTS/TOEIC dùng slug chương trình lấy từ backend; có lựa chọn đối tượng phù hợp theo dữ liệu khóa đang giới thiệu. THPT vẫn có trong các bộ lọc và menu con phù hợp.
- Thanh phụ có Tất cả khóa học, Chương trình, Giáo viên và Chi nhánh; CTA là **Tư vấn ngay**.
- Giữ logo và màu xanh hiện tại. Desktop có dropdown; mobile có menu cuộn, accordion, nút đóng và không tràn ngang ở 320/390px. Escape đóng dropdown và trả focus về nút mở; mở mục mới đóng mục trước.
- Giữ đối tượng, chương trình, chi nhánh và học phí khi lọc/phân trang danh sách. Khi vào khóa học, nhóm quan tâm và chi nhánh được giữ để lọc lớp, điền form tư vấn và chuyển nhu cầu nhóm vào ghi chú có thể sửa.
- Kết quả rỗng vẫn giữ chương trình/chi nhánh khi chuyển sang tư vấn. Không tạo dữ liệu giả để lấp kết quả rỗng hoặc lỗi backend.
- Header nhận cấu hình từ các danh mục/publications của backend. Khi backend lỗi, header vẫn có lối tìm khóa và giới thiệu; không tự sinh các khóa/chương trình chưa được công khai.

Mã website được cập nhật trong `language-center-website/src/lib/navigation*`, `components/header.tsx`, các trang/card khóa học, `layout.tsx` và `globals.css`. Cấu hình API/BFF key giữa website và backend đã kiểm tra khớp; không cần đổi key hoặc biến môi trường trong đợt này.

Ảnh kiểm tra: [desktop](previews/d6-home-desktop.png), [lọc THCS](previews/d6-thcs-desktop.png), [menu mobile](previews/d6-menu-mobile.png).

## D7 — Kết quả nghiệm thu

| Phạm vi chạy trong đợt này | Kết quả |
|---|---|
| Website production build / TypeScript | Đạt |
| API với 5 service thật | 54 kiểm tra đạt |
| Chrome với Gateway + API + MySQL bản sao thật | 21 kiểm tra đạt |
| Hồi quy Chrome với HTTP fixture | 38 kiểm tra đạt |
| Quy tắc ánh xạ + fingerprint | 6 unit test đạt |
| Audit database local | 63 kiểm tra quan hệ/nghiệp vụ và 36 fingerprint đạt |
| Chạy lại xuất bản | 2 lần × 40 đối tượng giữ nguyên, không thêm editorial event |

5 service là auth, academic, teaching, finance và API Gateway, chạy trên cổng riêng. Browser gửi form theo đường **Next.js BFF → Gateway → academic-service → lead**, không chỉ gọi API trực tiếp. Database kiểm thử là bản sao của bộ dữ liệu hiện tại, được xóa sau khi kết thúc. Thông tin đăng nhập và key không được đưa vào báo cáo hoặc JavaScript trình duyệt.

Các ca đã kiểm tra gồm:

- Tám tài khoản mẫu đăng nhập được mà không đổi mật khẩu; account STUDENT liên kết đúng profile mới.
- Website và assistant-query trả cùng tập khóa theo đối tượng trong phạm vi tương ứng, không đếm trùng khóa nhiều nhóm.
- Admin chỉ xem đúng chi nhánh; Staff/Teacher không được vào CMS; giáo viên chỉ nhận lớp được phân công.
- Student đọc lớp, lịch, điểm, điểm danh, học phí và phiếu thu của mình; đọc đăng ký/phiếu thu người khác bị từ chối.
- Dropdown, focus/Escape, bộ lọc, phân trang, nhóm quan tâm, chi nhánh và form hoạt động trên desktop/mobile.
- Form tạo đúng lead ở chi nhánh được chọn, giữ khóa và nhóm quan tâm; cặp lớp/chi nhánh sai bị backend từ chối.
- Lớp đầy, khóa đóng, chi nhánh ngừng hoạt động và nội dung chưa xuất bản không được hiện công khai; khóa chung ở chi nhánh khác vẫn còn hiển thị đúng.
- Kết quả rỗng, retry form, backend lỗi và bí mật BFF không xuất hiện trong client bundle.

### Lỗi dữ liệu phát hiện và đã sửa

Fingerprint điểm của seed JavaScript chưa khớp Java với số thập phân có số 0 cuối: ví dụ `10.00` sau `stripTrailingZeros()` được Java ghi thành `1E+1`, khác chuỗi `10` của JavaScript. Đã sửa serializer seed và kiểm chứng khớp service thật cho cả 36 bài.

Sau backup và xác minh điểm/candidate không đổi, metadata fingerprint của **28** lần công bố cần sửa đã được sửa trên local; 8 lần còn lại vốn khớp. Không thay điểm, lời nhận xét, roster hoặc kết quả công bố. Tool sửa từ chối nếu bài/điểm đã được người dùng chỉnh sau seed; không dùng nó để che việc thay đổi điểm thực tế.

## D8 — Cách chạy và công cụ tái tạo

Từ thư mục backend, mở các terminal riêng và chạy service như hiện tại:

```powershell
.\scripts\run-service.ps1 -Service auth-service
.\scripts\run-service.ps1 -Service academic-service
.\scripts\run-service.ps1 -Service teaching-service
.\scripts\run-service.ps1 -Service finance-service
.\scripts\run-service.ps1 -Service api-gateway
```

Các dòng trên là cho từng terminal riêng, không phải chờ một service thoát rồi chạy dòng kế. Muốn dùng chatbot, chạy thêm assistant-service theo cấu hình model đang có.

Tại thư mục website:

```powershell
npm run dev
```

Mở địa chỉ local do Next.js in ra (mặc định `http://localhost:3000`). Web quản lý và app tiếp tục chạy bằng lệnh hiện có. Đăng nhập lại nếu vẫn giữ phiên từ trước D4. `student1`–`student5` giữ mật khẩu cũ; `student5` có lớp hiện tại, lượt học trước đó, điểm và hai phiếu thu để thử luồng học viên.

### Kiểm tra lại, không thay dữ liệu nguồn

```powershell
node scripts\website\align-local-config.cjs
node scripts\website\audit-published-refresh.cjs
node scripts\website\rebuild-local-demo.cjs
```

Hai lệnh đầu kiểm tra cấu hình và dữ liệu; chỉ ghi báo cáo local. Lệnh cuối **mặc định dry-run**, in các bước sẽ chạy, không xóa/chèn database. Audit seed có các kỳ vọng dành cho bộ mẫu tại thời điểm bàn giao; khi người dùng thay dữ liệu nghiệp vụ hợp lệ, cần đánh giá lại kỳ vọng thay vì xóa thay đổi đó để cho audit xanh.

### Khi chủ động muốn tạo lại toàn bộ bộ mẫu

Sau khi dừng backend đang ghi dữ liệu, build artifact hiện tại nếu cần và dùng:

```powershell
node scripts\website\rebuild-local-demo.cjs --reset-and-publish-local
```

**Lệnh có flag này thay dữ liệu nghiệp vụ và metadata website**, giữ tài khoản/nhân sự/chi nhánh/phân quyền theo D1. Nó chạy inventory → plan → backup/restore verification → seed → rehearsal/rollback probe → apply → xuất bản, với guard localhost/schema, cổng service, fingerprint/precondition và transaction. ID/mã tiếp tục tăng, không tái dùng dải cũ. Không tự reset mật khẩu; nếu superadmin đã đổi mật khẩu, cấp `REFRESH_ADMIN_PASSWORD` qua môi trường cho bước đăng nhập xuất bản.

Đã kiểm thử dry-run và các bước riêng ở D1–D7; không chạy lại reset lần nữa ở D8 chỉ để thử wrapper. Nếu dừng ở bước xuất bản, sửa nguyên nhân rồi chạy lại riêng `publish-refresh.cjs --apply-local`, không cần xóa lại dữ liệu nghiệp vụ. Metadata xuất bản đã được kiểm tra chạy lặp an toàn.

Backup nằm trong vùng private `storage/data-refresh/`. Việc phục hồi toàn bộ cần dừng service ghi và thu hồi phiên theo hướng dẫn D1–D4. Không đưa SQL backup có hash mật khẩu lên Git hoặc website.

### Lệnh nghiệm thu trên bản sao

```powershell
node --test scripts/identity/course-classification-rules.test.cjs scripts/identity/grade-fingerprints.test.cjs
node scripts/website/verify-refresh.cjs
node scripts/website/verify-l67.cjs
node scripts/website/verify-publication-replay.cjs
```

Nghiệm thu cần Chrome trên Windows, dependencies/build website và các jar service hiện tại. Các cổng 31180–31184, 31200/31227 và cổng fixture 3010/19082/19227 phải rảnh. Không thay service của người dùng trên cổng mặc định. Mọi service/browser do công cụ mở được dừng khi kết thúc; không deploy hay để lại database thử.

## Giới hạn kiểm chứng còn lại

- Chưa thử lại APK trên điện thoại thật; đã thử API học viên bằng token và profile mới.
- Chưa gọi model Gemini/OpenAI thật; đã đối chiếu dữ liệu các công cụ tra cứu của chatbot.
- Chưa tạo một giao dịch VNPAY sandbox mới; các phiếu thu mẫu là CASH/BANK_TRANSFER, không phải giao dịch nhà cung cấp giả. Cấu hình VNPAY hiện tại giữ nguyên; lịch sử sandbox bên ngoài không bị xóa theo database local.

Không cần deploy để thực hiện các bước xác nhận thủ công này. Báo cáo tổng hợp: `d5-d8-validation.json`; chi tiết API/browser: `d7-validation.json`; audit nguồn: `d8-final-audit.json`; kiểm tra xuất bản lặp: `d8-publication-replay.json`.
