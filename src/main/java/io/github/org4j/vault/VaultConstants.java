package io.github.org4j.vault;

import java.util.Set;
import java.util.regex.Pattern;

/** Where the library's settings live. */
public final class VaultConstants {

  /** The KV mount: {@code org4j.vault.mount}. */
  public static final String PROPERTIES_PREFIX = "org4j.vault";

  /** The client registry: {@code org4j.vault.clients.*}. */
  public static final String CLIENTS_PREFIX = "org4j.vault.clients";

  /** API clients' signing keys: {@code org4j.vault.signing-keys.*}. */
  public static final String SIGNING_KEYS_PREFIX = "org4j.vault.signing-keys";

  /** Vault Transit: {@code org4j.vault.transit.*}. */
  public static final String TRANSIT_PREFIX = "org4j.vault.transit";

  static final String DEFAULT_MOUNT = "secret";
  static final String DEFAULT_SIGNING_KEYS_MAX_ENTRIES = "10000";

  static final String CLIENTS_CACHE_TTL_MISSING =
      "org4j.vault.clients.cache-ttl is required with org4j.vault.clients.path";
  static final String SIGNING_KEYS_FIELD_MISSING =
      "org4j.vault.signing-keys.field is required with org4j.vault.signing-keys.path";
  static final String SIGNING_KEYS_CACHE_TTL_MISSING =
      "org4j.vault.signing-keys.cache-ttl is required with org4j.vault.signing-keys.path";

  static final String TRANSIT_PREFIX_FORMAT = "vault:v%d:";
  static final String TRANSIT_FAILED = "Vault Transit failed: {}: {}";
  static final String TRANSIT_KEY_MISSING = "Vault Transit has no key {}";
  static final Set<Integer> TRANSIT_REFUSED_STATUSES = Set.of(400, 404);
  static final Pattern CARD_NUMBER = Pattern.compile("\\b(?:\\d[ -]*?){13,19}\\b");

  static final Pattern CLIENT_ID = Pattern.compile("[A-Za-z0-9_-][A-Za-z0-9._-]*");

  static final String TOKEN_LOOKUP_SELF = "auth/token/lookup-self";
  static final String HEALTH_INDICATOR_BEAN = "vaultTokenHealthIndicator";
  static final String HEALTH_ERROR_DETAIL = "error";
  static final String HEALTH_EMPTY_ANSWER = "empty";

  static final String SPRING_CLOUD_VAULT =
      "org.springframework.cloud.vault.config.VaultAutoConfiguration";

  private VaultConstants() {}
}
