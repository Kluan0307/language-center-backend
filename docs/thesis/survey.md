# Khảo sát nghiệp vụ — chuẩn bị G01

Ngày: **07/11/2026**. **Chưa có dữ liệu khảo sát thực địa/phỏng vấn được cung cấp trong repository hoặc cuộc trao đổi để chứng minh các chính sách mới.** Văn bản này là bảng câu hỏi và mẫu thu thập, không phải kết quả khảo sát.

## 1. Nguồn thông tin và trạng thái

| Nguồn | Có gì | Không được suy ra |
|---|---|---|
| Đề cương người dùng cung cấp | Mục tiêu, dữ liệu, nghiệp vụ cơ bản và chuẩn đầu ra | Không chứng minh một trung tâm cụ thể dùng quy tắc 50%, quy đổi thang hoặc chính sách giờ |
| Source BE/FE và migration | Quy tắc phần mềm hiện tại | Không chứng minh đây là quy trình thực tế đã được khảo sát |
| Quyết định G01-v1 | Phương án tối thiểu dùng để cài đặt, ghi rõ giả định | Không gọi là quy định của trung tâm hoặc xác nhận của người dùng |
| Phỏng vấn/tài liệu/quan sát trung tâm | [CẦN BỔ SUNG: nguồn và sự đồng ý sử dụng] | Không tự tạo tên người, ngày, câu trả lời hoặc số liệu |

## 2. Người cần trao đổi

Quản lý trung tâm/chi nhánh; nhân viên tuyển sinh/học vụ; giáo viên; nhân viên thu phí nếu vai trò thực tế tách riêng. Chỉ thu thập thông tin cần thiết, ẩn danh dữ liệu người học/nhân sự trong báo cáo nếu có mẫu thật. Không đưa mật khẩu, tài khoản thật hoặc giấy tờ cá nhân vào repository.

## 3. Bảng câu hỏi

| Mã | Nội dung hỏi / chứng từ nên xem | Quyết định bị ảnh hưởng |
|---|---|---|
| Q01 | Trung tâm quản lý một hay nhiều chi nhánh? Nhân sự làm việc ở nhiều nơi thế nào? | BR-01/03, quyền dùng chung hồ sơ |
| Q02 | Ai được sửa chuyên môn/liên hệ giáo viên? Có cần lịch sử hoặc duyệt hai cấp? | BR-02; G02 |
| Q03 | Khi giáo viên nghỉ/khóa tài khoản khẩn cấp, ai bàn giao lớp/buổi và xử lý buổi quá ngày chưa điểm danh? | BR-03; G03 |
| Q04 | Có biểu mẫu tiếp nhận học viên, người giám hộ và cách xử lý số điện thoại gia đình dùng chung không? | RQ01, validation |
| Q05 | Có những chương trình/thang trình độ nào? Đầu vào và mục tiêu đầu ra được ghi ở đâu? | BR-04; G04 |
| Q06 | Đánh giá đầu vào qua test, phỏng vấn hay chứng chỉ? Ai đánh giá và ai nhập? Có thời hạn sử dụng không? | BR-05; G05 |
| Q07 | Khóa cho người mới có cần test không? Chấp nhận các mức đầu vào nào, có quy đổi thang được phê duyệt không? | OPEN_ENTRY/required levels |
| Q08 | Ai được cho học viên học khác mức? Có cần hai người duyệt hoặc hạn sử dụng xác nhận không? | BR-07; G07 |
| Q09 | Bảo lưu/học lại/chuyển lớp có đánh giá lại không? Chuyển cùng khóa có giữ giá/phiếu thu không? | BR-06/11 và quy tắc hiện có |
| Q10 | “Thời lượng khóa 24” nghĩa là buổi/giờ/tuần? Có dữ liệu gốc chứng minh từng khóa cũ không? | BR-08, migration legacy |
| Q11 | Xác nhận một buổi đã dạy bằng điểm danh hay biên bản khác? Buổi học ngắn hơn lịch được ghi thế nào? | BR-08; G09 |
| Q12 | Nghỉ/bù/dạy thay, nhiều ca cùng ngày có thường gặp không? Quy trình xác nhận lịch là gì? | Phạm vi lịch hiện tại; đánh dấu nhu cầu mở rộng nếu có |
| Q13 | Mỗi chương trình có bài bắt buộc, thang điểm, trọng số và ngưỡng đạt nào? Có mẫu bảng điểm không? | BR-09; G10 |
| Q14 | Có xét chuyên cần không? Đi muộn/nghỉ có phép được tính thế nào? Bài vắng thi có thi lại không? | BR-09; không tự thêm thi lại nếu ngoài phạm vi |
| Q15 | Ai chốt/mở lại kết quả? Học xong nhưng không đạt ghi trạng thái nào? Có liên quan công nợ không? | BR-10; G11 |
| Q16 | Học phí theo khóa hay theo lớp? Thu nhiều đợt, giảm phí, nhầm phiếu/hoàn tiền được duyệt ra sao? | Regression tài chính hiện có |
| Q17 | Trung tâm cần báo cáo số hồ sơ hay số lượt học, thu thực nhận hay doanh thu ghi nhận? Theo ngày/tháng/chi nhánh? | BR-11; RQ09 |
| Q18 | Người dùng thường làm việc trên desktop/mobile? Các thao tác hiện tốn thời gian hoặc hay sai là gì? | UX, ca usability G12/G17 |
| Q19 | Ai vận hành, sao lưu, phục hồi? Có yêu cầu thời gian gián đoạn, lưu dữ liệu hoặc domain/TLS cụ thể không? | G20 và phạm vi triển khai riêng |
| Q20 | Định dạng khóa luận/slide, thời gian demo và bằng chứng nghiệm thu của khoa là gì? | G19, mẫu báo cáo |

## 4. Mẫu biên bản chưa điền

```text
Mã khảo sát: [CẦN BỔ SUNG]
Ngày/địa điểm/hình thức: [CẦN BỔ SUNG]
Vai trò người trao đổi (ẩn danh khi cần): [CẦN BỔ SUNG]
Nguồn/chứng từ và quyền sử dụng: [CẦN BỔ SUNG]
Các câu hỏi đã trao đổi: [Qxx]
Trả lời/quan sát nguyên nghĩa: [CẦN BỔ SUNG]
Quy tắc xác nhận hoặc khác BR hiện tại: [CẦN BỔ SUNG]
Vấn đề chưa rõ: [CẦN BỔ SUNG]
Người ghi nhận/người xác nhận: [CẦN BỔ SUNG]
Tác động source/migration/FE/test/tài liệu: [CẦN BỔ SUNG]
```

## 5. Quy trình mục tiêu đề xuất để đối chiếu

Tiếp nhận hồ sơ → ghi nhận nguồn đánh giá đầu vào hoặc khóa OPEN_ENTRY → gợi ý/chọn lớp → quản lý xác nhận ngoại lệ khi cần → kiểm tra sĩ số/lịch và ghi danh → thu phí theo phiếu → học/điểm danh/nghỉ bù → nhập điểm → xét đủ dữ liệu → quản lý chốt kết quả → hoàn tất đăng ký/đóng lớp → báo cáo.

Quy trình này mô tả hệ thống **mục tiêu của đồ án**, không tự gọi là quy trình hiện tại của một trung tâm đã khảo sát. Quy trình hiện tại bằng giấy/Excel/phần mềm nào, quy mô, thời gian thao tác, tỷ lệ sai sót và lợi ích định lượng đều còn [CẦN BỔ SUNG: bằng chứng thực tế].

## 6. Phần còn thiếu có chặn code không?

- Tên trung tâm/người phỏng vấn/mẫu khoa: chưa đủ để nộp báo cáo hoàn chỉnh nhưng không chặn G02–G12 dùng giả định G01-v1.
- Bảng quy đổi trình độ/chứng chỉ: không có thì không xây quy đổi tự động; danh mục và tập chấp nhận được cấu hình rõ.
- Đơn vị duration cũ: chưa có bằng chứng thì giữ UNVERIFIED; không tự backfill. Có thể code đường đối soát trước, xác minh dữ liệu sau trên bản sao.
- Ngưỡng đạt/chuyên cần thực tế: policy theo lớp cần quản lý cấu hình/xác nhận; không tuyên bố mặc định phần mềm là quy định đã khảo sát.
- Quy định bắt buộc khác với G01-v1 khi được cung cấp: tạo bản sửa quyết định có lý do và đánh giá tác động trước khi dùng cho dữ liệu thật.

## 7. Bổ sung G13 — 08/11/2026: sổ bằng chứng và kế hoạch khảo sát

Không có phỏng vấn, biên bản, chứng từ trung tâm hoặc số liệu đo thực địa mới được cung cấp trong lượt này. **Kết quả thực tế về một trung tâm: CẦN BỔ SUNG.** Không điền tên tổ chức/người/ngày khảo sát tưởng tượng. Việc phần mềm chạy được và bộ test đạt không chứng minh quy tắc là thực tế vận hành của một cơ sở.

| Mã nguồn | Đã có | Mức được kết luận | Cần bổ sung |
|---|---|---|---|
| EV-01 | Mô tả đề tài/mục tiêu/đầu vào/đầu ra do người dùng gửi | Phạm vi nghiên cứu do tác giả đề xuất | Văn bản duyệt đề cương/mẫu khoa, nếu bắt buộc |
| EV-02 | Source và bàn giao G01–G12 | Quy trình phần mềm as-built, giả định G01-v1 và hành vi đã kiểm trong mức ghi ở từng báo cáo | Xác nhận của đơn vị áp dụng rằng chính sách phù hợp |
| EV-03 | Metadata schema V013 trên bản sao, đối chiếu G14 | Cấu trúc dữ liệu cài đặt; không có thông tin phỏng vấn/khảo sát người dùng | Mẫu hồ sơ/chứng từ đã ẩn danh và quyền sử dụng |
| EV-04 | Phỏng vấn/quan sát tại trung tâm | CẦN BỔ SUNG, chưa có bản ghi để phân tích | Vai trò người trả lời, thời điểm, câu trả lời nguyên nghĩa, chấp thuận sử dụng |
| EV-05 | Đánh giá hiệu quả/usability trước-sau | CẦN BỔ SUNG, chưa có số liệu đo | Cỡ mẫu, kịch bản/thời gian, cách đo và nhận xét xác thực |

### Câu hỏi bổ sung cho các quyết định G02–G12

| Mã | Cần hỏi/xem chứng cứ | Liên quan |
|---|---|---|
| Q21 | Teacher ở nhiều chi nhánh: ai có quyền sửa hồ sơ chung, ai chịu trách nhiệm bàn giao và khi nào được ngừng? | UC04/05; BR-02/03 |
| Q22 | Quản lý có được tự duyệt ngoại lệ mình đề nghị không? Có giới hạn/chứng từ nào bắt buộc? | UC10; hiện code cho A tự duyệt có audit |
| Q23 | Khi học xong nhưng FAIL, có được hoàn tất đăng ký? Công nợ xử lý riêng hay chặn học vụ? | UC13/14; hiện tách kết quả/quá trình học/tiền |
| Q24 | Ai được mở lại final? Nguồn dùng chung lớp cần mở toàn lớp hay theo học viên? Cách thông báo người dùng khi kết quả bị thay? | UC13; hiện mở toàn lớp, lưu snapshot/revision |
| Q25 | Giờ buổi cũ thiếu/sai được chứng minh bởi chứng từ gì? Ai ký đối soát, có lịch thi lại/học bổ sung ngoài phạm vi không? | UC11/12; legacy UNKNOWN, không tự quy đổi |
| Q26 | Chứng từ thu phí và cách xác định thu ròng/nợ/credit/hoàn; có cần hóa đơn thuế hoặc gateway thật? | UC14/15; hiện chỉ phiếu và ledger ghi nhận |

### Cách thu thập và xử lý

1. Chọn người theo các vai trò thực tế ở mục2, xin phép sử dụng thông tin; không tự ấn định số người hay thời lượng đã phỏng vấn.
2. Đi theo một hồ sơ ẩn danh xuyên quy trình; ghi rõ bước, đầu vào, người quyết định, chứng từ và ngoại lệ. Nếu chưa tiếp cận được, ghi CẦN BỔ SUNG, không viết quá khứ như đã quan sát.
3. Ghi kết quả vào mẫu mục4, gắn Qxx/EVxx/UC/RQ và nguồn; tách nguyên văn, diễn giải và đề xuất của tác giả.
4. Lập bảng khác biệt với BR hiện tại; nhờ người có trách nhiệm xác nhận. Quyết định thay đổi phần mềm phải có version/lý do/đánh giá tác động, không sửa code chỉ để khớp lời kể chưa xác minh.
5. Chỉ báo cáo lợi ích định lượng khi có phép đo/baseline/cách tính/mẫu. Không lấy số fixture/test làm số học viên/doanh thu của trung tâm.

### Mẫu sổ kết quả chưa điền

| Mã biên bản | Q / UC / RQ | Người/vai trò và thời điểm | Nguồn/trả lời nguyên nghĩa | Khác BR | Người xác nhận / tình trạng |
|---|---|---|---|---|---|
| CẦN BỔ SUNG | CẦN BỔ SUNG | CẦN BỔ SUNG | CẦN BỔ SUNG | CẦN BỔ SUNG | Chưa khảo sát |

G13 hoàn thành bộ công cụ/đặc tả và ghi trung thực phần thiếu; chưa thể gọi phần khảo sát thực tế hoặc toàn bộ khóa luận là hoàn thành. [Quy trình as-built](processes.md), [Use Case](use-cases.md).

