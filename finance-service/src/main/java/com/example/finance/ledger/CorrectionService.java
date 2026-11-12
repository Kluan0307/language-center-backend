package com.example.finance.ledger;

import com.example.finance.common.*;
import com.example.finance.payment.*;
import com.example.finance.security.*;
import com.example.finance.tuition.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class CorrectionService {
  private final PaymentRepository payments;
  @org.springframework.beans.factory.annotation.Autowired private jakarta.persistence.EntityManager em;
  private final TuitionService tuitions;
  private final PaymentCorrectionRepository corrections;
  private final FinanceLedger ledger;

  public record Request(
      @NotNull PaymentCorrection.Type type,
      @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 2)
          BigDecimal amount,
      Payment.PaymentMethod paymentMethod,
      @Size(max = 100) String referenceCode,
      @NotBlank @Size(min = 3, max = 500) String reason,
      @NotBlank @Size(max = 100) String requestKey,
      @NotNull @PositiveOrZero Long version) {}

  public PaymentCorrection create(Long paymentId, Request r) {
    if (Users.current().role() != Role.ADMIN && Users.current().role() != Role.SUPER_ADMIN)
      throw ApiException.forbidden();
    var p = payments.findById(paymentId).orElseThrow(ApiException::missing);
    var t = tuitions.lock(p.getTuitionId());
    if (!p.getBranchId().equals(t.getBranchId()))
      throw ApiException.conflict("Phiếu thu sai chi nhánh; cần đối soát");
    var reason = FinanceLedger.reason(r.reason());
    var prior = corrections.findByBranchIdAndRequestKey(t.getBranchId(), r.requestKey());
    if (prior.isPresent()) {
      var c = prior.get();
      if (!c.getPaymentId().equals(paymentId)
          || c.getType() != r.type()
          || c.getAmount().compareTo(r.amount()) != 0
          || c.getPaymentMethod() != r.paymentMethod()
          || !Objects.equals(c.getReferenceCode(), r.referenceCode())
          || !c.getReason().equals(reason)
          || !c.getExpectedVersion().equals(r.version()))
        throw ApiException.conflict("requestKey đã dùng cho giao dịch khác");
      return c;
    }
    if (r.version() == null || r.version() != t.getVersion())
      throw ApiException.conflict("Số dư đã thay đổi; hãy tải lại trước khi xử lý");
    ledger.requireSynced(t);
    if (p.getStatus() != Payment.Status.PAID)
      throw ApiException.conflict("Phiếu cũ chưa xác minh PAID không được tự xử lý");
    var total =
        ledger
            .totals(List.of(paymentId))
            .getOrDefault(paymentId, new FinanceLedger.Totals(BigDecimal.ZERO, BigDecimal.ZERO));
    if (r.amount().signum() <= 0 || r.amount().compareTo(p.getAmount().subtract(total.total())) > 0)
      throw ApiException.conflict("Số xử lý vượt phần phiếu còn hiệu lực");
    if (r.type() == PaymentCorrection.Type.VOID) {
      if (total.total().signum() != 0
          || r.amount().compareTo(p.getAmount()) != 0
          || r.paymentMethod() != null
          || r.referenceCode() != null)
        throw ApiException.conflict(
            "Chỉ đảo toàn bộ phiếu chưa hoàn; đảo phiếu không phải chi trả tiền");
    } else {
      if (t.getStatus() == Tuition.Status.CANCELLED)
        throw ApiException.conflict("Học phí hủy cũ cần đối soát riêng");
      if (r.paymentMethod() != Payment.PaymentMethod.CASH
          && r.paymentMethod() != Payment.PaymentMethod.BANK_TRANSFER)
        throw ApiException.bad("Chọn phương thức hoàn tiền");
      if (r.amount().compareTo(t.credit()) > 0)
        throw ApiException.conflict("Chỉ hoàn phần tiền dư; cần điều chỉnh số phải thu trước");
      if (r.paymentMethod() == Payment.PaymentMethod.BANK_TRANSFER
          && (r.referenceCode() == null || r.referenceCode().isBlank()))
        throw ApiException.bad("Nhập mã giao dịch chuyển hoàn");
    }
    var c = new PaymentCorrection();
    c.setPaymentId(paymentId);
    c.setTuitionId(t.getId());
    c.setBranchId(t.getBranchId());
    c.setType(r.type());
    c.setAmount(r.amount());
    c.setPaymentMethod(r.paymentMethod());
    c.setReferenceCode(r.referenceCode());
    c.setReason(reason);
    c.setRequestKey(r.requestKey());
    c.setExpectedVersion(r.version());
    c.setRecordedBy(Users.current().accountId());
    c.setRecordedByName(Users.current().username());
    corrections.saveAndFlush(c);
    ledger.setNet(t, ledger.net(t.getId()));
    if(t.getStudentId()!=null)com.example.finance.notification.StudentEvents.add(em,"finance:correction:"+c.getId(),"PAYMENT","RECEIPT_CORRECTED","Phiếu thu có cập nhật","Phiếu thu có lịch sử xử lý mới. Mở phiếu để kiểm tra.",t.getStudentId(),null,null,null,t.getBranchId(),"RECEIPT",paymentId.toString(),null,null);
    return c;
  }
}
