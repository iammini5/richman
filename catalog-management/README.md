# Catalog Management

This folder keeps Google Play product catalog work repeatable for Richman.

## Files

- `sync_play_catalog.py` pulls the current Google Play catalog with the Play Developer API.
- `play-console/catalog.json` is the latest synced snapshot from Play Console.

## Sync Down

Run from the repo root:

```bash
python3 catalog-management/sync_play_catalog.py
```

The script uses the same service account configuration as Play publishing:

1. `PLAY_SERVICE_ACCOUNT_FILE` from `local.properties`, when set.
2. `service-account.json` in the repo root, as fallback.

It reads products for package `com.legendsoftware.richman` by default.

The snapshot uses the current Play catalog APIs:

- `oneTimeProducts` for coins and bundles.
- `subscriptions` for premium tiers.

The old `inappproducts` API may return `Please migrate to the new publishing API`; that is kept only as a diagnostic entry.

## Price Updates

Create a dry-run plan that changes every synced Play price to 10% of its current value:

```bash
python3 catalog-management/update_prices_to_ten_percent.py
```

Apply the plan through the Play Developer API:

```bash
python3 catalog-management/update_prices_to_ten_percent.py --apply --auth adc
```

The API caller must have Play Console permission to manage store presence / monetization catalog for `com.legendsoftware.richman`. Without that permission, Play returns `The caller does not have permission`.
