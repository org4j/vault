package io.github.org4j.vault;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.vault.VaultException;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.VaultVersionedKeyValueOperations;
import org.springframework.web.client.RestClientException;

/** Reads secrets from a KV version 2 mount, read only. */
public class VaultSecrets {

  private final VaultVersionedKeyValueOperations kv;

  public VaultSecrets(VaultOperations vault, String mount) {
    this.kv = vault.opsForVersionedKeyValue(mount);
  }

  /**
   * The latest version of a secret, each value as text.
   *
   * @return empty when there is no secret at the path
   * @throws VaultUnavailableException when Vault refuses or doesn't answer
   */
  public Optional<Map<String, String>> get(String path) {
    try {
      var versioned = kv.get(path);
      if (versioned == null || !versioned.hasData()) {
        return Optional.empty();
      }
      var secret = new LinkedHashMap<String, String>();
      versioned.getRequiredData().forEach((key, value) -> secret.put(key, String.valueOf(value)));
      return Optional.of(secret);
    } catch (VaultException | RestClientException e) {
      // VaultException when Vault answers with an error; RestClientException (connection refused,
      // a timeout) when it doesn't answer at all.
      throw new VaultUnavailableException(e);
    }
  }
}
