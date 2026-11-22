package io.github.org4j.vault;

import static io.github.org4j.vault.VaultConstants.CLIENT_ID;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * API clients' request-signing keys, base64 in Vault at the app's path and field. A key is kept for
 * the TTL, so a burst of signed requests reads Vault once; the least recently used key goes once
 * {@code maxEntries} are kept. A missing key is never kept, and a Vault failure throws {@link
 * VaultUnavailableException}, never an old key.
 */
public class SigningKeys {

  private final VaultSecrets vault;
  private final String path;
  private final String field;
  private final Duration ttl;
  private final Clock clock;
  private final Map<String, Cached> cache;

  private static final class Cached {
    private final byte[] key;
    private final Instant until;

    private Cached(byte[] key, Instant until) {
      this.key = key;
      this.until = until;
    }
  }

  /**
   * Keeps keys read from the app's path and field.
   *
   * @param path the KV entry of a client's key; {@code %s} is the client id
   * @param field the entry's key holding the base64 signing key
   * @param maxEntries how many keys are kept at most
   */
  public SigningKeys(
      VaultSecrets vault, String path, String field, Duration ttl, Clock clock, int maxEntries) {
    if (maxEntries < 1) {
      throw new IllegalArgumentException("maxEntries must be at least 1, not " + maxEntries);
    }
    this.vault = Objects.requireNonNull(vault, "vault must not be null");
    this.path = Objects.requireNonNull(path, "path must not be null");
    this.field = Objects.requireNonNull(field, "field must not be null");
    this.ttl = Objects.requireNonNull(ttl, "ttl must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
    this.cache =
        Collections.synchronizedMap(
            new LinkedHashMap<String, Cached>(16, 0.75f, true) {
              @Override
              protected boolean removeEldestEntry(Map.Entry<String, Cached> eldest) {
                return size() > maxEntries;
              }
            });
  }

  /**
   * A client's signing key.
   *
   * @return empty when none is set, or when the id can't be a client id (never a path)
   * @throws VaultUnavailableException when the key has to be read and Vault can't be
   */
  public Optional<byte[]> find(String clientId) {
    if (!CLIENT_ID.matcher(clientId).matches()) {
      return Optional.empty();
    }
    var now = clock.instant();
    var cached = cache.get(clientId);
    if (cached != null) {
      if (now.isBefore(cached.until)) {
        return Optional.of(cached.key.clone());
      }
      cache.remove(clientId);
    }
    var key =
        vault
            .get(path.formatted(clientId))
            .map(secret -> secret.get(field))
            .map(Base64.getDecoder()::decode);
    key.ifPresent(bytes -> cache.put(clientId, new Cached(bytes.clone(), now.plus(ttl))));
    return key;
  }

  int cacheSize() {
    return cache.size();
  }
}
