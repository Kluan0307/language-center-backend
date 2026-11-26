# DCK4 — React/Nginx và Next.js trong Compose

Hoàn thành ngày **24/11/2026** cho môi trường local. Stack chính có **9 container healthy**, gồm MySQL, sáu backend và hai web. Kết quả: [38 kiểm tra cấu hình/HTTP](dck4-validation.json), [18 kiểm tra Chrome](dck4-browser-validation.json). Database Docker vẫn có 87 bảng rỗng; chưa nhập dữ liệu từ MySQL Windows.

## Mở ứng dụng

| Thành phần | Địa chỉ trên máy chạy Docker |
|---|---|
| Web quản lý | http://localhost:15173 |
| Website công khai | http://localhost:13000 |
| Gateway | http://localhost:18080 |

**Chưa đăng nhập được bằng tài khoản của database local cũ** vì Docker dùng volume MySQL riêng. Danh sách khóa học/chi nhánh trên website Docker đang trống là đúng trạng thái dữ liệu. DCK5 sẽ backup, thử phục hồi dữ liệu và kiểm chứng đăng nhập, phân quyền, tư vấn và nghiệp vụ. Không tạo lại dữ liệu mẫu chỉ để làm giao diện có nội dung.

## Chạy từ backend root

Yêu cầu Docker Desktop chạy Linux containers và đủ ba thư mục repo cùng cấp trong `language-center/`.

```powershell
# Lần đầu: sinh secret riêng cho Docker, không ghi đè file đã có.
node scripts/docker/init-env.cjs
# Đồng bộ wrapper ngoài cùng và thêm cấu hình web/CORS còn thiếu.
node scripts/docker/configure-web.cjs

# Lần đầu hoặc sau khi sửa code: build và chạy.
.\scripts\docker\compose.ps1 -Action up -Web -Assistant -Build

# Các lần sau dùng image đã build.
.\scripts\docker\compose.ps1 -Action up -Web -Assistant
.\scripts\docker\compose.ps1 -Action ps
.\scripts\docker\compose.ps1 -Action logs
.\scripts\docker\compose.ps1 -Action down
```

`down` giữ nguyên volume MySQL và tài liệu. Bỏ `-Assistant` khi không dùng Assistant. Nếu chỉ cần backend, bỏ cả `-Web`; thao tác `ps/logs/stop/down` vẫn nhận diện đầy đủ stack. AI, VNPAY, push và job mặc định tắt trong env Docker, bật riêng sau khi cấu hình tích hợp.

Từ thư mục ngoài cùng `language-center/`, cách tương đương:

```powershell
docker compose --profile assistant up -d --build --wait
docker compose --profile assistant ps
docker compose --profile assistant down
```

## Cấu hình và đường đi API

- Backend canonical: `compose.yaml`; web bổ sung: `compose.web.yaml`. Wrapper ngoài cùng include hai file cùng một model. Không chạy fragment web riêng lẻ.
- React được build bằng Node 22, phục vụ qua Nginx không dùng root. `VITE_API_BASE_URL=/api/v1` được biên dịch lúc build. Browser gọi cùng origin; Nginx chuyển `/api/` tới `api-gateway:8080`, giữ token và header chi nhánh. Deep link được trả SPA; lỗi API và asset thiếu không bị đổi thành HTML ứng dụng.
- Next sử dụng Dockerfile standalone sẵn có, bổ sung npm cache và `.dockerignore`. SSR/BFF gọi `WEBSITE_API_URL=http://api-gateway:8080/api/v1/academic/public/catalog/`. Browser không nhận tên DNS Docker.
- `WEBSITE_FORM_SECRET` chỉ truyền lúc chạy Next và Academic, cùng giá trị từ `.env.docker.local`. Không dùng build arg hoặc `NEXT_PUBLIC_` cho secret. Cả hai build context loại `.env*`, dependency, cache và log local.
- Hai web chạy nonroot, filesystem chỉ đọc, tmpfs cho thư mục ghi tạm; Nginx có cache asset hash, HTML/API không cache; readiness Next kiểm tra backend và cấu hình form.
- Các cổng mặc định chỉ bind `127.0.0.1`, không đụng gateway 8080, Vite 5173, Next 3000 và MySQL 3306 đang chạy local.
- Đổi `ADMIN_HOST_PORT`, `WEBSITE_HOST_PORT`, `WEBSITE_PUBLIC_URL`, contact hoặc index flag trong `.env.docker.local`, chạy lại `configure-web.cjs`, rồi build/up. Public URL/contact/index metadata của Next là cấu hình build; restart đơn thuần không cập nhật hết metadata. Script giữ nguyên secret và origin tùy chỉnh đã có.

## Kết quả kiểm chứng và sửa build

Hai image đã build thật bằng `npm ci` trong Linux. Next lockfile được sửa vị trí dependency `picomatch` (4.0.7 ở root cho tinyglobby, 2.3.2 lồng dưới micromatch), không nâng phiên bản trực tiếp trong package.json. React admin hiện dùng thư viện `react-captcha-code@1.0.7` với peer React 16; Docker build dùng `npm ci --legacy-peer-deps` để phù hợp cây dependency hiện tại. Thay thư viện captcha là công việc bảo trì riêng, chưa thay đổi trong DCK4.

Đã kiểm tra 9 healthcheck, wrapper, proxy Auth/catalog, SPA deep link, asset 404/cache, CORS cho hai web và từ chối origin lạ; Next SSR/BFF, key form hợp lệ tới backend (payload thiếu bị trả 400), chặn origin/body không hợp lệ, secret không có trong browser asset/image env, nonroot/read-only và bảng nghiệp vụ vẫn rỗng.

Chrome headless kiểm tra hai web, website tại 1440px/390px, tải font và chụp [ảnh](screenshots/); không có exception JavaScript hoặc browser request tới tên service Docker/cổng gateway local cũ. Ảnh giao diện: [admin](screenshots/dck4-admin-login.png), [website desktop](screenshots/dck4-site-home-1440.png), [website mobile](screenshots/dck4-site-home-390.png).

Chạy lại kiểm chứng trên stack đang bật đầy đủ với port mặc định:

```powershell
node scripts/docker/verify-web.cjs
node scripts/docker/verify-web-browser.cjs
node scripts/docker/verify-main.cjs
```

Browser script cần Chrome cài ở đường dẫn Windows tiêu chuẩn. Các kiểm tra không chèn dữ liệu; chưa khẳng định các luồng có tài khoản/nội dung là đạt khi database còn rỗng.

## App Student

Expo/APK tiếp tục chạy ngoài Docker. Android emulator dùng `http://10.0.2.2:18080/api/v1`. Điện thoại thật cần IP LAN của máy, không dùng `localhost` hoặc tên `api-gateway`.

Để thử điện thoại qua LAN: đổi `GATEWAY_BIND_ADDRESS=0.0.0.0` trong env Docker, up lại stack; cho phép TCP 18080 trên mạng riêng của Windows Firewall; đặt `EXPO_PUBLIC_API_BASE_URL=http://IP-LAN-CUA-MAY:18080/api/v1` trong môi trường mobile rồi restart Metro hoặc build lại APK. Máy và điện thoại cùng mạng. Việc đổi URL không cần container hóa mobile. Chưa tự mở LAN/firewall hoặc kiểm thử trên thiết bị thật.

Ngrok dùng `ngrok http 18080` khi muốn trỏ tới Gateway Docker. URL VNPAY Return/IPN phải trỏ tới đúng môi trường có dữ liệu thanh toán; cấu hình host local không tự áp dụng sang Docker. DCK5 sẽ kiểm chứng tích hợp sau phục hồi dữ liệu.
