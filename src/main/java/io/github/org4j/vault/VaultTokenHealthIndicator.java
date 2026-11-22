package io.github.org4j.vault;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.vault.VaultException;
import org.springframework.vault.core.VaultOperations;
import org.springframework.web.client.RestClientException;

/**
 * UP while Vault accepts the app's token. Down names only the exception's class: Vault's message
 * may quote the token.
 */
class VaultTokenHealthIndicator implements HealthIndicator {

  private final VaultOperations vault;

  VaultTokenHealthIndicator(VaultOperations vault) {
    this.vault = vault;
  }

  @Override
  public Health health() {
    try {
      return vault.read(VaultConstants.TOKEN_LOOKUP_SELF) == null
          ? Health.down()
              .withDetail(VaultConstants.HEALTH_ERROR_DETAIL, VaultConstants.HEALTH_EMPTY_ANSWER)
              .build()
          : Health.up().build();
    } catch (VaultException | RestClientException e) {
      return Health.down()
          .withDetail(VaultConstants.HEALTH_ERROR_DETAIL, e.getClass().getSimpleName())
          .build();
    }
  }
}
