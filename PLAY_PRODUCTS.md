# Play Product Setup

The app queries these product IDs from Google Play Billing. They must exist in Play Console before they appear in the app.

## One-time products

| Product ID | App label | Entitlement |
| --- | --- | --- |
| `com.legendsoftware.richman.coins.50` | 50 Coins | 50 coins for quick boosts and small actions |
| `com.legendsoftware.richman.coins.100` | 100 Coins | 100 coins for extra turns and steady progress |
| `com.legendsoftware.richman.coins.200` | 200 Coins | 200 coins for longer sessions and bigger moves |
| `com.legendsoftware.richman.coins.500` | 500 Coins | 500 coins for power play and maximum flexibility |
| `com.legendsoftware.richman.bundle.starter` | Starter Bundle | 150 coins plus a guided path to add any premium tier |

## Multi-product OTP bundle

The More menu starts a single Play checkout for these one-time products when the user taps Starter Bundle:

| Product ID | Coins granted |
| --- | --- |
| `com.legendsoftware.richman.bundle.starter` | 150 |
| `com.legendsoftware.richman.coins.100` | 100 |
| `com.legendsoftware.richman.coins.200` | 200 |

The app grants every product returned by `Purchase.getProducts()` so multi-product purchases deliver all included entitlements.

## Subscriptions

These are separate subscription products so a user can subscribe to more than one tier at the same time.

| Product ID | App label | Intended price |
| --- | --- | --- |
| `premium_basic_monthly` | Basic Monthly | USD 1/month |
| `premium_plus_monthly` | Plus Monthly | USD 2/month |
| `premium_pro_monthly` | Pro Monthly | USD 3/month |

## Subscription add-on bundle

The Premium and More screens include a single checkout button for subscription with add-ons:

| Product ID | Role |
| --- | --- |
| `premium_basic_monthly` | Base subscription |
| `premium_plus_monthly` | Add-on subscription |
| `premium_pro_monthly` | Add-on subscription |

All three products use monthly auto-renewing base plans so Google Play can bill and manage them together as subscription add-ons.

Keep `premium_monthly` active only if existing testers still need the old subscription product.

## Next Subscription Plan Design

Product decision: use one subscription product per premium tier, with monthly and yearly base plans inside the same product ID.

This is the preferred model for the next Richman subscription setup because monthly and yearly options should be alternate billing periods for the same entitlement, not separate entitlements that a player can accidentally stack.

### New subscription product IDs

| Product ID | Tier | Entitlement | Monthly base plan ID | Yearly base plan ID |
| --- | --- | --- | --- | --- |
| `premium_basic` | Basic | Basic premium tools | `monthly` | `yearly` |
| `premium_plus` | Plus | Plus boosters and richer play | `monthly` | `yearly` |
| `premium_pro` | Pro | Pro access and top-tier perks | `monthly` | `yearly` |

### Intended pricing

| Product ID | Monthly price | Yearly price | Yearly positioning |
| --- | --- | --- | --- |
| `premium_basic` | USD 0.99/month | USD 9.99/year | About 2 months free |
| `premium_plus` | USD 1.99/month | USD 19.99/year | About 2 months free |
| `premium_pro` | USD 2.99/month | USD 29.99/year | About 2 months free |

### Product Manager requirements

- Show monthly and yearly options together for each tier.
- Position yearly as the better-value plan.
- Do not allow monthly and yearly for the same tier to behave like separate add-on entitlements.
- Backend entitlement should resolve the active premium tier from `premium_basic`, `premium_plus`, or `premium_pro`.
- Keep `premium_basic_monthly`, `premium_plus_monthly`, `premium_pro_monthly`, and `premium_monthly` active only for migration or existing tester continuity until the new products are validated.

### Play Console setup

Create these as new subscription products:

- `premium_basic`
- `premium_plus`
- `premium_pro`

For each product, create two auto-renewing base plans:

- Monthly base plan: `monthly`
- Yearly base plan: `yearly`

After Play Console setup, Engineer should update the app and backend product catalog to query the new product IDs and select the correct base plan offer token for monthly or yearly checkout.

### User flow: yearly to monthly switch

Goal: let a player move from yearly billing to monthly billing for the same premium tier without losing already-paid yearly time.

Flow:

1. Player opens Premium Subscriptions.
2. App shows each tier with monthly and yearly choices from the same product ID.
3. Player taps the monthly option for the tier they already have yearly.
4. App checks for an active subscription purchase with the same product ID.
5. App launches Google Play Billing with the monthly base plan offer token.
6. If an active purchase token exists, app passes it as the old purchase token and uses no-proration replacement behavior so the monthly plan is charged on the next billing date.
7. Backend verifies the new purchase token through the normal subscription sync path.
8. Product Manager treats this as a billing cadence switch, not a tier upgrade.

Expected user-facing copy:

```text
Switch to Monthly
Your yearly access stays active until the current billing period ends. Monthly billing starts on the next renewal date.
```
