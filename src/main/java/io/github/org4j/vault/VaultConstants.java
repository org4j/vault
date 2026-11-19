package io.github.org4j.vault;

import java.util.Set;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VaultConstants {

  public static final String PROPERTIES_PREFIX = "org4j.vault";
  public static final String CLIENTS_PREFIX = "org4j.vault.clients";
  public static final String CLIENTS_CACHE_TTL_MISSING =
      "org4j.vault.clients.cache-ttl is required with org4j.vault.clients.path";
  public static final String DEFAULT_MOUNT = "secret";

  public static final String SIGNING_KEYS_PREFIX = "org4j.vault.signing-keys";
  public static final String SIGNING_KEYS_FIELD_MISSING =
      "org4j.vault.signing-keys.field is required with org4j.vault.signing-keys.path";
  public static final String SIGNING_KEYS_CACHE_TTL_MISSING =
      "org4j.vault.signing-keys.cache-ttl is required with org4j.vault.signing-keys.path";
  public static final int DEFAULT_SIGNING_KEYS_MAX_ENTRIES = 10_000;
  public static final String TRANSIT_PREFIX = "org4j.vault.transit";
  public static final String TRANSIT_PREFIX_FORMAT = "vault:v%d:";
  public static final String TRANSIT_FAILED = "Vault Transit failed: {}: {}";
  public static final String TRANSIT_KEY_MISSING = "Vault Transit has no key {}";

  public static final Set<Integer> TRANSIT_REFUSED_STATUSES = Set.of(400, 404);
  public static final Pattern CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,19}\\b");

  public static final String TOKEN_LOOKUP_SELF = "auth/token/lookup-self";
  public static final String HEALTH_INDICATOR_BEAN = "vaultTokenHealthIndicator";
  public static final String HEALTH_ERROR_DETAIL = "error";
  public static final String HEALTH_EMPTY_ANSWER = "empty";

  public static final String SPRING_CLOUD_VAULT =
      "org.springframework.cloud.vault.config.VaultAutoConfiguration";

  static final Pattern CLIENT_ID = Pattern.compile("[A-Za-z0-9_-][A-Za-z0-9._-]*");
}
