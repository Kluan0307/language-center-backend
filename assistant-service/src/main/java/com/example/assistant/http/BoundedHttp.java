package com.example.assistant.http;

import java.io.ByteArrayOutputStream;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;

public final class BoundedHttp {
  private BoundedHttp() {}

  public static HttpResponse<byte[]> send(
      HttpClient http, HttpRequest request, int limit, Duration timeout) throws Exception {
    var pending = http.sendAsync(request, info -> new Body(limit));
    try {
      return pending.get(Math.max(1, timeout.toMillis()), TimeUnit.MILLISECONDS);
    } finally {
      if (!pending.isDone()) pending.cancel(true);
    }
  }

  private static final class Body implements HttpResponse.BodySubscriber<byte[]> {
    private final int limit;
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final CompletableFuture<byte[]> done = new CompletableFuture<>();
    private Flow.Subscription subscription;

    Body(int limit) {
      this.limit = limit;
    }

    public CompletionStage<byte[]> getBody() {
      return done;
    }

    public void onSubscribe(Flow.Subscription s) {
      subscription = s;
      s.request(1);
    }

    public void onNext(List<ByteBuffer> buffers) {
      if (bytes.size() + buffers.stream().mapToLong(ByteBuffer::remaining).sum() > limit) {
        subscription.cancel();
        done.completeExceptionally(new IllegalStateException("BODY_LIMIT"));
        return;
      }
      for (var b : buffers) {
        byte[] part = new byte[b.remaining()];
        b.get(part);
        bytes.writeBytes(part);
      }
      subscription.request(1);
    }

    public void onError(Throwable e) {
      done.completeExceptionally(e);
    }

    public void onComplete() {
      done.complete(bytes.toByteArray());
    }
  }
}
