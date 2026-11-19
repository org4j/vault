package uz.khakimjanovich.vault4j;

import uz.khakimjanovich.error4j.StatusThrowable;

public enum VaultExceptionStatus implements StatusThrowable {
  VAULT_TRANSIT_UNAVAILABLE,
  VAULT_TRANSIT_REFUSED;

  @Override
  public String getType() {
    return name();
  }
}
