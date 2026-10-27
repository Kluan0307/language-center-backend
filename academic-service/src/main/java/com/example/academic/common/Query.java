package com.example.academic.common;

import jakarta.persistence.criteria.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

public final class Query {
  private Query() {}

  public static Pageable page(int page, int size) {
    if (page < 0 || size < 1 || size > 200)
      throw ApiException.bad("page phải >= 0, size từ 1 đến 200");
    return PageRequest.of(page, size, Sort.by("id").descending());
  }

  public static <T> Specification<T> all() {
    return (r, q, b) -> b.conjunction();
  }

  public static <T> Specification<T> eq(String field, Object value) {
    return (r, q, b) -> value == null ? b.conjunction() : b.equal(path(r, field), value);
  }

  public static <T> Specification<T> branch(Long id) {
    return eq("branchId", id);
  }

  public static <T> Specification<T> keyword(String text, String... fields) {
    return (r, q, b) -> {
      if (text == null || text.isBlank()) return b.conjunction();
      String pattern =
          "%"
              + text.trim()
                  .toLowerCase(Locale.ROOT)
                  .replace("\\", "\\\\")
                  .replace("%", "\\%")
                  .replace("_", "\\_")
              + "%";
      return b.or(
          Arrays.stream(fields)
              .map(f -> b.like(path(r, f).as(String.class), pattern, '\\'))
              .toArray(Predicate[]::new));
    };
  }

  public static Path<?> path(Path<?> root, String field) {
    for (String p : field.split("\\.")) root = root.get(p);
    return root;
  }
}
