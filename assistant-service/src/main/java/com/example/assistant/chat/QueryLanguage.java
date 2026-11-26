package com.example.assistant.chat;

import java.util.*;

/** Finite vocabulary aliases, never a source of identity, SQL or permissions. */
final class QueryLanguage {
  private QueryLanguage() {}

  private static final Map<String, String> PROGRAM_ALIASES = Map.of(
        "GE", "ge|tieng anh tong quat|tieng anh tong hop",
        "COMMUNICATION", "communication|giao tiep",
        "IELTS", "ielts", "TOEIC", "toeic");
  private static final Map<String, String> AUDIENCE_ALIASES = Map.of(
        "PRESCHOOL", "preschool|mau giao|mam non",
        "PRIMARY", "primary|tieu hoc|cap 1",
        "SECONDARY", "secondary|thcs|trung hoc co so|cap 2",
        "HIGH_SCHOOL", "high_school|thpt|trung hoc pho thong|cap 3",
        "ADULT", "adult|nguoi lon|nguoi di lam");

  /** Match complete phrases; never silently pick one of multiple groups. */
  static Map<String, Object> classification(String s) {
    var out = new LinkedHashMap<String, Object>();
    matchClassification(s, "programCode", PROGRAM_ALIASES, out);
    matchClassification(s, "audienceCode", AUDIENCE_ALIASES, out);
    return out;
  }

  static boolean ambiguousClassification(String s) {
    return classificationMatches(s, PROGRAM_ALIASES).size() > 1
        || classificationMatches(s, AUDIENCE_ALIASES).size() > 1;
  }

  private static void matchClassification(String s, String key, Map<String, String> options, Map<String, Object> out) {
    var matches = classificationMatches(s, options);
    if (matches.size() == 1) out.put(key, matches.getFirst());
  }

  private static List<String> classificationMatches(String s, Map<String, String> options) {
    return options.entrySet().stream().filter(e -> java.util.regex.Pattern
        .compile("(?<![\\p{L}\\p{N}_])(?:" + e.getValue() + ")(?![\\p{L}\\p{N}_])")
        .matcher(s).find()).map(Map.Entry::getKey).toList();
  }

  static String aliases(String text) {
    String s = " " + text + " ";
    if(s.matches("(?s).*\\bgv[0-9]{3,}\\b.*")&&!s.contains("giao vien")) s="giao vien "+s;
    if(s.matches("(?s).*\\bhv[0-9]{4,}\\b.*")&&!s.contains("hoc vien")) s="hoc vien "+s;
    for (var pair :
        List.of(
            List.of("giang vien", "giao vien"),
            List.of("hoc sinh", "hoc vien"),
            List.of("sinh vien", "hoc vien"),
            List.of("van bang", "bang cap"),
            List.of("trinh do hoc van", "bang cap"),
            List.of("chung nhan", "chung chi"),
            List.of("co ay", "nguoi do"),
            List.of("thay ay", "nguoi do"),
            List.of("anh ay", "nguoi do"),
            List.of("chi ay", "nguoi do"),
            List.of("ban ay", "nguoi do"),
            List.of("co may", "co bao nhieu"),
            List.of("dem so", "dem"),
            List.of("lich giang day", "lich day"),
            List.of("phieu dong tien", "phieu thu"),
            List.of("cong no tre han", "cong no qua han")))
      s =
          s.replaceAll(
              "(?<![\\p{L}\\p{N}])"
                  + java.util.regex.Pattern.quote(pair.get(0))
                  + "(?![\\p{L}\\p{N}])",
              java.util.regex.Matcher.quoteReplacement(pair.get(1)));
    s =
        s.replaceAll("\\bgv\\s+(?=[\\p{L}#0-9])", "giao vien ")
            .replaceAll("\\bhv\\s+(?=[\\p{L}#0-9])", "hoc vien ")
            .replaceAll("\\bnv\\s+(?=[\\p{L}#0-9])", "nhan vien ")
            .replaceAll("\\bcn\\s*(?=#?[0-9])", "chi nhanh ");
    return s.trim();
  }

  static boolean pronoun(String s) {
    return s.matches(
        ".*\\b(?:nguoi do|hoc vien nay|hoc vien do|giao vien nay|giao vien do|nhan vien nay|nhan"
            + " vien do|lop nay|lop do|buoi do|bai do|bai kiem tra do|phieu nay|phieu do|nguoi vua"
            + " tim|khoang thoi gian do|khoang ngay do|khoang ngay da chon)\\b.*");
  }

  static boolean refinement(String s) {
    if (s.matches("^(?:con|chi lay|chi xem|loc|giu) .+") && !classification(s).isEmpty()) return true;
    return s.matches(
        "^(?:con |chi lay |chi xem |loc |giu )(?:(?:duoi|tren|tu|khong qua|toi da) .+|.*(?:dang"
            + " hoat dong|ngung hoat dong|tat ca trang thai|da khoa|qua han|hom nay|tuan sau|tuan"
            + " nay|hoc bu|day thay|diem 0|chua co diem).*)[?.!]*$");
  }

  static boolean total(String s) {
    return s.contains("khong chi trang nay")
        || s.contains("tong tat ca ket qua")
        || s.contains("tong so ket qua");
  }

  static boolean followup(String s) {
    return pronoun(s) || refinement(s) || total(s);
  }

  static Map<String, Object> refine(
      Map<String, Object> previous, Map<String, Object> requested, String s, String tool) {
    var out = new LinkedHashMap<>(previous);
    var allowed = new HashSet<String>();
    allowed.addAll(classification(s).keySet());
    if (s.matches(".*(?:duoi |tren |trieu|toi da |khong qua ).*"))
      allowed.addAll(List.of("minTuition", "maxTuition", "maxInclusive"));
    if (s.contains("tuan") || s.contains("hom nay")) allowed.addAll(List.of("from", "to"));
    if (s.contains("qua han")) allowed.addAll(List.of("overdue", "followUp"));
    if (s.contains("hom nay")) allowed.add("followUp");
    if (s.contains("diem 0") || s.contains("chua co diem")) allowed.add("scoreFilter");
    if (s.contains("hoc bu")) out.put("kind", "MAKEUP");
    if (s.contains("day thay")) out.put("substituteOnly", true);
    String activeField =
        Set.of("find_management_accounts", "get_account_statistics").contains(tool)
            ? "enabled"
            : "activeOnly";
    if (s.contains("dang hoat dong")) out.put(activeField, true);
    if (s.contains("tat ca trang thai")) {
      if (activeField.equals("enabled")) out.remove("enabled");
      else out.put(activeField, false);
    }
    if (s.contains("da khoa")) out.put("enabled", false);
    requested.forEach(
        (k, v) -> {
          if (allowed.contains(k)) out.put(k, v);
        });
    out.remove("page");
    return out;
  }
}
