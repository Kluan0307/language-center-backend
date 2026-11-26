package com.example.finance.assistant;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/finance/assistant-query")
public class FinanceInsightController {
  private final FinanceInsights service;

  @GetMapping("/tuitions")
  public ReadQuery.Result tuitions(@ModelAttribute FinanceInsights.Request r) {
    return service.tuitions(r);
  }

  @GetMapping("/receipts")
  public ReadQuery.Result receipts(@ModelAttribute FinanceInsights.Request r) {
    return service.receipts(r);
  }

  @GetMapping("/debt-statistics")
  public ReadQuery.Result debt(@ModelAttribute FinanceInsights.Request r) {
    return service.debt(r);
  }

  @GetMapping("/cash-flow")
  public ReadQuery.Result cash(@ModelAttribute FinanceInsights.Request r) {
    return service.cashFlow(r);
  }

  @GetMapping("/cash-comparison")
  public ReadQuery.Result compare(@ModelAttribute FinanceInsights.Request r) {
    return service.compare(r);
  }
}
