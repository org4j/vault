package io.github.org4j.vault;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * An app's clients, kept in one KV entry: one key per client id, its value the client's record in
 * the app's own format, passed on unread. The entry is read at most once per TTL, so a client added
 * or taken away in Vault counts without a restart. Past the TTL a failed read throws {@link
 * VaultUnavailableException}, never an old copy.
 */
public class ClientRegistry {

  private final VaultSecrets vault;
  private final String path;
  private final Duration ttl;
  private final Clock clock;
  private volatile @Nullable Read read;

  private record Read(Map<String, String> clients, Instant until) {}

  public ClientRegistry(VaultSecrets vault, String path, Duration ttl, Clock clock) {
    this.vault = vault;
    this.path = path;
    this.ttl = ttl;
    this.clock = clock;
  }

  /** One client's record, or empty when there is no such client. */
  public Optional<String> find(String clientId) {
    return Optional.ofNullable(all().get(clientId));
  }

  /** Every client's record, by client id. */
  public Map<String, String> all() {
    var now = clock.instant();
    var last = read;
    if (last != null && now.isBefore(last.until())) {
      return last.clients();
    }
    var clients = Map.copyOf(vault.get(path).orElse(Map.of()));
    read = new Read(clients, now.plus(ttl));
    return clients;
  }
}
