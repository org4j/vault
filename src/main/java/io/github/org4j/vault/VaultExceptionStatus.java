package io.github.org4j.vault;

import io.github.org4j.error.StatusThrowable;

/** {@link VaultTransit}'s errors, defined in the library's own {@code vault-exception.yaml}. */
public enum VaultExceptionStatus implements StatusThrowable {
  /** Vault didn't answer, failed, or refused the app's token: a later call may work. 503. */
  VAULT_TRANSIT_UNAVAILABLE,
  /** Vault answered that the call can't work, such as a ciphertext with the wrong context. 500. */
  VAULT_TRANSIT_REFUSED;

  @Override
  public String getType() {
    return name();
  }
}
