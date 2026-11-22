package io.github.org4j.vault;

import java.io.Serial;

/**
 * Vault could not be read: it refused, or it didn't answer at all. Never answered with an old copy,
 * so a caller fails closed.
 */
public class VaultUnavailableException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public VaultUnavailableException(Throwable cause) {
    super("Vault is not available", cause);
  }
}
