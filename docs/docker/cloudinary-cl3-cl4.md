# Cloudinary CL3–CL4 — 25/11/2026

Đã tích hợp ảnh nội dung công khai vào web quản lý và luồng xuất bản website. Academic lưu metadata trong bảng `media_assets`; file ảnh nằm trên Cloudinary. Đây không phải luồng avatar học viên hoặc tài liệu riêng tư.

## Cách sử dụng

1. Mở web quản lý Docker tại http://localhost:15173, đăng nhập.
2. Tại Khóa học/Chi nhánh, chọn **Ảnh và nội dung website**, hoặc mở mục **Nội dung website** trong sidebar.
3. Chọn loại nội dung, **Sửa** bản ghi cần cập nhật; nếu chưa có nội dung, chọn **Thêm nội dung** và chọn bản ghi nghiệp vụ.
4. Ở trường ảnh, chọn **Tải ảnh lên** hoặc **Thư viện ảnh**. JPEG/PNG tối đa 5 MiB, cạnh tối đa 8000px, tổng tối đa 20 triệu pixel.
5. Xem trước và **Lưu**. Nút lưu chờ upload hoàn tất. Nội dung đã xuất bản sẽ dùng ảnh mới ở website; bản nháp cần được Superadmin xuất bản.
6. **Gỡ ảnh** bỏ liên kết ảnh sau khi lưu. Ảnh cũ còn trong lịch sử biên tập vẫn được giữ để không làm hỏng lịch sử.

Superadmin quản lý tất cả loại nội dung và hồ sơ giáo viên. Admin dùng phạm vi chi nhánh được phân công, chỉ sửa bản nháp theo chính sách CMS hiện có. Staff/Teacher/Student không được gọi API marketing upload. Từ danh sách giáo viên, Superadmin có nút **Ảnh và hồ sơ website**.

## Thư viện và xử lý ảnh cũ

- Thư viện lọc theo nhóm ảnh/chi nhánh, phân trang 20 ảnh; có thể chọn lại ảnh mà không upload trùng.
- Ảnh dùng chung trong phạm vi chi nhánh có thể được chọn; quyền xóa vĩnh viễn dành cho người tải phù hợp phạm vi hoặc Superadmin.
- Chỉ xóa vĩnh viễn ảnh không còn trong nội dung và lịch sử. Backend kiểm tra lại và khóa bản ghi khi sử dụng/xóa để tránh race giữa lưu và xóa.
- Đóng form chưa lưu sẽ dọn các ảnh mới do form tạo. Nếu trình duyệt bị tắt đột ngột, có thể dọn ảnh chưa sử dụng qua thư viện ở lần đăng nhập tiếp theo.
- Upload dùng public ID UUID mới, không ghi đè ảnh cũ. CMS dùng URL gốc của ảnh được quản lý; URL biến thể không được dùng để bỏ qua metadata/quyền.
- Gỡ/ẩn nội dung không biến một ảnh public thành private. Không xóa ảnh còn sử dụng trực tiếp ở Cloudinary Console.

Rà dữ liệu trước tích hợp có **0 liên kết ảnh nội dung cũ**, nên không có file cần di chuyển. Logo/ảnh giao diện cố định và các tài nguyên Cloudinary ngoài dự án được giữ nguyên. Bằng chứng: `cloudinary-existing-image-inventory.json`.

## API và metadata

API nằm dưới `/api/v1/academic/media`:

| Method | Path | Chức năng |
|---|---|---|
| GET | `/config` | Khả năng upload, giới hạn, định dạng; không có credentials |
| POST | `/images` | Multipart `file`, `category`, `branchId`; trả metadata có `id` |
| GET | `/images?category=courses&branchId=1&page=0` | Thư viện được lọc scope; kèm `inUse`, `canDelete` |
| DELETE | `/images/{id}` | Xóa Cloudinary và đánh dấu deleted; idempotent, chặn ảnh được tham chiếu |

`media_assets` lưu assetId/publicId, URL, nhóm, người tải, chi nhánh, bytes, kích thước, định dạng và thời gian. Metadata/khóa Cloudinary không được thêm vào public catalog; website chỉ nhận URL từ bản ghi được xuất bản. API secret chỉ có ở Academic.

## Migration và vận hành

- Đã backup và áp dụng `docs/migrations/V029__cloudinary_assets.sql` cho MySQL Docker; **88 bảng**, các bảng dữ liệu cũ không đổi.
- `node scripts/docker/apply-media-migration.cjs` áp dụng idempotent và so sánh dữ liệu cũ. Init Docker mới có `003-media-assets.sql`; công cụ snapshot/migration và kiểm tra runtime đã cập nhật theo schema mới.
- Nếu chạy Maven với database Windows, áp dụng V029 lên database local bằng MySQL Workbench trước khi chạy profile `validate`, rồi restart Academic. Source frontend hỗ trợ cả local và Docker; bản đã triển khai/nghiệm thu là Docker.
- Cập nhật backend có thể đổi IP container. Công cụ update/rollback đã làm mới các caller để tránh cache DNS cũ; đây là lỗi kết nối được phát hiện và sửa trong đợt này.
- Backup DB lưu metadata/URL ảnh, không tự tải toàn bộ bytes ảnh Cloudinary vào volume tài liệu. Restore backup schema cũ cần dùng image phù hợp hoặc áp dụng V029 trước khi chạy Academic mới. Không coi backup SQL là bản sao độc lập của toàn bộ ảnh trên Cloudinary.

## Kiểm chứng CL4

- Build TypeScript/Vite đạt. 48 kiểm thử backend đạt: ảnh giả/rỗng/quá lớn, metadata, ownership, scope, thư viện dùng chung, URL biến thể, xóa idempotent, nguồn đã xóa và bảo toàn lịch sử; kèm hồi quy CMS/intake/security.
- API thật qua Gateway đạt, dùng ảnh tổng hợp rồi xóa qua API để metadata và Cloudinary đồng bộ: `cloudinary-validation.json`.
- Chrome + Cloudinary + Academic/Next thật trên **database sao chép và container tạm**: upload từ file input, lưu khóa, xem website, thay/gỡ, thư viện/chọn lại, hủy form, xóa ảnh chưa dùng, ảnh chi nhánh/giáo viên và viewport website 390px. Ảnh tổng hợp và toàn bộ tài nguyên thử đã dọn.
- Đối chiếu hash CMS chính với backup trước deploy khớp; test không biên tập nội dung CMS chính. Bằng chứng: `cloudinary-cl4-validation.json` và ảnh `screenshots/cl4-*.png`.

19 kiểm tra UI/Cloudinary thực tế, 9 kiểm tra API upload và hồi quy 23 chức năng/phân quyền + 38 HTTP/CORS đạt. Viewport 390px là giả lập trình duyệt, chưa phải thiết bị thật.

Chạy lại khi cần nghiệm thu, không chạy song song với thao tác quản trị/backup:

```powershell
node scripts/docker/verify-cloudinary.cjs
node scripts/docker/verify-cloudinary-ui.cjs
```

Script dùng tài khoản demo/mật khẩu mẫu còn giữ từ DCK5 và Chrome Windows tiêu chuẩn. Có gọi Cloudinary thật bằng ảnh tổng hợp; không dùng ảnh cá nhân. Chưa kiểm thử trên điện thoại thật hoặc tích hợp avatar/tài liệu riêng tư.
