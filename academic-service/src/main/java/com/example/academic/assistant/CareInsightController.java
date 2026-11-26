package com.example.academic.assistant;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/academic/assistant-query")
public class CareInsightController {
  private final CareInsights service;

  @GetMapping("/leads")
  public ReadQuery.Result leads(@ModelAttribute CareInsights.Request r) {
    return service.leads(r);
  }

  @GetMapping("/care-statistics")
  public ReadQuery.Result leadStats(@ModelAttribute CareInsights.Request r) {
    return service.leadStats(r);
  }

  @GetMapping("/support-requests")
  public ReadQuery.Result support(@ModelAttribute CareInsights.Request r) {
    return service.support(r);
  }

  @GetMapping("/support-statistics")
  public ReadQuery.Result supportStats(@ModelAttribute CareInsights.Request r) {
    return service.supportStats(r);
  }

  @GetMapping("/center-statistics")
  public ReadQuery.Result center(@ModelAttribute CareInsights.Request r) {
    return service.center(r);
  }
}
