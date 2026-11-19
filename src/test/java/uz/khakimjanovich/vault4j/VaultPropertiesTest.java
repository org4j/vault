package uz.khakimjanovich.vault4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.vault.core.VaultTemplate;

class VaultPropertiesTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(VaultAutoConfiguration.class))
          .withBean(VaultTemplate.class, () -> mock(VaultTemplate.class));

  @Test
  void withoutAPathThereAreNoSigningKeys() {
    runner.run(context -> assertThat(context).doesNotHaveBean(SigningKeys.class));
  }

  @Test
  void theAppsPathFieldAndCacheTimeStart() {
    runner
        .withPropertyValues(
            "vault4j.signing-keys.path=clients/%s",
            "vault4j.signing-keys.field=key", "vault4j.signing-keys.cache-ttl=30s")
        .run(context -> assertThat(context).hasSingleBean(SigningKeys.class));
  }

  @Test
  void aPathWithoutItsFieldStopsStartup() {
    runner
        .withPropertyValues(
            "vault4j.signing-keys.path=clients/%s",
            "vault4j.signing-keys.cache-ttl=30s")
        .run(
            context ->
                assertThat(context)
                    .getFailure()
                    .rootCause()
                    .hasMessage(VaultConstants.SIGNING_KEYS_FIELD_MISSING));
  }

  @Test
  void aPathWithoutItsCacheTimeStopsStartup() {
    runner
        .withPropertyValues(
            "vault4j.signing-keys.path=clients/%s", "vault4j.signing-keys.field=key")
        .run(
            context ->
                assertThat(context)
                    .getFailure()
                    .rootCause()
                    .hasMessage(VaultConstants.SIGNING_KEYS_CACHE_TTL_MISSING));
  }

  @Test
  void aPathWithoutTheClientIdStopsStartup() {
    runner
        .withPropertyValues(
            "vault4j.signing-keys.path=clients/",
            "vault4j.signing-keys.field=key",
            "vault4j.signing-keys.cache-ttl=30s")
        .run(
            context ->
                assertThat(context)
                    .getFailure()
                    .rootCause()
                    .hasMessageContaining("path")
                    .hasMessageContaining("must contain %s"));
  }
}
