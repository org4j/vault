package io.github.org4j.vault;

import io.github.org4j.error.StatusThrowable;

public enum VaultExceptionStatus implements StatusThrowable {
  VAULT_TRANSIT_UNAVAILABLE,
  VAULT_TRANSIT_REFUSED;

  @Override
  public String getType() {
    return name();
  }
}
