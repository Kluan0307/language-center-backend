package com.example.assistant.chat;

import java.util.*;
import java.util.regex.Pattern;

final class ReferenceIndex {
  private final Map<String, LinkedHashSet<Long>> ids = new HashMap<>();

  void clear() {
    ids.clear();
  }

  void add(String key, Object value) {
    Long id = number(value);
    if (id == null) return;
    var set = ids.computeIfAbsent(key, k -> new LinkedHashSet<>());
    set.add(id);
    while (set.size() > 100) set.remove(set.iterator().next());
  }

  static Long number(Object value) {
    try {
      long id = new java.math.BigDecimal(value.toString()).longValueExact();
      return id > 0 ? id : null;
    } catch (Exception e) {
      return null;
    }
  }

  boolean permits(String key, Object value, String question) {
    Long id = number(value);
    if (id == null) return false;
    if (ids.getOrDefault(key, new LinkedHashSet<>()).contains(id)) return true;
    var m =
        Pattern.compile("(?:#|\\bid\\s*)\\s*([1-9][0-9]{0,17})", Pattern.CASE_INSENSITIVE)
            .matcher(question);
    while (m.find()) if (Objects.equals(number(m.group(1)), id)) return true;
    return false;
  }

  void collect(ChatModels.Evidence evidence) {
    if (evidence.payload().get("data") instanceof Map<?, ?> parent)
      for (String key : List.of("examId", "classId", "sessionId", "studentId", "enrollmentId"))
        add(key, parent.get(key));
    if (evidence.tool().equals("get_my_personnel_profile")
        && evidence.payload().get("data") instanceof Map<?, ?> self) {
      add("personnelId", self.get("id"));
      add("teacherId", self.get("id"));
    }
    String key =
        switch (evidence.tool()) {
          case "search_courses" -> "courseId";
          case "search_classes" -> "classId";
          case "find_students" -> "studentId";
          case "find_personnel", "get_my_personnel_profile" -> "personnelId";
          case "list_sessions" -> "sessionId";
          case "list_learning_exams" -> "examId";
          case "search_tuitions" -> "tuitionId";
          case "search_receipts" -> "receiptId";
          case "search_leads" -> "leadId";
          case "find_management_accounts" -> "accountId";
          default -> null;
        };
    for (var row : rows(evidence.payload().get("data"))) {
      if (evidence.tool().equals("find_management_accounts")
          && "STAFF".equals(row.get("accountRole"))) add("assigneeId", row.get("accountId"));
      if (evidence.tool().equals("find_personnel") && "TEACHER".equals(row.get("kind")))
        add("teacherId", row.get("id"));
      if (key != null) add(key, row.get("id"));
      for (var entry : row.entrySet())
        if (entry.getKey().endsWith("Id")) add(entry.getKey(), entry.getValue());
    }
  }

  static List<Map<String, Object>> rows(Object data) {
    if (data instanceof Map<?, ?> map) {
      if (map.get("records") != null) return rows(map.get("records"));
      if (map.get("content") instanceof List<?> list)
        return list.stream()
            .filter(Map.class::isInstance)
            .map(
                value -> {
                  var row = new LinkedHashMap<String, Object>();
                  ((Map<?, ?>) value).forEach((k, v) -> row.put(String.valueOf(k), v));
                  return (Map<String, Object>) row;
                })
            .toList();
    }
    return List.of();
  }
}
