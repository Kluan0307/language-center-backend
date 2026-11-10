# finance-service

Học phí, phiếu thu và công nợ. Cổng **8084**, prefix `/api/v1/finance`; hiện có **8 API nghiệp vụ chính**.

## Tra cứu

- [Toàn bộ endpoint và quyền](../docs/api-list.md#3-finance--8-api-chính)
- [Body và response](../docs/api-contracts.md)
- [Hướng dẫn chạy/cấu hình toàn dự án](../README.md)
- [Migration dữ liệu cũ](../docs/migrations/README.md)

## Nhóm API

- GET/POST /tuitions; GET/PUT /tuitions/{id}. Có 3 alias /tuition cũ.
- GET/POST /payments; GET /payments/{id}.
- GET /stats/overview — doanh thu theo tháng/khóa/chi nhánh và công nợ hiện tại.

Các path rút gọn ở trên ghép với prefix của service. Qua gateway dùng cổng 8080 và giữ nguyên path.

## Quyền và phụ thuộc

SA/A/S đọc học phí và thu tiền; chỉ SA/A điều chỉnh học phí/xem doanh thu; T không truy cập tài chính.

Phụ thuộc: Academic để đọc snapshot đăng ký/giá bằng JWT người dùng.

## Chạy và kiểm thử

Chạy từ thư mục root, sau khi cấu hình `.env.local` theo README chính:

```powershell
.\scripts\run-service.ps1 finance-service
mvn -f finance-service/pom.xml test
```

Test dùng H2/mock/HTTP stub, không dùng MySQL hiện tại.

## Frontend giai đoạn 4

Đã nối học phí/công nợ, thu tiền từng đợt, phiếu thu và điều chỉnh học phí.
Xem [hướng dẫn và kết quả kiểm tra](../../../language-center-web-frontend/docs/phase-four.md).
TuitionDto trả người/thời gian/lý do điều chỉnh gần nhất. Retry một khoản thu phải giữ
nguyên requestKey, tuitionId, amount, paymentMethod, referenceCode và note.
Finance có 8 test đạt trên H2; luồng frontend cũng đã kiểm tra trên MySQL với fixture riêng.

Giai đoạn 6 bổ sung báo cáo trên frontend và chuẩn hóa nhóm tháng YYYY-MM theo thứ tự
thời gian. Finance hiện có 10 test đạt. Bộ lọc ngày áp dụng cho khoản thu PAID;
công nợ trả về luôn là số dư hiện tại, không phải số dư cuối kỳ.

## Cải thiện 5

Sổ điều chỉnh và correction VOID/REFUND bất biến, credit/balance tách riêng, version + requestKey; báo cáo thu ròng theo kỳ và đối soát nội bộ. [Quy tắc, API và V005](../docs/improvements/phase-five-finance-ledger.md).
