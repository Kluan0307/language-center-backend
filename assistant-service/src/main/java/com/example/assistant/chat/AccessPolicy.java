package com.example.assistant.chat;

import com.example.assistant.mcp.SessionVerifier;
import java.util.*;
import java.util.regex.Pattern;

final class AccessPolicy {
  private AccessPolicy() {}

  /** No natural-language branch request silently changes or broadens the host-selected scope. */
  static boolean needsScope(
      String question,
      SessionVerifier.Identity actor,
      Map<String, Object> scope,
      TopicPolicy policy) {
    String s = policy.normalize(question);
    if (s.contains("toi la superadmin")
        || s.contains("toi la super admin")
        || s.contains("bo kiem quyen")
        || s.contains("act as super")) throw new ChatFault(403, "FORBIDDEN");
    if (actor.role() == SessionVerifier.Role.TEACHER) {
      if ((s.contains("tat ca hoc vien") || s.contains("all students"))
          && !(s.contains("lop cua toi") || s.contains("lop toi") || s.contains("my class")))
        throw new ChatFault(403, "FORBIDDEN");
      if (s.contains("day thay")
          && policy.family(question, TopicPolicy.Family.NONE) == TopicPolicy.Family.RESULTS)
        throw new ChatFault(403, "FORBIDDEN");
    }
    String mode = String.valueOf(scope.get("mode"));
    if (s.contains("toan he thong") || s.contains("whole system")) {
      if (actor.role() != SessionVerifier.Role.SUPER_ADMIN) throw new ChatFault(403, "FORBIDDEN");
      if (!mode.equals("WHOLE_SYSTEM")) return true;
    }
    if (s.contains("cac chi nhanh toi quan ly") && !mode.equals("ALL_ASSIGNED")) return true;
    if (s.contains("chi nhanh kia") || s.contains("chi nhanh khac")) return true;
    var matcher = Pattern.compile("(?:chi nhanh|branch)\\s*(?:#|so)?\\s*([0-9]{1,12})").matcher(s);
    while (matcher.find()) {
      long branch = Long.parseLong(matcher.group(1));
      if (actor.role() != SessionVerifier.Role.SUPER_ADMIN && !actor.branches().contains(branch))
        throw new ChatFault(403, "FORBIDDEN");
      if (!mode.equals("ACTIVE_BRANCH")
          || !(scope.get("branchIds") instanceof List<?> branches)
          || branches.size() != 1
          || !Objects.equals(ReferenceIndex.number(branches.getFirst()), branch)) return true;
    }
    return false;
  }
}
