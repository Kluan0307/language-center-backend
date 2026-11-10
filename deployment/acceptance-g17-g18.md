# Chạy lại G17–G18 và mở dữ liệu demo

Cập nhật 10/11/2026. Đây là nghiệm thu qua gateway, MySQL, năm JVM và Edge thật. Bộ cũ tự mô phỏng trạng thái đã được lưu lịch sử; hai lệnh cũ chuyển sang bộ thực thi này.

## Chuẩn bị

Chạy PowerShell tại thư mục backend chứa `pom.xml` tổng và `scripts`. Cần Java 21, Maven, Node 22+, MySQL 8 và MySQL CLI, Microsoft Edge ở đường dẫn cài đặt Windows mặc định. Frontend nằm tại `../../language-center-web-frontend`, đã cài dependency bằng `npm ci`. Runtime dùng Vite phục vụ source hiện tại; không phải kiểm tra bộ release G20.

`.env.local` cần có `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`. Tài khoản MySQL phải có quyền backup và tạo schema thử. Runtime đổi tên database trong URL sang bản sao mới; không tự chạy migration trên database gốc. Không mở/đóng các service người dùng đang chạy trên 8080–8084.

Nguồn cần có tài khoản Super Admin đăng nhập được. Nếu khác tài khoản thử mặc định của dự án, đặt `ACCEPTANCE_BOOTSTRAP_USERNAME` và `ACCEPTANCE_BOOTSTRAP_PASSWORD` trong môi trường PowerShell. Không sửa mật khẩu/tài khoản gốc để chạy nghiệm thu.

Các cổng riêng phải trống: `28080–28084`, `28090` (relay mất phản hồi sau commit), `25173` (FE), `29388` (Edge CDP). Buổi workshop thử là 00:00–03:00 ngày chạy theo giờ Việt Nam; chạy sau 03:00, tránh qua nửa đêm trong cùng lượt.

## Chạy trọn bộ

Build backend trước, kiểm tra Maven kết thúc thành công:

```powershell
mvn clean package -DskipTests
```

Sau đó chạy, cung cấp **tất cả thư mục chứa file tài liệu đang được database gốc tham chiếu**. Với dữ liệu local đã kiểm tra trong đợt này:

```powershell
.\scripts\acceptance\run.ps1 -Documents storage/improvement6-documents
```

Nếu tài liệu nằm ở nhiều nơi, truyền mảng `-Documents @('duong-dan-1','duong-dan-2')`. Thiếu file được tham chiếu sẽ làm chuẩn bị thất bại; không tạo file giả để vượt kiểm tra.

Lệnh tạo backup `--online-rehearsal`, restore vào schema `lc_rehearsal_<random>`, áp dụng V001–V013 trên bản sao, chạy `prod` + `ddl-auto=validate`, chạy lần lượt các phase và dừng tiến trình riêng trong `finally`. Nó giữ database, file, log và bằng chứng. Snapshot online này không phải điểm phục hồi production đã được chứng nhận; giữ nguồn không có ghi mới trong lượt chạy để đối soát fingerprint cuối lượt.

`storage/acceptance/latest.txt` chứa đường dẫn lượt mới nhất. `acceptance-summary.json` chỉ được ghi khi đủ **86/86** kiểm tra: G17 API20, CRUD8, G18 API38, browser12 và SQL/file8. Health smoke riêng có19 kiểm tra. Mỗi bước có `*.console.log`; lỗi dừng lượt, không đổi lỗi thành PASS.

## Chạy từng bước / điều tra lỗi

```powershell
node scripts/acceptance/prepare.cjs --documents storage/improvement6-documents
.\scripts\acceptance\runtime.ps1 -Action start -Service all
node scripts/acceptance/g17.mjs
node scripts/acceptance/g18.mjs
node scripts/acceptance/g17-crud.mjs
node scripts/acceptance/browser.mjs
node scripts/acceptance/verify-db.mjs
node scripts/acceptance/summarize.mjs
.\scripts\acceptance\runtime.ps1 -Action stop -Service all
```

Khi chạy tay, dừng runtime trong `finally` nếu xảy ra lỗi. Không chạy cùng lúc hai lượt trên cùng bộ cổng. Phase đã PASS từ chối chạy lại trên dữ liệu đã thay đổi; muốn nghiệm thu lại, chuẩn bị lượt mới. G17 đã ghi report cũng yêu cầu lượt mới khi lỗi giữa chừng. G18/browser thất bại giữ report cũ và tạo lần thử mới có ghi vết; để chứng minh tái tạo sạch, dùng `run.ps1` từ đầu.

Nếu cần làm việc với lượt cũ, đặt `$env:ACCEPTANCE_RUN_DIR` cho các script Node và truyền cùng đường dẫn bằng `-RunDir` cho `runtime.ps1`. `run.ps1` dùng lượt mới; bỏ biến `ACCEPTANCE_RUN_DIR` cũ trước khi chạy trọn bộ.

## Xem demo sau khi nghiệm thu

```powershell
$acceptanceDir = (Get-Content storage/acceptance/latest.txt -Raw).Trim()
.\scripts\acceptance\runtime.ps1 -Action start -Service all -RunDir $acceptanceDir
```

Mở `http://127.0.0.1:25173`. Tên/mật khẩu **tài khoản mới của riêng bản sao** nằm ở `private-session.json` trong thư mục lượt, nhóm `accounts`: `admin`, `staff`, `teacher`, `staffB`; Super Admin dùng thông tin nguồn. Không đưa file này, token hoặc dump database lên Git/báo cáo. ID/tên của chi nhánh, lớp và học viên nằm trong `fixture.json` đã loại password/token. Các tài khoản QA khác và lượt lỗi được giữ để đối chiếu.

Demo dữ liệu đã hoàn thành theo thứ tự:

1. Staff mở học viên `fixture.student` → Hồ sơ học tập: khoảng học, PRESENT1, kết quả Đạt; không có quyền quản lý điểm.
2. Admin mở lớp `fixture.schoolClass` → kết quả cuối khóa: A85/PASS, B40/FAIL. Mở lịch sử để xem các revision và snapshot bất biến. Teacher đọc lớp được giao, không có nút chốt/mở lại.
3. Mở đăng ký A/B: cả hai COMPLETED và tham chiếu bản chốt hiện hành. B còn nợ3.000.000; nợ học phí không đổi kết quả học tập.
4. Học phí A có hai phiếu1.000.000 và2.000.000, dư nợ0. Báo cáo **đúng chi nhánh A và đúng ngày `fixture.today`**: thu/ròng3.000.000, công nợ3.000.000. Các chi nhánh QA có tiền thử riêng, không gộp vào con số demo này.
5. Lớp đã đóng có kiểm toán (`closed=true`, `VERIFIED`), khác với trạng thái lịch FINISHED. Muốn diễn lại từ nhập điểm đến chốt thì tạo lượt mới; không sửa trực tiếp snapshot bằng SQL.

Đây là workshop ngắn180phút để kiểm tra toàn bộ vòng đời trong một ngày, không phải khóa45giờ hoặc chứng nhận CEFR. Đầu vào so tập trình độ B1 được chấp nhận, không tự đặt ngưỡng70 hay hạn duyệt7ngày. A đi đúng điều kiện; B đầu vào A2 đi qua đề nghị/duyệt/tiêu thụ ngoại lệ thực tế.

Kết thúc:

```powershell
.\scripts\acceptance\runtime.ps1 -Action stop -Service all -RunDir $acceptanceDir
```

## Bằng chứng và giới hạn

`g17.json`, `g18.json`, `g17-crud.json`, `browser.json`, `database.json` lưu expected/actual, thời gian và trạng thái; `http.jsonl` loại password/token; PNG chụp UI thật; `lifecycle.log` ghi dừng/khởi động JVM và hash JAR. Manifest tổng ghi hash file và harness tại bàn giao. Không sửa kết quả thất bại để làm đẹp số liệu.

Các race dùng request HTTP đồng thời và đối soát trạng thái lưu. Lỗi phụ thuộc dừng JVM riêng thật rồi khởi động lại; relay cắt kết nối sau upstream commit để kiểm tra retry cùng intent. SQL đối soát số phiếu, tiền, ghế cuối, final/barrier và snapshot cũ; fingerprint nguồn phải bằng backup trước chạy.

Kết quả chỉ áp dụng cho bộ ca đã chạy trên máy local. Không suy ra đã kiểm thử tải, Nginx/TLS/Internet, mọi tổ hợp quyền hoặc bộ release G20. Báo cáo G19 và gói G20 cũ cần cập nhật theo bằng chứng mới. Xem [G17](../docs/thesis-progress/G17.md), [G18](../docs/thesis-progress/G18.md).
