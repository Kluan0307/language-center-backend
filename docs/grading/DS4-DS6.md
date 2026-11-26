# Điểm số — ĐS4, ĐS5, ĐS6

ĐS7–ĐS8 bổ sung lịch sử chi tiết, CSV, đối chiếu điểm cũ và liên kết hoàn thành học vụ. Xem [hướng dẫn hiện hành](DS7-DS8.md); các giới hạn chuyển đổi/chốt kết quả mô tả bên dưới là trạng thái tại ĐS6.

Ngày cập nhật: 25/11/2026. Đã triển khai teaching-service và web quản lý trên Docker local. App học viên đã bổ sung giao diện đọc công bố mới; cần chạy lại Expo hoặc build/cài bản app mới để dùng code mới.

## ĐS4 — Tính điểm, thi lại và điều kiện hoàn thành

Điểm nội bộ được tính theo hai bước: trung bình các bài thuộc từng thành phần, chuẩn hóa theo thang của thành phần, sau đó tính trung bình có trọng số. So ngưỡng đạt bằng giá trị chưa làm tròn; hiển thị tối đa hai chữ số thập phân.

Ví dụ, HOMEWORK 10%, QUIZ 20%, MIDTERM 30%, FINAL 40%, tất cả thang 10: điểm các thành phần 8, 7, 6, 9 cho kết quả 76%. Hai bài tập 6 và 10 cho trung bình thành phần bài tập 8; không cộng thêm trọng số chỉ vì có nhiều bài tập.

- Chưa nhập, Vắng và Chưa nộp không tự thành 0. Thiếu kết quả bắt buộc trả “Chưa đủ dữ liệu”, không tự kết luận đạt/rớt.
- Được miễn phải có ghi chú; bài miễn được bỏ khỏi mẫu số. Thành phần hoàn toàn được miễn có trọng số bằng 0 khi tính thực tế; các thành phần còn lại được chuẩn hóa lại. Toàn bộ được miễn không tạo điểm tổng 100%.
- Bài thi lại tham chiếu bài gốc đã chốt danh sách, có cùng thành phần và ngày không trước bài gốc. Có thể chọn một nhóm học viên trong danh sách gốc. Không sửa/xóa điểm bài gốc.
- Trong mỗi chuỗi thi lại, chọn lần có điểm hoặc được miễn gần nhất. Vắng thi lại không xóa điểm hợp lệ trước đó. Mỗi chuỗi chỉ được tính một lần vào thành phần.
- Nhận xét trẻ nhỏ dùng mức đánh giá và nội dung; không tạo điểm phần trăm giả. Chưa có nhận xét thì không tự đánh giá “Đạt yêu cầu”.

Kết quả học thuật, chuyên cần và thời lượng được thể hiện riêng. Đủ điểm học thuật nhưng chưa đủ buổi/thời lượng vẫn là “Đang học”; thiếu dữ liệu điểm danh là “Chưa đủ dữ liệu”; không đạt tỷ lệ tham dự là “Chưa đạt chuyên cần”. Vắng có phép không được tính thành có mặt. Cách tính chuyên cần dùng cùng bộ quy tắc hiện có của hệ thống.

Đây là kết quả đánh giá theo quy chế. Công bố kết quả không tự đổi `study_status` của đăng ký học hoặc phát hành chứng chỉ. Luồng chuyển đổi/đối chiếu với kết quả chính thức của dữ liệu cũ thuộc đợt chuyển tiếp sau.

## ĐS5 — IELTS và TOEIC

### IELTS thi thử

- Bốn kỹ năng Listening, Reading, Writing, Speaking, thang 0–9, bước nhập 0,5.
- Overall là trung bình bốn kỹ năng, làm tròn đến band 0,5; trung bình 5,75 thành 6,0, trung bình 6,25 thành 6,5.
- Chỉ dùng bốn kỹ năng của cùng một lần thi thử đầy đủ. Không lấy Listening/Reading từ một bài và Writing/Speaking từ bài khác để tạo overall.
- Dùng bài thi thử đầy đủ gần nhất đã đến ngày đánh giá. Bài tương lai không làm mất kết quả bài đã hoàn thành.
- Với chuỗi thi lại, ưu tiên lần đầy đủ có đủ bốn điểm hợp lệ gần nhất. Nếu không có lần đầy đủ, hiển thị thiếu dữ liệu; không ghép từng kỹ năng giữa các lần thi.
- Mẫu quy chế có thể đặt mục tiêu overall và mức kỹ năng tối thiểu. Không có mục tiêu thì chỉ “Đã ghi nhận”, không tự suy ra đạt/rớt.

Quy tắc band được đối chiếu với [IELTS — Scoring in detail](https://ielts.org/take-a-test/your-results/ielts-scoring-in-detail). Đây là thi thử tại trung tâm, không phải kết quả/chứng chỉ chính thức. Chưa triển khai quy đổi câu đúng IELTS hoặc chấm chi tiết rubric Writing/Speaking tự động.

### TOEIC Listening & Reading thi thử

- Mỗi kỹ năng 5–495, bước 5; tổng hai kỹ năng 10–990. [ETS — TOEIC L&R FAQ](https://www.ets.org/toeic/about/faq/product-specific-faq/toeic-listening-reading.html).
- Có thể nhập điểm đã quy đổi trực tiếp, hoặc chọn bảng quy đổi theo đề khi tạo bài và nhập số câu đúng 0–100.
- Bảng quy đổi phải có đủ 101 giá trị cho mỗi kỹ năng, tăng không giảm, điểm nằm trong 5–495 và chia hết cho 5. Lưu mã, phiên bản, tên đề và nguồn tham khảo.
- Bảng là bất biến theo phiên bản. Bài đã tạo giữ ID bảng cũ; tạo phiên bản mới không tính lại bài cũ.
- Lưu đồng thời số câu đúng, điểm đã quy đổi và ID bảng. Nếu client gửi điểm khác với bảng, backend từ chối.
- Không có bảng phù hợp thì nhập điểm đã quy đổi từ nguồn xác định; không dùng phép nhân tuyến tính hoặc tự nhận một bảng chung là chính thức.

Database chính không được chèn bảng quy đổi giả. Bảng tổng hợp trong kiểm thử chỉ nằm trên schema riêng và đã được xóa khi kết thúc.

## ĐS6 — Gửi duyệt, công bố và sửa điểm

Luồng: **Bản nháp → Gửi duyệt → Đã duyệt → Đã công bố**.

| Thao tác | Quyền và điều kiện |
|---|---|
| Gửi duyệt | Giáo viên đúng lớp hoặc quản lý có quyền; có kết quả, không để trống các ô của bài đã đến ngày đánh giá. Có thể dùng trạng thái Vắng/Miễn/Chưa nộp để giải thích. |
| Duyệt | Admin/Superadmin đúng phạm vi; không được tự duyệt bản mình gửi. |
| Trả lại | Quản lý trả bản gửi/đã duyệt về trạng thái cho phép sửa, có lý do. |
| Công bố | Chỉ bản đã duyệt, nguồn điểm và học vụ vẫn khớp phiên bản được duyệt. |
| Thu hồi | Quản lý thu hồi công bố; học viên không còn đọc bản hiện hành. |
| Mở lại | Quản lý mở bản gửi/đã duyệt/đã công bố/đã thu hồi để sửa; ẩn công bố hiện hành và yêu cầu gửi duyệt lại. |

Khi gửi duyệt hoặc công bố, không được sửa nguồn điểm/tạo bài mới cho đến khi trả lại hoặc mở lại. Mọi thao tác có lý do, người thực hiện, thời điểm, phiên bản và UUID yêu cầu. Gửi lại đúng UUID không tạo thêm công bố; dùng UUID cho ý định khác bị từ chối.

Bản gửi duyệt được chụp bất biến. Fingerprint bao gồm quy chế lớp, bài/điểm, danh sách dự đánh giá, dữ liệu học vụ liên quan, phiên bản nguồn và kết quả tính. Nguồn thay đổi sau gửi/duyệt thì phải kiểm tra và duyệt lại. Điểm học viên đọc là bản đã công bố, không phải kết quả nháp tính lại trên màn hình quản lý.

Nhận xét trong kết quả đánh giá được công bố cho chính học viên cùng điểm/mức đánh giá. Không đặt ghi chú nội bộ nhạy cảm vào trường nhận xét của kết quả gửi duyệt.

Học viên gọi API theo đăng ký của mình; backend xác minh quyền sở hữu qua Academic, rồi lọc đồng thời enrollmentId/studentId. Không trả toàn bộ snapshot lớp. Các trạng thái nháp/chờ duyệt/đã duyệt/thu hồi/mở lại đều không trả kết quả riêng tư. Công bố/thu hồi tạo sự kiện thông báo vào outbox; không tự bật push hoặc cấu hình nhà cung cấp.

## Cách sử dụng

1. Web quản lý → **Bảng điểm → chọn lớp → Đánh giá theo quy chế**.
2. Nhập đủ kết quả, dùng “Tính lại” và “Xem cách tính” để kiểm tra từng thành phần, thi lại, chuyên cần và điều kiện hoàn thành.
3. Giáo viên bấm **Gửi duyệt**, nhập lý do. Quản lý khác kiểm tra và bấm **Duyệt kết quả**.
4. Quản lý bấm **Công bố cho học viên**. Chưa bấm công bố thì học viên chưa thấy kết quả mới.
5. Muốn sửa sau công bố, dùng **Mở lại để chỉnh sửa**, sau đó gửi duyệt lại; dùng **Thu hồi công bố** nếu chỉ muốn ẩn.
6. Với TOEIC, Superadmin có thể mở **Bảng quy đổi TOEIC**, dán CSV 101 dòng theo thứ tự `số_câu_đúng,điểm_Listening,điểm_Reading`, không có tiêu đề, kèm mã/tên/nguồn. Chọn đúng bảng khi tạo bài.
7. Trên app học viên → điểm của lớp: xem kết quả đã công bố, từng thành phần, nhận xét, cách tính và lưu ý thi thử. Kéo tải lại sẽ kiểm tra công bố mới nhất. Cần code app mới; APK cũ không tự nhận phần giao diện này.

Lớp có bài/điểm cũ vẫn dùng luồng cũ; không tự gắn quy chế mới hoặc chuyển điểm cũ thành từng kỹ năng. Bốn lớp chưa có bài kiểm tra cũ trong dữ liệu mẫu có thể được quản lý chủ động áp dụng quy chế theo hướng dẫn ĐS1–ĐS3.

## API bổ sung

Base `/api/v1/teaching/grading`, cần đăng nhập và kiểm tra role/lớp/chi nhánh.

| Method | Đường dẫn | Chức năng |
|---|---|---|
| GET | `/classes/{id}/preview` | Kết quả tính, cách tính, fingerprint và trạng thái duyệt |
| POST | `/classes/{id}/workflow/{action}` | SUBMIT, APPROVE, REJECT, PUBLISH, RETRACT, REOPEN |
| GET | `/classes/{id}/workflow/history` | Lịch sử phiên bản duyệt/công bố |
| GET | `/conversions` | Bảng quy đổi TOEIC đã lưu |
| POST | `/conversions` | Superadmin tạo phiên bản bảng quy đổi |

Body thao tác workflow: `requestId` UUID, `expectedRevision`, `fingerprint` từ preview và `reason` 3–500 ký tự. Bài đánh giá nhận thêm `retakeOf`, `studentIds` cho thi lại và `conversionId` cho TOEIC. Dòng điểm nhận thêm `rawScore` nếu dùng bảng quy đổi.

API học viên: `GET /api/v1/teaching/student/me/enrollments/{id}/grading-result`. Chỉ STUDENT, chỉ đăng ký của mình. API điểm cũ giữ nguyên hợp đồng.

## Dữ liệu, backup và kiểm chứng

- Migration V031 thêm hai bảng `grading_conversions`, `grading_releases` và bốn cột nullable cho bài/điểm mới. Tổng schema Docker 97 bảng; không sửa điểm cũ.
- Fresh Docker: `deployment/docker/mysql/005-grading-outcomes.sql`. Database Windows riêng chưa được tự áp dụng migration.
- Backup trước migration: `ops-1795612730397-404829`; trước cập nhật image: `ops-1795612904402-768c7d`.
- [Báo cáo nghiệm thu tích hợp](ds456-live.json), [báo cáo hoàn thành](DS4-DS6-completion.json), [desktop](screenshots/ds456-1440.png), [mobile web](screenshots/ds456-390.png).
- 154 kiểm thử teaching-service, 108 kiểm thử mobile hiện có; web build và mobile TypeScript đạt. 17 bước MySQL/API/Chrome đạt, gồm nhập điểm, gửi duyệt và công bố bằng giao diện thật.
- Kiểm thử dùng schema/container riêng và nguồn Academic tổng hợp. Đã đối chiếu xác nhận không ghi dữ liệu điểm thử vào database chính và đã dọn tài nguyên tạm.
- Chưa thử trực tiếp thiết bị Android hoặc build APK mới. Chưa tự đổi trạng thái đăng ký học hoặc cấp chứng chỉ từ công bố mới.

Tiếp theo ĐS7: hoàn thiện trải nghiệm quản lý/học viên, lịch sử thi lại chi tiết và xuất bảng điểm. ĐS8: đối chiếu/chuyển dữ liệu cũ và kết nối quy trình kết thúc khóa có kiểm soát.
