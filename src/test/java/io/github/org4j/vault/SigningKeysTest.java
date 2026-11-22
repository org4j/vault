package io.github.org4j.vault;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SigningKeysTest {

  private static final Duration TTL = Duration.ofSeconds(30);
  private static final byte[] KEY = "shop-1-signing-key".getBytes(StandardCharsets.UTF_8);

  private final VaultSecrets vault = mock(VaultSecrets.class);
  private final TestClock clock = new TestClock();
  private final SigningKeys keys = keys(10_000);

  private SigningKeys keys(int maxEntries) {
    return new SigningKeys(vault, "merchants/%s", "signingKey", TTL, clock, maxEntries);
  }

  private void vaultHas(byte[] key) {
    when(vault.get("merchants/shop-1"))
        .thenReturn(Optional.of(Map.of("signingKey", Base64.getEncoder().encodeToString(key))));
  }

  @Test
  void theKeyIsDecodedFromBase64() {
    vaultHas(KEY);
    assertThat(keys.find("shop-1")).contains(KEY);
  }

  @Test
  void aBurstOfRequestsReadsVaultOnce() {
    vaultHas(KEY);
    for (int i = 0; i < 100; i++) {
      assertThat(keys.find("shop-1")).contains(KEY);
    }
    verify(vault, times(1)).get("merchants/shop-1");
  }

  @Test
  void aKeyIsReadAgainAfterTheTtl() {
    vaultHas(KEY);
    keys.find("shop-1");
    clock.advance(TTL.plusSeconds(1));
    var rotated = "rotated-key".getBytes(StandardCharsets.UTF_8);
    vaultHas(rotated);
    assertThat(keys.find("shop-1")).contains(rotated);
    verify(vault, times(2)).get("merchants/shop-1");
  }

  @Test
  void aMissingKeyIsNeverRemembered() {
    when(vault.get("merchants/shop-1")).thenReturn(Optional.empty());
    assertThat(keys.find("shop-1")).isEmpty();
    assertThat(keys.find("shop-1")).isEmpty();
    verify(vault, times(2)).get("merchants/shop-1");
  }

  @Test
  void aVaultFailureIsNeverAnsweredWithAnOldKey() {
    vaultHas(KEY);
    keys.find("shop-1");
    clock.advance(TTL.plusSeconds(1));
    when(vault.get("merchants/shop-1"))
        .thenThrow(new VaultUnavailableException(new IllegalStateException("sealed")));
    assertThatThrownBy(() -> keys.find("shop-1")).isInstanceOf(VaultUnavailableException.class);
  }

  @Test
  void aCacheOfNoKeysIsRefused() {
    assertThatThrownBy(() -> keys(0)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void anIdThatIsAPathNeverReachesVault() {
    assertThat(keys.find("../demo")).isEmpty();
    assertThat(keys.find("..")).isEmpty();
    assertThat(keys.find("")).isEmpty();
    verifyNoInteractions(vault);
  }

  @Test
  void aBurstOfDistinctClientsKeepsAtMostMaxEntries() {
    var boundedKeys = keys(5);
    for (int i = 1; i <= 20; i++) {
      var id = "shop-" + i;
      var rawKey = ("key-" + i).getBytes(StandardCharsets.UTF_8);
      when(vault.get("merchants/" + id))
          .thenReturn(
              Optional.of(Map.of("signingKey", Base64.getEncoder().encodeToString(rawKey))));
      assertThat(boundedKeys.find(id)).contains(rawKey);
    }
    assertThat(boundedKeys.cacheSize()).isEqualTo(5);
  }

  @Test
  void theLeastRecentlyUsedKeyGoesFirst() {
    var boundedKeys = keys(3);
    for (int i = 1; i <= 3; i++) {
      var id = "shop-" + i;
      var rawKey = ("key-" + i).getBytes(StandardCharsets.UTF_8);
      when(vault.get("merchants/" + id))
          .thenReturn(
              Optional.of(Map.of("signingKey", Base64.getEncoder().encodeToString(rawKey))));
      boundedKeys.find(id);
    }
    assertThat(boundedKeys.cacheSize()).isEqualTo(3);

    boundedKeys.find("shop-1");
    var key4 = "key-4".getBytes(StandardCharsets.UTF_8);
    when(vault.get("merchants/shop-4"))
        .thenReturn(Optional.of(Map.of("signingKey", Base64.getEncoder().encodeToString(key4))));
    boundedKeys.find("shop-4");

    assertThat(boundedKeys.cacheSize()).isEqualTo(3);

    boundedKeys.find("shop-1");
    verify(vault, times(1)).get("merchants/shop-1");

    boundedKeys.find("shop-2");
    verify(vault, times(2)).get("merchants/shop-2");
  }
}
