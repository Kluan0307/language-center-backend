package com.example.assistant.mcp;

import io.modelcontextprotocol.json.*;
import io.modelcontextprotocol.spec.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import reactor.core.publisher.Mono;

/** Private, one-client JSON-RPC transport. No sockets, external sessions or ambient identity. */
final class LocalMcpTransport implements McpServerTransportProvider {
  private final McpJsonMapper mapper;
  private final AtomicBoolean closed = new AtomicBoolean();
  private McpServerSession session;
  private Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> receiver;

  LocalMcpTransport(McpJsonMapper mapper) {
    this.mapper = mapper;
  }

  private McpSchema.JSONRPCMessage copy(McpSchema.JSONRPCMessage message) {
    try {
      String json = mapper.writeValueAsString(message);
      int limit = message instanceof McpSchema.JSONRPCRequest ? 65536 : 1048576;
      if (json.length() > limit) throw new McpFault("PAYLOAD_TOO_LARGE");
      return McpSchema.deserializeJsonRpcMessage(mapper, json);
    } catch (McpFault e) {
      throw e;
    } catch (Exception e) {
      throw new McpFault("INVALID_PROTOCOL_MESSAGE");
    }
  }

  private void check() {
    if (closed.get()) throw new McpFault("SESSION_CLOSED");
  }

  @Override
  public void setSessionFactory(McpServerSession.Factory factory) {
    session =
        factory.create(
            new McpServerTransport() {
              @Override
              public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
                return Mono.defer(
                    () -> {
                      check();
                      if (receiver == null) return Mono.error(new McpFault("SESSION_CLOSED"));
                      return receiver.apply(Mono.just(copy(message))).then();
                    });
              }

              @Override
              public <T> T unmarshalFrom(Object value, TypeRef<T> type) {
                return mapper.convertValue(value, type);
              }

              @Override
              public Mono<Void> closeGracefully() {
                return Mono.fromRunnable(() -> closed.set(true));
              }
            });
  }

  McpClientTransport client() {
    return new McpClientTransport() {
      @Override
      public Mono<Void> connect(
          Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> handler) {
        return Mono.fromRunnable(
            () -> {
              check();
              if (receiver != null) throw new McpFault("SESSION_CLOSED");
              receiver = handler;
            });
      }

      @Override
      public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
        return Mono.defer(
            () -> {
              check();
              if (session == null) return Mono.error(new McpFault("SESSION_CLOSED"));
              return session.handle(copy(message));
            });
      }

      @Override
      public <T> T unmarshalFrom(Object value, TypeRef<T> type) {
        return mapper.convertValue(value, type);
      }

      @Override
      public Mono<Void> closeGracefully() {
        return Mono.fromRunnable(() -> closed.set(true));
      }
    };
  }

  @Override
  public Mono<Void> notifyClients(String method, Object params) {
    return session == null ? Mono.empty() : session.sendNotification(method, params);
  }

  @Override
  public Mono<Void> closeGracefully() {
    return session == null ? Mono.fromRunnable(() -> closed.set(true)) : session.closeGracefully();
  }
}
