package io.github.org4j.vault;

import io.github.org4j.error.GeneralException;
import io.github.org4j.masking.Masking;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.vault.VaultException;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.VaultTransitOperations;
import org.springframework.vault.support.Ciphertext;
import org.springframework.vault.support.Plaintext;
import org.springframework.vault.support.VaultHmacRequest;
import org.springframework.vault.support.VaultTransitContext;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * Seals values with Vault Transit, whose keys never leave Vault. The context says where a value
 * lives ({@code <table>.<column>:<row id>}); with a derived key a ciphertext copied elsewhere does
 * not open.
 *
 * <p>Every failure is a {@link GeneralException}: {@link
 * VaultExceptionStatus#VAULT_TRANSIT_UNAVAILABLE} when a later call may work, {@link
 * VaultExceptionStatus#VAULT_TRANSIT_REFUSED} when Vault answered that it can't. The warning it
 * logs is masked, and card numbers in it are always hidden.
 *
 * {@snippet :
 * var sealed = transit.encrypt(KEY, "item.secret:" + id, plain);
 * var plain = transit.decrypt(KEY, "item.secret:" + id, sealed);
 * }
 */
public class VaultTransit {

  private static final Logger log = LoggerFactory.getLogger(VaultTransit.class);

  private final VaultTransitOperations transit;
  private final Masking masking;

  public VaultTransit(VaultOperations vault, String mount, Masking masking) {
    this.transit = vault.opsForTransit(mount);
    this.masking = Objects.requireNonNull(masking, "masking must not be null");
  }

  /** Seals a value: {@code vault:v<n>:…}. */
  public String encrypt(String key, String context, String plain) {
    return vault(
        () ->
            transit
                .encrypt(
                    key,
                    Plaintext.of(plain.getBytes(StandardCharsets.UTF_8)).with(context(context)))
                .getCiphertext());
  }

  /** Opens a sealed value; refused when the context is not the one it was sealed with. */
  public String decrypt(String key, String context, String sealed) {
    return vault(
        () -> transit.decrypt(key, Ciphertext.of(sealed).with(context(context))).asString());
  }

  /** Seals a value again with the key's latest version, without its plaintext leaving Vault. */
  public String rewrap(String key, String context, String sealed) {
    return vault(() -> transit.rewrap(key, sealed, context(context)));
  }

  /** The prefix of a value sealed with the key's latest version, {@code vault:v<n>:}. */
  public String latestPrefix(String key) {
    var found = vault(() -> transit.getKey(key));
    if (found == null) {
      log.warn(VaultConstants.TRANSIT_KEY_MISSING, key);
      throw new GeneralException(VaultExceptionStatus.VAULT_TRANSIT_REFUSED);
    }
    return VaultConstants.TRANSIT_PREFIX_FORMAT.formatted(found.getLatestVersion());
  }

  /**
   * A blind index: the same value always gives the same HMAC. The key version is pinned by the
   * caller, since a rotation would change every index.
   */
  public String hmac(String key, int keyVersion, String value) {
    return vault(
        () ->
            transit
                .getHmac(
                    key,
                    VaultHmacRequest.builder()
                        .plaintext(Plaintext.of(value.getBytes(StandardCharsets.UTF_8)))
                        .keyVersion(keyVersion)
                        .build())
                .getHmac());
  }

  private static VaultTransitContext context(String context) {
    return VaultTransitContext.builder().context(context.getBytes(StandardCharsets.UTF_8)).build();
  }

  private <T extends @Nullable Object> T vault(Supplier<T> call) {
    try {
      return call.get();
    } catch (VaultException | RestClientException e) { // Vault refused, or didn't answer at all
      log.warn(VaultConstants.TRANSIT_FAILED, e.getClass().getSimpleName(), masked(e.getMessage()));
      throw new GeneralException(
          refused(e)
              ? VaultExceptionStatus.VAULT_TRANSIT_REFUSED
              : VaultExceptionStatus.VAULT_TRANSIT_UNAVAILABLE);
    }
  }

  // Vault may quote the plaintext or the context back; a card number goes even when the app's
  // rules don't name it.
  private String masked(@Nullable String message) {
    var text = masking.text(message);
    if (text == null || text.isBlank()) {
      return "";
    }
    return VaultConstants.CARD_NUMBER
        .matcher(text)
        .replaceAll(number -> masking.maskValue(number.group()));
  }

  private static boolean refused(Exception e) {
    var cause = e instanceof HttpClientErrorException ? e : e.getCause();
    return cause instanceof HttpClientErrorException client
        && VaultConstants.TRANSIT_REFUSED_STATUSES.contains(client.getStatusCode().value());
  }
}
