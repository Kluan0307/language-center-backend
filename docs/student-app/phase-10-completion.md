# Student App — G10: dọn bản bàn giao và nghiệm thu

Ngày: 12/11/2026. Đã dọn runtime, kiểm thử tự động, export Android release và **build thành công APK cài nội bộ**. Người dùng chọn **chưa có thiết bị, hoàn thiện build và hướng dẫn trước**. Nghiệm thu cài đặt/chạy trên Android thật và push thật vẫn chờ, không được tính PASS từ kiểm thử trình duyệt.

## Phần đã dọn

- Gỡ hẳn cơ chế preview khỏi session manager/provider, login, route gate, header và hồ sơ. Không còn nút “Khám phá giao diện” hoặc đường vào không đăng nhập. Các route nghiệp vụ vẫn bắt xác thực và đổi mật khẩu khi được yêu cầu.
- Gỡ hướng dẫn số thẻ/OTP thử nghiệm khỏi màn hình thanh toán; chuyển sang README. Giữ nhãn **VNPAY sandbox** vì backend đang dùng sandbox, không giả thành cổng tiền thật.
- Gỡ flag mở cổng mô phỏng localhost khỏi runtime app. Checkout chỉ mở đúng HTTPS host/path VNPAY sandbox, chữ ký có định dạng hợp lệ và phiên còn hiệu lực. Bộ kiểm thử/backend protocol simulator vẫn được giữ ngoài runtime bàn giao.
- Cài đặt chỉ hiển thị thao tác đăng ký push khi backend và nền tảng hỗ trợ; phiên bản chưa có push hiển thị thông tin inbox, không bày các nút đăng ký/toggle không dùng được.
- Thay icon Expo mẫu bằng biểu tượng sách theo màu nhận diện của ứng dụng, có nguồn vector; gỡ một ảnh splash mẫu không còn được tham chiếu.
- README app được viết lại theo hiện trạng, bỏ các mô tả G2/G4 đã lỗi thời. Giữ trạng thái loading, trống, lỗi, retry và cảnh báo tài chính vì đó là hành vi nghiệp vụ cần thiết.

Không xóa `tests/`, Java test, test fixture hoặc bằng chứng kiểm chứng chỉ để giao diện sạch hơn. Chúng không phải màn hình người dùng và không trở thành tính năng của APK.

## Cấu hình và công cụ bàn giao

- `eas.json`: development client; preview APK nội bộ; production AAB. Hai profile bàn giao không dùng development client và yêu cầu cấu hình release.
- `app.config.ts` + `config/release.cjs`: Android package mặc định `com.languagecenter.student`, API HTTPS cho release, UUID project khi có EAS, chặn biến public có tên secret/password/private key và chặn payment protocol simulator.
- `scripts/check-release.cjs`: kiểm tra config, asset và các dấu vết UI phát triển đã dọn. Không in secret vào log.
- `scripts/build-android-local.cjs`: prebuild khi cần, giới hạn bộ nhớ/worker và xử lý đường dẫn dài Windows bằng drive alias tạm. Không di chuyển source hoặc đổi registry. APK/manifest/SHA-256 được lưu trong `artifacts/` khi build thành công.
- `.easignore`/`.gitignore`: không đưa env local, credential, keystore, logs, tests và artifact cũ vào gói upload/bản nguồn công khai. `expo-system-ui` được cài đúng SDK để áp dụng giao diện sáng của Android.
- `scripts/student-app/phase10/verify.cjs` tại backend: chạy kiểm thử app/web hoặc 5 service tuần tự, có giới hạn tài nguyên.
- `scripts/student-app/phase10/check-artifacts.cjs`: kiểm tra bundle/APK không chứa các secret backend đã biết hoặc nội dung thử nghiệm đã loại khỏi runtime; không in giá trị secret.

Không đổi secret/merchant VNPAY, không reset dữ liệu, không sửa khóa học/lớp/học phí để chạy kiểm thử. Các bài kiểm thử backend dùng H2; kiểm tra API/UI thật chỉ đọc và đăng nhập tài khoản đã được cấp.

## Kết quả kiểm chứng

| Phạm vi | Bằng chứng / kết quả |
|---|---|
| Auth | 68 test đạt |
| Academic | 225 test đạt |
| Teaching | 95 test đạt |
| Finance | 107 test đạt |
| Gateway | 8 test đạt |
| Tổng backend | **503 test đạt**, không lỗi |
| App | **80 test đạt**, TypeScript đạt |
| Web quản trị | **96 test đạt**, build production đạt; vẫn có cảnh báo chunk lớn |
| Android JS/Hermes release export | Đạt; dùng HTTPS Gateway, không phải APK cài đặt |
| Scan bundle release | Không tìm thấy các secret backend được kiểm hoặc nội dung UI thử nghiệm đã loại |
| APK arm64 release | Build thành công; chữ ký hợp lệ, `debuggable=false`, hash khớp manifest; scan APK không tìm thấy secret backend được kiểm hoặc nội dung UI thử nghiệm đã loại, có đúng HTTPS API đã cấu hình |
| UI với backend thật | 5 kiểm tra đạt: login, học phí/lịch sử, deep link giao dịch sau login, cài đặt khi push chưa có, trang chủ |
| Android thật / SecureStore / back / keyboard / native sharing | Chờ thiết bị |
| Push FCM/APNs và app-killed delivery | Chờ cấu hình EAS/Firebase và thiết bị |

**Giới hạn UI harness:** phản hồi API là thật, không mock dữ liệu hoặc ghi tiền. Browser kiểm thử dùng profile cô lập và transport bỏ Origin để tương đương native; Gateway mặc định chỉ cho origin web quản trị localhost:5173. Điều này không chứng minh Expo web localhost:8085 đã được cấu hình CORS hoặc Android thật đã qua nghiệm thu. Nếu muốn dùng Expo web bình thường, cấu hình thêm đúng origin đó; không tắt CORS của Gateway.

**Bằng chứng thanh toán đang có:** quan sát đọc ngày 12/11 thấy đơn sandbox `e9140455-f297-420b-85e5-c6c9b0f9e01e` SUCCEEDED, 1.800.000đ, liên kết phiếu thu 11, không có review flag. Ngrok có IPN lúc 16:31:00, chữ ký hợp lệ, ResponseCode/TransactionStatus đều 00, HTTP200. Đây là giao dịch người dùng đã thực hiện trước đợt kiểm chứng; tác vụ G10 không tạo thêm giao dịch hoặc phiếu thu. Không suy ra các kịch bản hủy/thất bại/đến muộn trên native đã đạt chỉ từ một luồng thành công.

Evidence cục bộ: `storage/student-phase10/summary.json`, các log kiểm thử, `live-ui-report.json`, `payment-evidence.json`, `android-export-result.json`, `artifact-scan.json`, `build-result.json`. Các log PowerShell cũ có thể là UTF-16; trình tổng hợp đọc đúng BOM, không coi cảnh báo stderr là kiểm thử thất bại.

Khi tiếp tục hoàn thiện bàn giao sau lần hết hạn mức công cụ, kiểm tra lại không thấy các cổng 8080–8084 và ngrok 4040 lắng nghe; API hiện chưa kết nối được. Đây là trạng thái môi trường tại lần kiểm tra cuối, không phải kết quả chạy lại các test H2 hoặc phủ nhận bằng chứng UI/API đã đạt trước đó. Bật lại backend và ngrok trước khi dùng APK; không tự khởi động lại toàn bộ các tiến trình mà người dùng có thể đã chủ động đóng.

## APK và cách chạy

APK đã tạo: [LanguageCenter-1.0.0-sandbox-arm64-20261112103722.apk](../../../../language-center-student-app/artifacts/LanguageCenter-1.0.0-sandbox-arm64-20261112103722.apk).

| Thuộc tính | Giá trị đã kiểm |
|---|---|
| Dung lượng | 28.747.961 bytes, khoảng 28,7 MB |
| Package / phiên bản | `com.languagecenter.student` / `1.0.0` |
| Android tối thiểu / target | API 24 / API 36 |
| ABI | `arm64-v8a` |
| Gateway nhúng trong APK | `https://subsystem-handwork-subscript.ngrok-free.dev/api/v1` |
| Chữ ký | Hợp lệ, debug keystore dùng nội bộ; manifest ứng dụng `debuggable=false` |
| SHA-256 | `5484783e87435639ec4af930ef8cda656bdac6f3dae3ab255dbb7e4accc8b2d8` |

`build-result.json`, `apk-verification.json`, `artifact-scan.json` và `language-center-student-app/artifacts/latest-android.json` ghi bằng chứng. Bản này là APK **nội bộ**, không phải bản phát hành store đã có keystore riêng. Chữ ký debug không đồng nghĩa variant app đang bật debug; badging đã kiểm `debuggable=false`.

Cài trên điện thoại Android 64-bit bằng cách chép APK vào máy, mở file và cho phép cài từ nguồn tương ứng. Hoặc khi bật USB debugging, từ thư mục app:

```powershell
adb install -r .\artifacts\LanguageCenter-1.0.0-sandbox-arm64-20261112103722.apk
```

Sau cài, mở **Language Center** và đăng nhập bằng tài khoản STUDENT được cấp. Không cần Metro hoặc Expo Go cho APK này; backend và ngrok vẫn phải chạy.

Build local trên Windows từ thư mục app:

```powershell
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
$env:EXPO_PUBLIC_API_BASE_URL='https://<gateway>/api/v1'
npm run check:release
npm run build:android:local
```

Đầu ra khi thành công là `artifacts/LanguageCenter-<version>-sandbox-arm64-<timestamp>.apk`, có SHA-256 trong manifest. Bản này dùng `arm64-v8a`; thiết bị giả lập x86 không phải mục tiêu bản build này. Backend và tunnel HTTPS phải tiếp tục hoạt động khi sử dụng. Đổi hostname Gateway đã đóng gói cần build lại.

Script khai báo các biến EXPO_PUBLIC là input của tác vụ bundle để lần build sau đổi API không giữ nhầm bundle cũ. Có thể kiểm tra cấu hình/task graph mà không tạo APK mới bằng `npm run build:android:local -- --check-config`.

SDK/NDK/CMake đang yêu cầu: Android platform 37, Build Tools 37.0.0, NDK 27.1.12297006, CMake 3.22.1. Quá trình build đã gặp thiếu bộ nhớ khi chạy song song, thư mục NDK cài dở và lỗi Ninja vượt 260 ký tự. Script giới hạn Gradle một worker và dùng CMake job pool để giới hạn compiler C++ một tác vụ; chỉ giới hạn Gradle worker chưa đủ để hạn chế Ninja. Drive alias tạm rút ngắn đường dẫn, object path dùng ngưỡng 240 để CMake còn đủ chỗ tạo tên/hash. Thư mục NDK cài dở đã được chuyển vào storage, không xóa SDK hoàn chỉnh khác. Xem [CMAKE_OBJECT_PATH_MAX](https://cmake.org/cmake/help/latest/variable/CMAKE_OBJECT_PATH_MAX.html), [CMake compile job pool](https://cmake.org/cmake/help/latest/variable/CMAKE_JOB_POOL_COMPILE.html).

Nếu chọn EAS cloud, đăng nhập/link project và khai báo biến trong EAS environment tương ứng; `.env.local` không được upload. Xem hướng dẫn đầy đủ tại README app và [Expo APK](https://docs.expo.dev/build-reference/apk/), [build profiles](https://docs.expo.dev/build/eas-json/).

## Phần cần nghiệm thu khi có điện thoại

1. Cài APK, tắt Metro, mở app và đăng nhập; kiểm tra sai mật khẩu, khóa/reset từ web và hết phiên.
2. Đóng/mở app để kiểm tra SecureStore; đổi account và mở deep link không được lộ dữ liệu tài khoản trước.
3. Kiểm lịch/time-grid, đổi ngày/lớp, font lớn, bàn phím, nút Back Android, mạng yếu và tải lại.
4. Kiểm điểm công bố/thu hồi, chuyên cần, tải/chia sẻ tài liệu và quyền sau khi thu hồi.
5. Thanh toán sandbox bằng thẻ thử, Return trước IPN, đóng browser, mất mạng, thất bại/hết hạn, phiếu và số dư; không tạo phiếu hoặc đánh dấu đã trả tiền bằng tay.
6. Khi đã có EAS/Firebase, kiểm push foreground/background/app-killed, từ chối quyền và logout/đổi account.

G10 chưa được đánh dấu hoàn thành nghiệm thu thiết bị hoặc phát hành store khi các mục tương ứng còn chờ. Đợt tối ưu thanh toán giai đoạn 5 (web vận hành) là kế hoạch riêng, không tự được coi là đã làm trong G10 mobile.
