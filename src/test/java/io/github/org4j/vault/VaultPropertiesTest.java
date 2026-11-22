package io.github.org4j.vault;

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
            "org4j.vault.signing-keys.path=clients/%s",
            "org4j.vault.signing-keys.field=key", "org4j.vault.signing-keys.cache-ttl=30s")
        .run(context -> assertThat(context).hasSingleBean(SigningKeys.class));
  }

  @Test
  void aPathWithoutItsFieldStopsStartup() {
    runner
        .withPropertyValues(
            "org4j.vault.signing-keys.path=clients/%s", "org4j.vault.signing-keys.cache-ttl=30s")
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
            "org4j.vault.signing-keys.path=clients/%s", "org4j.vault.signing-keys.field=key")
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
            "org4j.vault.signing-keys.path=clients/",
            "org4j.vault.signing-keys.field=key",
            "org4j.vault.signing-keys.cache-ttl=30s")
        .run(
            context ->
                assertThat(context)
                    .getFailure()
                    .rootCause()
                    .hasMessageContaining("path")
                    .hasMessageContaining("must contain %s"));
  }

  @Test
  void keepsTenThousandKeysUnlessToldOtherwise() {
    var keys =
        new String[] {
          "org4j.vault.signing-keys.path=clients/%s",
          "org4j.vault.signing-keys.field=key",
          "org4j.vault.signing-keys.cache-ttl=30s"
        };
    runner
        .withPropertyValues(keys)
        .run(
            context ->
                assertThat(context.getBean(VaultSigningKeysProperties.class).maxEntries())
                    .isEqualTo(10_000));
    runner
        .withPropertyValues(keys)
        .withPropertyValues("org4j.vault.signing-keys.max-entries=0")
        .run(
            context ->
                assertThat(context).getFailure().rootCause().hasMessageContaining("maxEntries"));
  }
}
