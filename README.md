# vault

Secrets from HashiCorp Vault (KV version 2) through Spring Cloud Vault, read only and failing
closed. Spring Cloud Vault handles sign-in (a token locally, AppRole or Kubernetes elsewhere),
token renewal, and Vault as the app's source of configuration (`spring.config.import`).

```groovy
implementation libs.vault
```

A service uses the four classes below and never `VaultTemplate` itself. A test that points one at a
container declares Spring Vault for itself (`integrationTestImplementation`).

```yaml
spring:
  config:
    import: ${VAULT_IMPORT} # vault://secret/<service>: a Vault that can't be read stops startup
  cloud:
    vault:
      uri: ${VAULT_URL}
      authentication: ${VAULT_AUTHENTICATION} # TOKEN, APPROLE, KUBERNETES
      token: ${VAULT_TOKEN} # locally only; none has a default
      fail-fast: ${VAULT_FAIL_FAST}
```

## What it gives

| Bean | When | Does |
|---|---|---|
| `VaultSecrets` | always | `get(path)`: the latest version of a secret; empty if there is none |
| `SigningKeys` | `signing-keys.path` set | `find(clientId)`: an API client's request-signing key, kept for `cache-ttl` so a burst of signed requests reads Vault once |
| `ClientRegistry` | `clients.path` set | `find(clientId)`, `all()`: an app's clients from one KV entry, one key per client id, each record passed on unread |
| `VaultTransit` | `transit.mount` set | seals values with keys that never leave Vault |

`SigningKeys` answers empty for an id that can't be a client id (`[A-Za-z0-9_-][A-Za-z0-9._-]*`,
never a path), and never keeps a missing key. `ClientRegistry` reads its entry once per `cache-ttl`,
so a client added or taken away in Vault counts without a restart. It holds an app's own per-client
data, never permissions: who may call what is granted in Keycloak. The library names no path,
field or key: they are the app's own.

### Transit

The module names the key and gives a context that says where the value lives
(`<table>.<column>:<row id>`). With a derived key (`derived=true`), a ciphertext copied to another
row or column does not open.

```java
var sealed = transit.encrypt(KEY, "item.secret:" + id, plain);
var plain = transit.decrypt(KEY, "item.secret:" + id, sealed);
var index = transit.hmac(INDEX_KEY, 1, value); // blind index: the same value, the same hash
```

| Step | Who | How |
|---|---|---|
| create the keys | infra | `vault secrets enable transit`, `vault write -f transit/keys/<key> derived=true` |
| move values after a rotation | the module's own job | every value not starting with `latestPrefix(KEY)` (`vault:v<n>:`) goes through `rewrap(KEY, context, sealed)`; the plaintext never leaves Vault |
| retire an old version | operator | once no value is left on it (`min_decryption_version`) |
| HMAC key version | the module | pinned: a rotation would change every index |

## Properties

| Property | Default | What |
|---|---|---|
| `org4j.vault.mount` | `secret` | the KV version 2 mount |
| `org4j.vault.signing-keys.path` | none | a client's key entry, `%s` its client id (required in the path); unset, there are no signing keys |
| `org4j.vault.signing-keys.field` | none, required with `path` | the entry's key holding the base64 signing key |
| `org4j.vault.signing-keys.cache-ttl` | none, required with `path` | how long a key read is used |
| `org4j.vault.signing-keys.max-entries` | `10000` | how many keys are kept at most; the least recently used goes first |
| `org4j.vault.clients.path` | none | the client registry's KV entry; unset, there is no registry |
| `org4j.vault.clients.cache-ttl` | none, required with `path` | how long one read of the registry is used |
| `org4j.vault.transit.mount` | none | the Transit engine's mount; unset, there is no `VaultTransit`; blank stops startup |

Where Vault is and how to sign in are Spring Cloud Vault's `spring.cloud.vault.*`.

## Errors

`VaultSecrets`, `SigningKeys` and `ClientRegistry` throw `VaultUnavailableException` when Vault
refuses or doesn't answer, never an old copy: callers fail closed.

`VaultTransit` throws `GeneralException`. Its warning names the exception and Vault's message,
masked by the app's `Masking`, with card numbers hidden (`first6……last4`) even when no rule names
them.

| Code | Status | When |
|---|---|---|
| `VAULT_TRANSIT_UNAVAILABLE` | 503 | Vault didn't answer, failed, or refused the app's token: a later call may work |
| `VAULT_TRANSIT_REFUSED` | 500 | Vault answered that the call can't work: a ciphertext that doesn't open with its context, a tampered one, a key that doesn't exist |

## Health

With the actuator, `vaultToken` is UP while Vault accepts the app's token (a lookup of itself).
Spring Cloud Vault's own `vault` checks that Vault is up and unsealed.

## Beans you can replace

`VaultSecrets`, `SigningKeys`, `ClientRegistry`, `VaultTransit`, and the `vaultTokenHealthIndicator`
bean.

## Tests

- `./gradlew test`: every Transit error defined in every language, failures sorted into refused and
  unavailable, and log masking. Also the signing-key cache (one read per TTL, misses, failures,
  least recently used first), ids that aren't client ids, the client registry's TTL, and the
  settings that stop startup.
- `./gradlew integrationTest`: a real Vault.
  - Sign-in: token and AppRole.
  - KV reads: the latest version, a missing path, a wrong token, a rotation after the TTL.
  - Vault as a configuration source.
  - Transit: a value opens only with its own context and untampered, rewrap after a rotation, a
    stable HMAC, a refused token (unavailable), a missing key (refused), a blank mount that stops
    startup, and no `VaultTransit` without a mount.
