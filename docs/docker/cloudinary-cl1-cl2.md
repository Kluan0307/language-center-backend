# Cloudinary CL1–CL2

**CL3–CL4 đã bổ sung metadata/thư viện/xóa ảnh và giao diện upload.** Xem [bàn giao mới](cloudinary-cl3-cl4.md). Các giới hạn của bản CL2 bên dưới là lịch sử; API upload hiện trả thêm `id` metadata.

Academic cung cấp upload ảnh marketing công khai, JPEG/PNG, tối đa 5 MiB, cạnh tối đa 8000px và tổng tối đa 20 triệu pixel. Kiểm tra header ảnh thật, không tin extension/MIME do client gửi. Không nhận SVG, GIF, PDF hoặc ảnh riêng tư qua API này.

## Cấu hình

Backend `.env.local` và workspace `.env.docker.local`:

```dotenv
CLOUDINARY_ENABLED=true
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
CLOUDINARY_FOLDER=language-center/dev
```

API secret chỉ tới Academic, không truyền cho gateway, website, frontend hoặc mobile. Dockerfile không copy env vào image. Chạy `node scripts/docker/configure-cloudinary.cjs` để bổ sung key còn thiếu từ env backend vào Docker; giữ nguyên giá trị Docker đã có. Restart/recreate Academic sau khi đổi env.

## API

`GET /api/v1/academic/media/config`: trả trạng thái sẵn sàng, giới hạn và loại ảnh; không trả key/secret.

`POST /api/v1/academic/media/images`: multipart form có `file`, `category` (`courses`, `branches`, `teachers`, `banners`), `branchId`. Chỉ SUPER_ADMIN/ADMIN được gọi; ADMIN bị giới hạn chi nhánh được phân công. Lỗi 401 khi chưa đăng nhập, 403 khi sai role, 404 khi sai phạm vi chi nhánh, 400 khi ảnh không hợp lệ, 413 quá dung lượng, 503 khi chưa cấu hình hoặc provider lỗi.

Header `Authorization: Bearer <token>`. Qua Docker Gateway dùng `http://localhost:18080`; qua web quản lý proxy cùng origin dùng `/api/v1/...`.

Kết quả gồm `assetId`, `publicId`, `url`, `format`, `bytes`, `width`, `height`, `category`, `branchId`, `uploadedBy`. Dynamic folders dùng `asset_folder`, public ID do backend sinh UUID, không ghi đè ảnh cũ. Upload diễn ra qua HTTPS server-side Basic Auth theo Upload API của Cloudinary. Có timeout kết nối/đọc, không tự retry upload để tránh tạo file trùng khi chưa biết kết quả.

CL2 hiện trả metadata; chưa lưu liên kết vào khóa học/hồ sơ hoặc xây API xóa tùy ý. Việc gắn metadata vào bản ghi, xử lý ảnh cũ/orphan, avatar, tích hợp form upload và cập nhật hiển thị thuộc CL3. Không dùng API public này cho chứng chỉ/tài liệu riêng tư.

## Kiểm chứng

`CloudinaryImageTest` kiểm tra PNG thật, file giả và file rỗng. `node scripts/docker/verify-cloudinary.cjs` kiểm thử quyền, chi nhánh, upload một PNG tổng hợp qua Gateway, tải ảnh rồi xóa đúng asset thử bằng Cloudinary API. Không dùng ảnh cá nhân để kiểm thử. Báo cáo thành công tại `cloudinary-validation.json` khi chạy xong; upload thật tiêu thụ quota Cloudinary.

## Trạng thái 25/11/2026

Đã triển khai API vào Docker Academic. Anonymous bị chặn 401, staff 403, admin đọc config được; file giả bị chặn 400 và chi nhánh ngoài phạm vi bị chặn 404. Upload thật đang bị Cloudinary từ chối 403 do key thiếu permission `create`; ứng dụng trả 503 thay vì lộ thông tin provider/credential. Chưa đánh dấu upload Cloudinary đã nghiệm thu thành công.

Vào Settings → API Keys, kiểm tra role và folder permissions của key trong đúng product environment. Nếu dùng Media Library User, cấp quyền upload cho folder `language-center/dev` cùng các folder con. Nếu đổi key, cập nhật **cả** backend `.env.local` và workspace `.env.docker.local` vì script configure giữ nguyên giá trị Docker đã có. Chạy `lc.ps1 -Action up` để recreate Academic theo env mới, rồi chạy lại verify-cloudinary. Tham khảo: https://cloudinary.com/documentation/developer_onboarding_faq_find_credentials
