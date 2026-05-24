# Richman Backend

This module is the first backend implementation for Google Play purchase synchronization.

## What Is Included

- `POST /v1/play/purchases:sync` to accept Google Play purchase tokens from the Android app.
- `GET /v1/entitlements/me?userId=...` to return the current entitlement snapshot.
- `GET /health` for service health.
- Product catalog rules for the current Richman coin packs, starter bundle, and subscriptions.
- Idempotent purchase storage and entitlement grants.
- A pluggable `PlayPurchaseVerifier` interface so production Google Play API verification can replace the local verifier.
- An in-memory repository for the first runnable service and tests.

## Local Build

```sh
./gradlew :backend:assemble
```

## Local Run

The default verifier refuses to grant purchases unless local mock verification is explicitly enabled.

```sh
RICHMAN_ALLOW_UNVERIFIED_PLAY_PURCHASES=true PORT=8080 ./gradlew :backend:run
```

For local-only runs, an optional shutdown endpoint can be enabled:

```sh
RICHMAN_ALLOW_UNVERIFIED_PLAY_PURCHASES=true RICHMAN_ENABLE_LOCAL_SHUTDOWN=true PORT=8080 ./gradlew :backend:run
```

Then stop it with:

```sh
curl -X POST http://localhost:8080/internal/shutdown
```

## Example Purchase Sync

```sh
curl -X POST http://localhost:8080/v1/play/purchases:sync \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": "user-1",
    "packageName": "com.legendsoftware.richman",
    "purchaseType": "one_time",
    "productIds": ["com.legendsoftware.richman.coins.100"],
    "purchaseToken": "local-token-100",
    "clientPurchaseTime": "2026-05-24T00:00:00Z",
    "appVersion": "1.0"
  }'
```

Expected local response:

```json
{
  "status": "verified",
  "entitlements": {
    "coins": 100,
    "premiumTier": null,
    "premiumState": "none",
    "premiumExpiresAt": null
  }
}
```

## Production Gaps To Fill Next

- Replace `EnvironmentPlayPurchaseVerifier` with a Google Play Developer API verifier.
- Replace `InMemoryPurchaseRepository` with a durable database repository.
- Add app authentication for backend calls.
- Add Real-time Developer Notifications.
- Add Voided Purchases polling.
- Add deployment configuration and secret management.
