/**
 * Secrets from HashiCorp Vault through Spring Cloud Vault, read only and failing closed: {@link
 * io.github.org4j.vault.VaultSecrets} reads a KV entry, {@link io.github.org4j.vault.SigningKeys}
 * and {@link io.github.org4j.vault.ClientRegistry} keep what an app reads per request a short time,
 * and {@link io.github.org4j.vault.VaultTransit} seals values with keys that never leave Vault.
 */
@NullMarked
package io.github.org4j.vault;

import org.jspecify.annotations.NullMarked;
