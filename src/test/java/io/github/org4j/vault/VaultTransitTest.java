package io.github.org4j.vault;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.org4j.error.GeneralException;
import io.github.org4j.masking.Masking;
import io.github.org4j.masking.MaskingRules;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.vault.VaultException;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.VaultTransitOperations;
import org.springframework.vault.support.Ciphertext;
import org.springframework.vault.support.Plaintext;
import org.springframework.vault.support.VaultHmacRequest;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

@ExtendWith(OutputCaptureExtension.class)
class VaultTransitTest {

  private VaultOperations vaultOperations;
  private VaultTransitOperations transitOperations;
  private VaultTransit vaultTransit;

  @BeforeEach
  void setUp() {
    vaultOperations = mock(VaultOperations.class);
    transitOperations = mock(VaultTransitOperations.class);
    when(vaultOperations.opsForTransit("transit")).thenReturn(transitOperations);
    vaultTransit = new VaultTransit(vaultOperations, "transit", Masking.empty());
  }

  @Test
  void sanitizesPanInWarningLogWhenVaultFails(CapturedOutput output) {
    when(transitOperations.encrypt(eq("card-key"), any(Plaintext.class)))
        .thenThrow(new VaultException("Error processing card 8600301234567890 at endpoint"));

    assertThatThrownBy(() -> vaultTransit.encrypt("card-key", "ctx", "raw-secret"))
        .isInstanceOfSatisfying(
            GeneralException.class,
            e ->
                assertThat(e.getStatus())
                    .isEqualTo(VaultExceptionStatus.VAULT_TRANSIT_UNAVAILABLE));

    assertThat(output.getOut()).contains("860030......7890").doesNotContain("8600301234567890");
  }

  @Test
  void sanitizesQualifiedContextAndSecretFieldInWarningLog(CapturedOutput output) {
    var masking = new Masking(MaskingRules.of(List.of("pan", "secret")));
    var transit = new VaultTransit(vaultOperations, "transit", masking);

    when(transitOperations.decrypt(eq("card-key"), any(Ciphertext.class)))
        .thenThrow(
            new VaultException(
                "Decrypt failed for context card.pan:8600301234567890 and field secret:supersecret"));

    assertThatThrownBy(
            () -> transit.decrypt("card-key", "card.pan:8600301234567890", "vault:v1:AAAA"))
        .isInstanceOf(GeneralException.class);

    assertThat(output.getOut())
        .contains("card.pan:860030......7890")
        .doesNotContain("8600301234567890")
        .contains("secret:supers.cret")
        .doesNotContain("supersecret");
  }

  @Test
  void sanitizesBlindIndexPanInWarningLog(CapturedOutput output) {
    when(transitOperations.getHmac(eq("index-key"), any(VaultHmacRequest.class)))
        .thenThrow(new VaultException("HMAC computation failed for pan=9860090101191238"));

    assertThatThrownBy(() -> vaultTransit.hmac("index-key", 1, "9860090101191238"))
        .isInstanceOf(GeneralException.class);

    assertThat(output.getOut()).contains("986009......1238").doesNotContain("9860090101191238");
  }

  @Test
  void a400Or404IsRefusedAndAnythingElseUnavailable() {
    assertThat(statusFor(clientError(HttpStatus.BAD_REQUEST)))
        .isEqualTo(VaultExceptionStatus.VAULT_TRANSIT_REFUSED);
    assertThat(statusFor(clientError(HttpStatus.NOT_FOUND)))
        .isEqualTo(VaultExceptionStatus.VAULT_TRANSIT_REFUSED);
    assertThat(statusFor(clientError(HttpStatus.INTERNAL_SERVER_ERROR)))
        .isEqualTo(VaultExceptionStatus.VAULT_TRANSIT_UNAVAILABLE);
    assertThat(statusFor(new RestClientException("network timeout")))
        .isEqualTo(VaultExceptionStatus.VAULT_TRANSIT_UNAVAILABLE);
  }

  @Test
  void aMissingKeyIsRefused() {
    when(transitOperations.getKey("gone")).thenReturn(null);
    assertThatThrownBy(() -> vaultTransit.latestPrefix("gone"))
        .isInstanceOfSatisfying(
            GeneralException.class,
            e -> assertThat(e.getStatus()).isEqualTo(VaultExceptionStatus.VAULT_TRANSIT_REFUSED));
  }

  private static VaultException clientError(HttpStatus status) {
    return new VaultException(
        status.getReasonPhrase(),
        HttpClientErrorException.create(status, status.getReasonPhrase(), null, null, null));
  }

  private Object statusFor(RuntimeException failure) {
    when(transitOperations.encrypt(eq("key"), any(Plaintext.class))).thenThrow(failure);
    var thrown =
        catchThrowableOfType(
            GeneralException.class, () -> vaultTransit.encrypt("key", "ctx", "val"));
    return thrown.getStatus();
  }

  @Test
  void aMissingMaskingIsRefused() {
    assertThatThrownBy(() -> new VaultTransit(vaultOperations, "transit", null))
        .isInstanceOf(NullPointerException.class);
  }
}
