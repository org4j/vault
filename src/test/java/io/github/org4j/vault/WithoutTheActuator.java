package io.github.org4j.vault;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;

/** An app started in a class loader that has no actuator, by {@link VaultPropertiesTest}. */
public final class WithoutTheActuator {

  @ImportAutoConfiguration(VaultAutoConfiguration.class)
  static class App {

    @Bean
    VaultTemplate vaultTemplate() {
      return new VaultTemplate(
          VaultEndpoint.create("localhost", 8200), new TokenAuthentication("t"));
    }
  }

  private WithoutTheActuator() {}

  /** Starts the app and names its {@link VaultSecrets} beans. */
  public static String start() {
    try (var context = new SpringApplicationBuilder(App.class).web(WebApplicationType.NONE).run()) {
      return String.join(",", context.getBeanNamesForType(VaultSecrets.class));
    }
  }
}
