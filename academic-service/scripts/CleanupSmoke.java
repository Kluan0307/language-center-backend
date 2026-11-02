import java.sql.*;
import java.util.*;

public class CleanupSmoke {
  public static void main(String[] args) throws Exception {
    if (args.length != 1 || !args[0].matches("SMOKE[0-9]{14}"))
      throw new IllegalArgumentException(
          "Expected the exact SMOKEyyyyMMddHHmmss prefix produced by smoke.ps1");
    String url =
        System.getenv()
            .getOrDefault(
                "DB_URL",
                "jdbc:mysql://localhost:3306/language_center_db?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC");
    try (Connection c =
        DriverManager.getConnection(
            url,
            System.getenv().getOrDefault("DB_USERNAME", "root"),
            System.getenv("DB_PASSWORD"))) {
      c.setAutoCommit(false);
      try {
        String scope = " b.code like ?";
        String[] statements = {
          "delete a from attendances a join enrollments e on a.enrollment_id=e.id join"
              + " school_classes s on e.class_id=s.id join branches b on s.branch_id=b.id where"
              + scope,
          "delete l from leads l join branches b on l.branch_id=b.id where" + scope,
          "delete e from enrollments e join school_classes s on e.class_id=s.id join branches b on"
              + " s.branch_id=b.id where"
              + scope,
          "delete t from schedules t join school_classes s on t.class_id=s.id join branches b on"
              + " s.branch_id=b.id where"
              + scope,
          "delete s from school_classes s join branches b on s.branch_id=b.id where" + scope,
          "delete r from rooms r join branches b on r.branch_id=b.id where" + scope,
          "delete s from students s join branches b on s.branch_id=b.id where" + scope,
          "delete from courses where code like ?",
          "delete from branches where code like ?"
        };
        int removed = 0;
        for (String sql : statements)
          try (var s = c.prepareStatement(sql)) {
            s.setString(1, args[0] + "-%");
            removed += s.executeUpdate();
          }
        c.commit();
        System.out.println("Cleaned " + removed + " academic fixture rows for " + args[0]);
      } catch (Exception e) {
        c.rollback();
        throw e;
      }
      Set<String> expected =
          new TreeSet<>(
              List.of(
                  "branches",
                  "courses",
                  "rooms",
                  "students",
                  "leads",
                  "school_classes",
                  "schedules",
                  "enrollments",
                  "attendances"));
      try (var s =
              c.prepareStatement(
                  "select table_name from information_schema.tables where table_schema=DATABASE()");
          var r = s.executeQuery()) {
        while (r.next()) expected.remove(r.getString(1));
      }
      if (!expected.isEmpty())
        throw new IllegalStateException("Missing academic tables: " + expected);
      System.out.println("Verified all 9 academic tables.");
      try (var s = c.prepareStatement("select count(*) from branches where id in(1,2)");
          var r = s.executeQuery()) {
        r.next();
        if (r.getLong(1) != 2) throw new IllegalStateException("Missing seed branches");
      }
      System.out.println("Verified seed branches 1 and 2.");
    }
  }
}
