package com.example.academic.stats;

import java.math.BigDecimal;
import java.util.*;

public final class StatsDto {
  private StatsDto() {}

  public record Overview(
      Long branchId,
      String branchName,
      Map<String, Long> studentsByStatus,
      Map<String, Long> studentsByLevel,
      long newStudentsThisMonth,
      Map<String, Long> classesByStatus,
      BigDecimal classOccupancyRate,
      Map<String, Long> leadsByStatus,
      BigDecimal leadConversionRate) {}

  public record Response(Overview overall, List<Overview> branches) {}
}
