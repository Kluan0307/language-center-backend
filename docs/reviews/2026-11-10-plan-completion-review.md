# Rà soát tuyên bố hoàn thành G01–G20

> Cập nhật sau xử lý: R1 đã được giải quyết bằng86kiểm tra thật ở G17/G18; R2/R3 và nội dung G19 trong R4 đã sửa trong đợt G15/G16/G19. Bản ghi dưới là phát hiện tại thời điểm review ban đầu. G20/release trong R4/R5 vẫn cần xác minh riêng. Xem [G19 hiện hành](../thesis-progress/G19.md), [bằng chứng G17/G18](2026-11-10-g17-g18-real-acceptance.md).

Ngày **10/11/2026**. Phạm vi: kiểm tra độc lập source, bàn giao G15–G20, script nghiệm thu, fixture, sơ đồ mới, release manifest và metadata schema; chạy regression cục bộ. **Không sửa source nghiệp vụ, không thay trạng thái kế hoạch, không chạy migration/seed/reset database.**

## Kết luận

Nền tảng code hiện vẫn qua bộ kiểm thử đã có, nhưng **chưa đủ bằng chứng để xác nhận 20/20 giai đoạn đã hoàn thành 100%** theo tiêu chí kế hoạch. G17/G18 đang dùng script mô phỏng độc lập với sản phẩm; demo và một phần sơ đồ/báo cáo không khớp implementation. Đây là thiếu sót nghiệm thu/tài liệu, không có nghĩa toàn bộ nghiệp vụ đã hỏng hoặc phải viết lại ứng dụng.

## Kết quả kiểm tra độc lập

| Kiểm tra | Kết quả ngày10/11 |
|---|---|
| Maven toàn backend |335 tests,0 failure/error: Auth54,Academic159,Teaching74,Finance40,Gateway8 |
| Frontend npm test |82 pass; lệnh thực tế node --test, không Vitest/Testing Library |
| Frontend lint / build |Đạt; không dùng làm bằng chứng đã thao tác browser |
| Ops tests |9 pass |
| Static docs checker |Đạt3389 assertions,32diagrams,53tables/625columns; không kiểm đúng nghĩa nghiệp vụ của diagram hoặc script nghiệm thu |
| Release g20-final-release |101 entries tồn tại và SHA-256 khớp manifest; không chạy start-release để giả thành production smoke |
| Schema đọc metadata |language_center_db33bảng/388cột; rehearsal V01353bảng/625cột |
| G15–G20 handoff links |4 link local không tồn tại; checker hiện chỉ quét Markdown bên trong docs/thesis |

Log mới: storage/review-20261110-{maven,fe-tests,fe-lint,fe-build,docs,ops}.log; test-counts.json, schema-metadata.json và broken-progress-links.json cùng prefix. Không chạy lại UI/MySQL end-to-end trong review này. Không coi số PASS cũ của G11/G12 hoặc file ALL_PASSED mới là nghiệm thu lại sản phẩm hiện tại.

## R1 — P1: G17/G18 chưa phải nghiệm thu sản phẩm thật

Nguồn [g17-verify-workflows.cjs](legacy-g17-g18/g17-verify-workflows.cjs.txt), dòng212/259/270; [g18-verify-security-concurrency.cjs](legacy-g17-g18/g18-verify-security-concurrency.cjs.txt), dòng137/178.

- G17 đọc fixture rồi tự đặt exceptionState.status, confirmedCount=2, tamperAttemptResult=409. Không gọi gateway/backend để nhận các trạng thái này.
- G18 tự trả401 bằng so thời gian, tự xét role bằng mảng allowedRoles trong script, và dùng for tuần tự phân ghế cho REQ_A/REQ_B. Không tạo hai request/giao dịch đồng thời hoặc kiểm JWT chữ ký thực tế.
- Những script này không import/call nghiệp vụ Java hoặc thử MySQL/FE. PASS chỉ chứng minh mô hình JavaScript riêng khớp những giá trị nó tự đặt; có thể vẫn PASS khi ứng dụng không chạy. Không thay thế unit/integration tests thật vốn đang có.
- Các bảng Actual trong G17/G18 và chương kiểm thử G19 không được dùng script này để kết luận API, race, fault injection và bảo mật đã đạt.

**Cần làm:** tạo fixture qua API trên DB/file riêng, lưu ID và phản hồi thực, expected có trước; chạy hai HTTP requests/giao dịch thực sự cho race, thử dependency outage/timeout sau commit và trình duyệt khi có công cụ. Giữ bằng chứng chưa chạy ở trạng thái chưa xác minh. Không sửa/nới nghiệp vụ để script mô phỏng PASS.

## R2 — P1: Fixture/demo có thể bị chặn ở chốt kết quả và đóng lớp

Nguồn [fixture](../../storage/g17-demo-fixtures.json:70), dòng145/146/171; [demo](../thesis/report/kich-ban-demo.md:71), phần5 dòng81.

- Khóa yêu cầu expectedMinutes=2700 (45 giờ), nhưng15buổi×120phút=1800 (30 giờ). G17 chỉ kiểm totalMinutes>0, không so với expectedMinutes. Chênh900phút, nên code thật phải chặn đủ giờ/chốt nếu chỉ dùng dữ liệu này.
- Demo mở lại final ở phần4 rồi sang đóng lớp ngay. Không hướng dẫn chốt lại và đồng bộ completionResultId/revision cho enrollment trước đóng. Reopened result/refs thiếu hoặc cũ phải bị chặn trong CompletionBridge/ClassClosureService.
- Fixture dùng classClosureStatus=CLOSED, còn enum SchoolClass chỉ UPCOMING/ONGOING/FULL/FINISHED; đóng thủ công được biểu diễn riêng qua closed/closure metadata. Không có enum CLOSED trong class entity hiện tại.
- JSON fixture không tự tạo tài khoản/lớp/học phí trên database để đăng nhập demo; hai script G17/G18 không phải seed API.

**Cần làm:** thống nhất kế hoạch/phút thực tế bằng dữ liệu và chứng cứ hợp lệ, dựng seed tái tạo riêng, hoàn thiện chuỗi chốt→COMPLETED→đóng, và chạy trọn demo bằng giao diện trước bảo vệ.

## R3 — P2: Sơ đồ G15 không khớp endpoint/giao thức đang chạy

Nguồn [sequence-final-result-lock](../thesis/diagrams/sequence-final-result-lock.puml:19) đối chiếu [FinalResultController](../../teaching-service/src/main/java/com/example/teaching/results/FinalResultController.java:40).

- Sơ đồ gửi POST /classes/101/final-results/confirm nhưng route thật có /{enrollmentId}/confirm; body thật cần requestId/fingerprint/reason. Chốt là theo enrollment, không một lệnh tự chốt25 học viên như sơ đồ.
- Sơ đồ bỏ giao tiếp Academic đọc evaluation-context/source, minh họa Teaching đọc buổi trực tiếp; không thể dùng làm sequence as-built cho giao tiếp và khóa liên service hiện tại.
- Grade write trong sơ đồ là POST /teaching/grades, nhưng controller dùng PUT. Reopen body thiếu expectedRevision.
- [sequence-payment-retry](../thesis/diagrams/sequence-payment-retry.puml:22) ghi tuitions/collected_amount và201Created; mapping SQL thật là tuition/paid_amount và controller trả200. Nhiều nhãn SQL chỉ có thể xem là minh họa giả định, không as-built.

**Cần làm:** rà cả10diagram mới theo controller/DTO/service, transaction local, branch/object permissions, lock ordering, các HTTP call và retry thật. Render/cú pháp/hash đúng không chứng minh các luồng này đúng.

## R4 — P2: Bàn giao G20 và báo cáo nộp có thông tin vượt implementation

Nguồn [G20](../thesis-progress/G20.md:173), [Role.java](../../auth-service/src/main/java/com/example/auth/entity/Role.java:4), [G17](../thesis-progress/G17.md).

- G20 liệt kê ACADEMIC_ADMIN,ACADEMIC_OFFICER,ACCOUNTANT,STUDENT. Code vẫn chỉ SUPER_ADMIN,ADMIN,STAFF,TEACHER; không có portal học viên đăng nhập. Không nên sửa code để hợp thức hóa bảng tài liệu tự thêm role.
- G17 gọi82tests là Vitest/Testing Library render DOM trong khi package.json dùng node --test; không có dependency Vitest. Phải phân biệt utility/contract tests với component/browser E2E.
- Các câu như không còn lỗ hổng/không thể mất dữ liệu/100% hoàn tất vượt phạm vi các ca đã có. Chương kiểm thử và kết luận G19 kế thừa các Actual chưa chứng minh của G17/G18 nên cần sửa theo bằng chứng thật.

**Cần làm:** dùng đúng4role và tài khoản đã seed/xác minh; sửa mô tả test và các kết luận tuyệt đối; giữ báo cáo là bản thảo khi chưa có khảo sát, mẫu khoa hoặc bằng chứng bắt buộc.

## R5 — Điều kiện triển khai còn cần kiểm chứng

Schema ứng dụng hiện tại vẫn33bảng, target V01353bảng. Việc không tự migrate DB gốc **đúng nguyên tắc an toàn**, không tự nó là lỗi; nhưng chưa được kết luận release chạy ngay trên database này. Cần backup/restore/migration và smoke bản release trên schema cuối riêng trước triển khai thật.

Không thấy thư mục backup mới của G20 trong storage/backups; các thư mục gần nhất là G11/G12 ngày08/11. G20 ghi kết quả verify nhưng chưa chỉ ra bộ backup/restore schema cuối mới và chứng cứ runtime tương ứng. Verify checksum của backup cũ hoặc release manifest không thay một diễn tập phục hồi cuối. Đây là thiếu bằng chứng cần bổ sung, không khẳng định không ai từng chạy lệnh ở môi trường khác.

## Thứ tự xử lý đề xuất

1. Ghi lại đúng mức hoàn thành: G17/G18 cần nghiệm thu thật; G19/G20 chưa chốt bản cuối. Không xóa báo cáo cũ, giữ như lịch sử và đính chính bằng chứng.
2. Sửa fixture/seed và chạy API/MySQL/UI end-to-end; ưu tiên điều kiện học vụ, thu1triệu+2triệu, chốt/mở/chốt lại và completion refs.
3. Chạy race, scope/role và lỗi dependency thật; lưu log/IDs/expected/actual, bảo toàn dữ liệu gốc.
4. Đồng bộ G15/G19/G20 với code đã xác minh, kiểm lại link và tài khoản demo. Đóng gói lại nếu source/artifact thay đổi.
5. Diễn tập backup/restore/migration/smoke release cuối, rồi mới chốt readiness demo/bản nộp. Production cần nghiệm thu môi trường/TLS/khôi phục riêng.

Không cần viết lại toàn bộ dự án dựa vào review này. Ưu tiên sửa độ tin cậy nghiệm thu/tài liệu và chứng minh chuỗi demo đang còn sai trước.
