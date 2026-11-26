package com.example.assistant.chat;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.*;

class FinanceCareExpansionTest {
  ChatServiceTest h;

  @BeforeEach
  void setup() {
    h = new ChatServiceTest();
    h.setup();
  }

  @AfterEach
  void close() {
    h.clear();
  }

  @Test
  void debtUsesAggregateAndPinsOverdue() {
    h.model.add("get_debt_statistics", Map.of("overdue", false));
    var r = h.ask("Tổng nợ quá hạn là bao nhiêu?");
    assertEquals("ANSWER", r.status());
    assertEquals("true", h.backend.calls.getLast().get("overdue"));
    assertTrue(r.cards().toString().contains("outstanding=0"));
  }

  @Test
  void cashFlowKeepsNegativeNetAndUsesAuthorizedTool() {
    h.model.add("get_cash_flow", Map.of("from", "2026-11-01", "to", "2026-11-10"));
    var r = h.ask("Thực thu từ 2026-11-01 đến 2026-11-10?");
    assertEquals("ANSWER", r.status());
    assertTrue(r.cards().toString().contains("netCollected=-100"));
  }

  @Test
  void equalPeriodComparisonUsesServerComputedDifference() {
    h.model.add(
        "compare_cash_flow",
        Map.of(
            "from",
            "2026-11-01",
            "to",
            "2026-11-10",
            "compareFrom",
            "2026-11-11",
            "compareTo",
            "2026-11-20"));
    assertEquals(
        "ANSWER",
        h.ask("So sánh thực thu hai kỳ từ 2026-11-01 đến 2026-11-10 và 2026-11-11 đến 2026-11-20")
            .status());
  }

  @Test
  void receiptVoidAndRefundFiltersArePinned() {
    h.model.add("search_receipts", Map.of("receiptState", "ALL"));
    h.ask("Phiếu thu nào đã bị hủy?");
    assertEquals("VOIDED", h.backend.calls.getLast().get("receiptState"));
  }

  @Test
  void staffFollowupFilterIsTodayNotArbitraryTopFive() {
    h.backend.role = "STAFF";
    h.model.add("search_leads", Map.of("followUp", "OVERDUE"));
    h.ask("Khách nào của tôi cần liên hệ hôm nay?");
    assertEquals("TODAY", h.backend.calls.getLast().get("followUp"));
  }

  @Test
  void closedSupportByIdDisablesOpenOnly() {
    h.backend.role = "STAFF";
    h.model.add("search_support_requests", Map.of("supportId", 1, "openOnly", true));
    assertEquals("ANSWER", h.ask("Yêu cầu hỗ trợ #1 đang ở trạng thái nào?").status());
    assertEquals("false", h.backend.calls.getLast().get("openOnly"));
  }

  @Test
  void supportCountUsesCountTool() {
    h.model.add("get_support_statistics", Map.of());
    assertEquals("ANSWER", h.ask("Có bao nhiêu yêu cầu hỗ trợ đang mở?").status());
  }

  @Test
  void branchWithoutAdminUsesCenterMetric() {
    h.backend.role = "SUPER_ADMIN";
    h.model.add("get_center_statistics", Map.of());
    h.ask("Chi nhánh nào chưa có admin phụ trách?");
    assertEquals("BRANCHES_WITHOUT_ADMIN", h.backend.calls.getLast().get("metric"));
  }

  @Test
  void lockedAccountFilterIsHostPinned() {
    h.backend.role = "SUPER_ADMIN";
    h.model.add("find_management_accounts", Map.of("enabled", true));
    h.ask("Tài khoản nào đang bị khóa tại chi nhánh 1?");
    assertEquals("false", h.backend.calls.getLast().get("enabled"));
  }

  @Test
  void teacherCannotQueryFinanceCareSupportOrAccounts() {
    h.backend.role = "TEACHER";
    for (var q :
        List.of(
            "Tổng nợ quá hạn?",
            "Thực thu hôm nay?",
            "Có bao nhiêu yêu cầu hỗ trợ?",
            "Có bao nhiêu chi nhánh?"))
      assertEquals("FORBIDDEN", assertThrows(ChatFault.class, () -> h.ask(q)).code());
    assertTrue(h.backend.calls.isEmpty());
  }

  @Test
  void staffCannotQueryCashFlow() {
    h.backend.role = "STAFF";
    assertEquals(
        "FORBIDDEN", assertThrows(ChatFault.class, () -> h.ask("Thực thu hôm nay?")).code());
  }

  @Test
  void historicalDebtDoesNotBecomeCurrentZero() {
    assertEquals("UNSUPPORTED_CAPABILITY", h.ask("Tổng công nợ tháng trước?").status());
    assertTrue(h.backend.calls.isEmpty());
  }

  @Test
  void ambiguousStaffNameResolvesAccountNotStaffProfileId() {
    h.model.add("find_management_accounts", Map.of("keyword", "An", "accountRole", "STAFF"));
    var r = h.ask("Nhân viên An được giao bao nhiêu khách hàng đang mở?");
    assertEquals("CLARIFICATION_REQUIRED", r.status());
    assertEquals(ChatModels.EntityType.ACCOUNT, r.choices().getFirst().type());
    h.model.add("get_care_statistics", Map.of("assigneeId", 102));
    var chosen =
        h.ask(
            h.request(
                "Người thứ hai",
                r.conversationId(),
                UUID.randomUUID(),
                new ChatModels.Selection(ChatModels.EntityType.ACCOUNT, 102L)));
    assertEquals("ANSWER", chosen.status());
    assertEquals("102", h.backend.calls.getLast().get("assigneeId"));
  }

  @Test
  void pendingOrderExplanationUsesReviewedHelp() {
    h.backend.role = "STAFF";
    var r = h.ask("Đơn online đang chờ có được tính là đã đóng học phí không?");
    assertEquals("ANSWER", r.status());
    assertEquals("PAYMENT_ACCOUNTING", h.backend.calls.getLast().get("topic"));
    assertEquals(0, h.model.count);
  }

  @Test
  void cashFlowWithoutPeriodAsksInsteadOfGuessing() {
    assertEquals("CLARIFICATION_REQUIRED", h.ask("Thực thu của trung tâm?").status());
    assertEquals(0, h.model.count);
  }

  @Test
  void revenueNeedsMetricClarification() {
    assertEquals("CLARIFICATION_REQUIRED", h.ask("Doanh thu tháng này?").status());
    assertEquals(0, h.model.count);
  }

  @Test
  void receiptFollowupWithNoRefundStillReturnsTheReceiptAndActualZero() {
    h.model.add("search_receipts", Map.of());
    var first = h.ask("Lịch sử phiếu thu?");
    h.model.add("search_receipts", Map.of("receiptState", "REFUNDED"));
    var next =
        h.ask(
            h.request(
                "Phiếu này có hoàn tiền không?", first.conversationId(), UUID.randomUUID(), null));
    assertEquals("ANSWER", next.status());
    assertEquals("51", h.backend.calls.getLast().get("receiptId"));
    assertEquals("ALL", h.backend.calls.getLast().get("receiptState"));
    assertTrue(next.cards().toString().contains("refundedAmount=0"));
  }
}
