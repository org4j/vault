package io.github.org4j.vault;

import io.github.org4j.error.ErrorDefinitions;
import org.junit.jupiter.api.Test;

class VaultExceptionStatusTest {

  @Test
  void everyErrorIsDefinedInEveryLanguage() {
    ErrorDefinitions.assertComplete(
        "io.github.org4j.vault", "classpath*:properties/vault-exception.yaml");
  }
}
