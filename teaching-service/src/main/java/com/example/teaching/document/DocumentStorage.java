package com.example.teaching.document;

import com.example.teaching.common.ApiException;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class DocumentStorage {
  private final Path root;

  public DocumentStorage(@Value("${teaching.storage.path}") String directory) {
    root = Path.of(directory).toAbsolutePath().normalize();
  }

  public String save(MultipartFile file) {
    if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024)
      throw ApiException.bad("File must be between 1 byte and 10 MB");
    String name = filename(file);
    int dot = name.lastIndexOf('.');
    String extension = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    if (!Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "png", "jpg", "jpeg", "txt")
        .contains(extension)) throw ApiException.bad("Unsupported file type");
    String key = UUID.randomUUID().toString();
    try {
      Files.createDirectories(root);
      try (InputStream input = file.getInputStream()) {
        Files.copy(input, path(key));
      }
      return key;
    } catch (IOException ex) {
      delete(key);
      throw new ApiException(
          org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Document storage unavailable");
    }
  }

  public String filename(MultipartFile file) {
    String raw = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
    raw = raw.replace('\\', '/');
    raw = raw.substring(raw.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
    if (raw.isBlank() || raw.length() > 200) throw ApiException.bad("Invalid filename");
    return raw;
  }

  private Path path(String key) {
    if (key == null || !key.matches("[0-9a-f-]{36}")) throw ApiException.missing();
    Path p = root.resolve(key).normalize();
    if (!p.startsWith(root)) throw ApiException.missing();
    return p;
  }

  public Resource load(String key) {
    Path p = path(key);
    if (!Files.isRegularFile(p)) throw ApiException.missing();
    return new FileSystemResource(p);
  }

  public void delete(String key) {
    try {
      Files.deleteIfExists(path(key));
    } catch (IOException ignored) {
    }
  }
}
