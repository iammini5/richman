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
