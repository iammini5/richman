# Catalog Management

This folder keeps Google Play product catalog work repeatable for Richman.

## Files

- `sync_play_catalog.py` pulls the current Google Play catalog with the Play Developer API.
- `create_premium_subscription_plans.py` creates or repairs the Basic Premium same-product-ID subscription, then deactivates the unused Plus/Pro same-product-ID base plans.
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

## Premium Subscription Setup

Create or repair the Basic Premium subscription product and deactivate unused Plus/Pro same-product-ID base plans:

```bash
python3 catalog-management/create_premium_subscription_plans.py --apply
```

The active same-product-ID subscription is `premium_basic`, with `monthly` and `yearly` auto-renewing base plans. The monthly-only products `premium_plus_monthly` and `premium_pro_monthly` remain active separately in Play Console.

## Price Updates

### Half-price changes from a live baseline

`inspect_current_catalog.py` reads the full paginated product, base-plan, and offer catalog using existing Application Default Credentials. It writes a new audit snapshot and refuses to overwrite one. Capture the baseline with:

```bash
python3 catalog-management/inspect_current_catalog.py --output current-catalog-before-half-price.json
python3 catalog-management/half_price_catalog.py
```

The half-price tool scales each original regional amount independently, preserves currencies and availability, and records exact and rounded targets. It does not use exchange-rate conversion or silently clamp platform minimums. Review the plan before using `--apply-all`. Readback verifies every returned price field. Re-running an apply uses the same baseline targets, not another reduction of the already changed live prices.

`--apply-pending-uae-minimum` is the explicitly approved exception for the October 2, 2026 change: it targets only 50 coins and `premium_basic_monthly`, setting their UAE amounts to AED 0.30 and retaining half-price targets elsewhere. It does not authorize other minimum exceptions; Play may reject another region. Existing subscriber migrations are separate operations and must be reviewed against legacy cohort amounts to avoid increases.

Audit snapshots and API results should remain local. No tokens, service-account keys, or signing secrets belong in Git.

### Historical ten-percent workflow

Create a dry-run plan that changes every synced Play price to 10% of its current value:

```bash
python3 catalog-management/update_prices_to_ten_percent.py
```

Apply the plan through the Play Developer API:

```bash
python3 catalog-management/update_prices_to_ten_percent.py --apply --auth adc
```

The API caller must have Play Console permission to manage store presence / monetization catalog for `com.legendsoftware.richman`. Without that permission, Play returns `The caller does not have permission`.

## Permission Grant

Grant the catalog permissions to the Play API service account:

```bash
python3 catalog-management/grant_play_catalog_permission.py --developer-id 9087387577453167492
```

This uses the official Play Developer API `grants.create` endpoint. The signed-in Google user must already have permission to manage Play Console users/permissions. If Play returns `You do not have permission to access this object`, the account owner/admin must perform the grant.

If the browser auto-selects the wrong Google account during API login, use the account-specific OAuth opener:

```bash
BROWSER=catalog-management/open_iammini5_oauth.py \
  gcloud auth application-default login iammini5@gmail.com \
  --scopes=openid,https://www.googleapis.com/auth/userinfo.email,https://www.googleapis.com/auth/androidpublisher,https://www.googleapis.com/auth/cloud-platform
```
