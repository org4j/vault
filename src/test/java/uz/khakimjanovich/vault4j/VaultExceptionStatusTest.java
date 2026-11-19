package uz.khakimjanovich.vault4j;

import org.junit.jupiter.api.Test;
import uz.khakimjanovich.error4j.ErrorDefinitions;

class VaultExceptionStatusTest {

  @Test
  void everyErrorIsDefinedInEveryLanguage() {
    ErrorDefinitions.assertComplete(
        "uz.khakimjanovich.vault4j", "classpath*:properties/vault-exception.yaml");
  }
}
