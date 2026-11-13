package com.example.assistant.chat;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Materialize at most 16 KiB + 1, even for unknown/chunked/incorrect Content-Length. */
final class BoundedRequest extends HttpServletRequestWrapper {
  private static final tools.jackson.databind.json.JsonMapper STRICT_JSON =
      tools.jackson.databind.json.JsonMapper.builder()
          .enable(tools.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION)
          .enable(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .build();
  private final byte[] body;

  BoundedRequest(HttpServletRequest request) throws IOException {
    super(request);
    String encoding = request.getHeader("Content-Encoding");
    if (encoding != null && !encoding.equalsIgnoreCase("identity"))
      throw new ChatFault(415, "UNSUPPORTED_ENCODING");
    body = request.getInputStream().readNBytes(16385);
    if (body.length > 16384) throw new ChatFault(413, "REQUEST_TOO_LARGE");
    try {
      STRICT_JSON.readValue(body, Object.class);
    } catch (RuntimeException invalid) {
      throw new ChatFault(400, "INVALID_REQUEST");
    }
  }

  @Override
  public int getContentLength() {
    return body.length;
  }

  @Override
  public long getContentLengthLong() {
    return body.length;
  }

  @Override
  public BufferedReader getReader() {
    return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
  }

  @Override
  public ServletInputStream getInputStream() {
    var input = new ByteArrayInputStream(body);
    return new ServletInputStream() {
      public int read() {
        return input.read();
      }

      public int read(byte[] bytes, int offset, int length) {
        return input.read(bytes, offset, length);
      }

      public boolean isFinished() {
        return input.available() == 0;
      }

      public boolean isReady() {
        return true;
      }

      public void setReadListener(ReadListener listener) {
        throw new IllegalStateException("Synchronous bounded request only");
      }
    };
  }
}
