package com.example.assistant.chat;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ConversationStore {
  static final class Entry {
    final UUID id = UUID.randomUUID();
    final String owner, binding, initialKey;
    final ReentrantLock lock = new ReentrantLock();
    volatile long touched = System.nanoTime();
    final ArrayDeque<Map<String, String>> history = new ArrayDeque<>();
    final LinkedHashMap<UUID, String> fingerprints = new LinkedHashMap<>();
    final Map<UUID, ChatModels.Response> replies = new HashMap<>();
    final ReferenceIndex refs = new ReferenceIndex();
    TopicPolicy.Family family = TopicPolicy.Family.NONE;
    final Map<String, Long> selected = new HashMap<>();
    List<ChatModels.Choice> choices = List.of();
    ChatModels.Evidence pendingEvidence;
    String originalQuestion;
    boolean leadConfirmation;
    ChatModels.Evidence lastEvidence;

    Entry(String owner, String binding, String initialKey) {
      this.owner = owner;
      this.binding = binding;
      this.initialKey = initialKey;
    }

    void pendingClear() {
      choices = List.of();
      pendingEvidence = null;
      originalQuestion = null;
      leadConfirmation = false;
    }

    void remember(UUID request, String fingerprint, ChatModels.Response response, String question) {
      fingerprints.put(request, fingerprint);
      replies.put(request, response);
      while (fingerprints.size() > 8) {
        var old = fingerprints.keySet().iterator().next();
        fingerprints.remove(old);
        replies.remove(old);
      }
      if (!Set.of("OUT_OF_SCOPE", "UNSUPPORTED_ACTION").contains(response.status())) {
        history.addLast(Map.of("question", question, "status", response.status()));
        while (history.size() > 10) history.removeFirst();
      }
      touched = System.nanoTime();
    }
  }

  private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();
  private final Map<String, UUID> initial = new ConcurrentHashMap<>();

  synchronized Entry acquire(String owner, String binding, UUID id, UUID request) {
    purge();
    String key = owner + ":" + request;
    if (id == null) id = initial.get(key);
    if (id != null) {
      var entry = entries.get(id);
      if (entry == null || !entry.owner.equals(owner))
        throw new ChatFault(404, "CONVERSATION_NOT_FOUND");
      if (!entry.binding.equals(binding)) throw new ChatFault(409, "CONTEXT_CHANGED");
      if (!entry.lock.tryLock()) throw new ChatFault(409, "CONVERSATION_BUSY");
      return entry;
    }
    if (entries.size() >= 200) throw new ChatFault(429, "CAPACITY_EXCEEDED");
    var entry = new Entry(owner, binding, key);
    entry.lock.lock();
    entries.put(entry.id, entry);
    initial.put(key, entry.id);
    return entry;
  }

  public synchronized void delete(String owner, UUID id) {
    var entry = entries.get(id);
    if (entry == null || !entry.owner.equals(owner))
      throw new ChatFault(404, "CONVERSATION_NOT_FOUND");
    if (!entry.lock.tryLock()) throw new ChatFault(409, "CONVERSATION_BUSY");
    try {
      entries.remove(id);
      initial.remove(entry.initialKey);
    } finally {
      entry.lock.unlock();
    }
  }

  @Scheduled(fixedDelay = 60000)
  public synchronized void purge() {
    long now = System.nanoTime();
    for (var entry : List.copyOf(entries.values()))
      if (now - entry.touched > TimeUnit.MINUTES.toNanos(15) && entry.lock.tryLock())
        try {
          entries.remove(entry.id);
          initial.remove(entry.initialKey);
        } finally {
          entry.lock.unlock();
        }
  }
}
