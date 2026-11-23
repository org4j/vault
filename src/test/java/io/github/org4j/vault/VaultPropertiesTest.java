package io.github.org4j.vault;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.vault.core.VaultTemplate;

class VaultPropertiesTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(VaultAutoConfiguration.class))
          .withBean(VaultTemplate.class, () -> mock(VaultTemplate.class));

  @Test
  void withTheActuatorTheTokenHasAHealthIndicator() {
    runner.run(context -> assertThat(context).hasBean("vaultTokenHealthIndicator"));
  }

  @Test
  void anAppWithoutTheActuatorStarts() throws Exception {
    var urls =
        Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator, -1))
            .filter(entry -> !entry.contains("spring-boot-health"))
            .map(VaultPropertiesTest::url)
            .toArray(URL[]::new);
    try (var withoutActuator = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader())) {
      assertThat(
              withoutActuator.getResource(
                  HealthIndicator.class.getName().replace('.', '/') + ".class"))
          .isNull();
      var thread = Thread.currentThread();
      var original = thread.getContextClassLoader();
      thread.setContextClassLoader(withoutActuator);
      try {
        var started =
            withoutActuator
                .loadClass(WithoutTheActuator.class.getName())
                .getMethod("start")
                .invoke(null);
        assertThat(started).isEqualTo("vaultSecrets");
      } finally {
        thread.setContextClassLoader(original);
      }
    }
  }

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

  private static URL url(String classpathEntry) {
    try {
      return Path.of(classpathEntry).toUri().toURL();
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(classpathEntry, e);
    }
  }
}
