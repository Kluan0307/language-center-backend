# Ảnh minh họa và tài liệu học tập mẫu — 25/11/2026

Đã cập nhật trực tiếp qua Gateway/API vào **database Docker**. Database MySQL chạy riêng trên Windows không thay đổi. Không cần rebuild website để thấy dữ liệu mới.

## Nội dung đã thêm

| Nội dung | Kết quả |
|---|---|
| Ảnh nguồn | 4 ảnh học tập miễn phí, đã xem và kiểm tra giấy phép |
| Thư viện Cloudinary | 8 asset theo hai nhóm `courses` và `banners`, có thể tái sử dụng |
| Nội dung website | 4 chương trình và 20 khóa học đã có ảnh |
| Tài liệu | 7 bộ PDF tự biên soạn, mỗi bộ 2 trang, gồm bài tập và đáp án |
| Phạm vi chia sẻ | 28 bản ghi tài liệu: 7 bộ/chi nhánh × 4 chi nhánh |

Giữ nguyên tên, học phí, thời lượng, lớp, lịch, tài khoản, trạng thái công bố và các thông tin nghiệp vụ hiện có. Thêm câu “Ảnh minh họa học tập.” vào phần giới thiệu có ảnh để phân biệt với ảnh thực tế. Ảnh giáo viên và chi nhánh vẫn chờ ảnh thật.

## Tài liệu và khóa học

Tài liệu được gắn **khóa học + chi nhánh**, không chia sẻ toàn hệ thống. Học viên phải có đăng ký phù hợp mới xem/tải được. Các lớp tương ứng trong cùng chi nhánh dùng chung tài liệu khóa học đó.

| Khóa học | Bộ bài tập | Nội dung |
|---|---|---|
| MG1 | Colours and numbers | Nghe, chỉ màu, đếm và vẽ cùng người lớn |
| TH1 | My school bag | Từ vựng đồ dùng học tập, đọc và viết câu ngắn |
| CS1 | A new classmate | A1: giới thiệu, lịch sinh hoạt, hiện tại đơn |
| GT1 | Making a study plan | Đề xuất, đổi và xác nhận lịch hẹn |
| IL2 | Describing a small data table | IELTS 5.5: mô tả xu hướng bằng số liệu giả định |
| TC2 | An office training email | TOEIC 450: đọc email công việc và viết phản hồi |
| PT1 | Choosing how to study | B1: so sánh, giải thích lựa chọn cách học |

Nội dung PDF bằng tiếng Anh, tên/giới thiệu trên app bằng tiếng Việt. Có hướng dẫn, thời lượng, bài tập và trang đáp án tự kiểm tra. Các tài liệu này **không phải đề IELTS/TOEIC chính thức, không dự đoán điểm**, không sao chép giáo trình thương mại. Đã đi qua quy trình tải lên → duyệt → chia sẻ; có nhật ký và sự kiện thông báo tài liệu mới. Không tự bật push hoặc dịch vụ bên ngoài.

Tài liệu tham khảo thêm qua liên kết chính thức, không tải và phát hành lại nội dung của các đơn vị này:

- [British Council — A1 speaking](https://learnenglish.britishcouncil.org/free-resources/speaking/a1)
- [IELTS — Preparation resources](https://ielts.org/take-a-test/preparation-resources)
- [ETS — TOEIC preparation](https://www.ets.org/toeic/test-takers/prepare.html)

## Nguồn ảnh

Các ảnh dưới đây được ghi nhận là ảnh miễn phí theo [Unsplash License](https://unsplash.com/license) tại thời điểm tải. Nguồn, tác giả, URL tải và SHA-256 được lưu để đối chiếu. Không dùng ảnh người thật làm hồ sơ nhân sự hoặc ảnh cơ sở khác làm chi nhánh trung tâm.

| Ảnh | Tác giả | Sử dụng |
|---|---|---|
| [Bút chì màu](https://unsplash.com/photos/a-group-of-colored-pencils-sitting-on-top-of-each-other-AWxDuxOIOkM) | Josephina Kolpachnikof | Mẫu giáo, tiểu học |
| [Sổ và bút trên bàn](https://unsplash.com/photos/open-notebook-with-pen-and-pencils-on-desk-n9AaeihA9HI) | Clay Banks | Giao tiếp, IELTS |
| [Sổ học tập](https://unsplash.com/photos/an-open-notebook-and-pens-on-a-desk-hBdaqrr5Z3k) | Kelly Sikkema | Tiếng Anh tổng quát, A1/B1 |
| [Sổ kế hoạch](https://unsplash.com/photos/a-notebook-with-a-pen-on-top-of-it-v30bSAWzp4I) | My Profit Tutor | TOEIC, tiếng Anh công việc |

## Xem kết quả

- Website: http://localhost:13000/khoa-hoc/mg1 và các trang khóa học/chương trình.
- Web quản lý: http://localhost:15173 → Nội dung website để thay ảnh; Tài liệu để quản lý PDF theo chi nhánh/khóa học.
- App học viên: đăng nhập học viên có đăng ký khóa học tương ứng → Tài liệu. Tải lại danh sách nếu đang mở app từ trước.

## Tệp bàn giao và kiểm chứng

- [Kết quả kiểm chứng](sample-learning-media.json).
- [Ảnh màn hình desktop](screenshots/learning-media-1440.png), [mobile](screenshots/learning-media-390.png).
- `storage/sample-learning-media/sources/`: bốn JPEG gốc.
- `storage/sample-learning-media/documents/`: bảy PDF gốc, có thể tái tạo từ script.
- `storage/sample-learning-media/sources.json`: nguồn, giấy phép, checksum.
- `storage/sample-learning-media/import.json`: tiến độ, ID asset/tài liệu và nội dung CMS trước/sau. Giữ tệp này để chạy lại không tạo trùng.

Các tệp trong `storage` thuộc dữ liệu local, không commit lên Git. Nội dung PDF gốc và quy tắc nhập nằm trong `scripts/docker/seed-learning-media.cjs`. Script này dành riêng cho bộ dữ liệu mẫu hiện tại (course ID 17–36, branch ID 1–4), không phải seed chung cho database bất kỳ.

```powershell
# Tái tạo PDF local, không gọi API hoặc thay dữ liệu
node scripts/docker/seed-learning-media.cjs --prepare-only

# Kiểm tra catalog, tải PDF với học viên, chặn chi nhánh khác, Chrome desktop/mobile
node scripts/docker/verify-learning-media.cjs
```

Đã kiểm tra 28 lượt xem/tải đúng khóa học và chi nhánh, 28 lượt tải tài liệu khác chi nhánh bị từ chối 404, tải khi chưa đăng nhập bị từ chối 401. Tất cả PDF và ảnh tải về khớp SHA-256 gốc. Ảnh hiển thị trong Chrome ở 1440px và 390px. Đây là kiểm chứng website và API học viên; không thay thế thử thao tác tải PDF trên thiết bị Android thật.

Bản sao lưu trước khi nhập: `ops-1795598691028-e5a7c7`, gồm database và volume tài liệu. **Khôi phục toàn bộ backup sẽ mất các thay đổi phát sinh sau thời điểm backup**; chỉ thực hiện khi đã chủ động chọn khôi phục toàn bộ. Backup local không chứa ảnh gốc trên Cloudinary; giữ thư mục ảnh nguồn nói trên. Không xóa asset Cloudinary khi vẫn được nội dung/lịch sử tham chiếu.
