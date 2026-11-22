package io.github.org4j.vault;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Signing keys ({@link SigningKeys}), for an app whose API clients sign their requests. Where the
 * keys are is the app's own: {@code path}, {@code field} and {@code cache-ttl} have no default.
 * Without {@code path} there are no signing keys, and with it the other two must be set too.
 *
 * @param path the KV entry of a client's key, under {@code org4j.vault.mount}; {@code %s} is its
 *     client id (required in it: without it every client would verify with one key)
 * @param field the entry's key holding the base64 signing key
 * @param cacheTtl how long a key read from Vault is used before reading it again
 * @param maxEntries how many keys are kept at most; the least recently used goes first
 */
@Validated
@ConfigurationProperties(VaultConstants.SIGNING_KEYS_PREFIX)
public record VaultSigningKeysProperties(
    @Nullable @Pattern(regexp = ".*%s.*", message = "must contain %s, the client id") String path,
    @Nullable String field,
    @Nullable Duration cacheTtl,
    @DefaultValue(VaultConstants.DEFAULT_SIGNING_KEYS_MAX_ENTRIES) @Positive int maxEntries) {}
