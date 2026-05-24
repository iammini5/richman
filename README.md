# Richman

## Project Planning

- [Play Purchase Sync Backend PRD and Design](PLAY_PURCHASE_SYNC_PRD_AND_DESIGN.md)

## Weekly Changes

This README is the home for weekly Richman project change notes.

### 2026-W21 Weekly Changes

Date range: May 18, 2026 to May 24, 2026

#### Summary

This week focused on preparing Richman for Google Play closed testing, adding Play Billing monetization, improving the purchase UI, and automating release uploads.

#### Android and Build

- Updated the Android app package to `com.legendsoftware.richman`.
- Updated Gradle and Android build configuration so the project builds with the supported Android Gradle Plugin version.
- Added release signing support using local properties and a local release key.
- Added Gradle Play Publisher configuration for Play Console uploads.
- Added Play service account credential handling while keeping credential JSON files out of Git.
- Uploaded release builds through the Play publishing API to the `alpha` closed testing track.

#### Google Play Billing

- Upgraded Google Play Billing Library to `9.0.0`.
- Added one-time product support for coin packs:
  - `com.legendsoftware.richman.coins.50`
  - `com.legendsoftware.richman.coins.100`
  - `com.legendsoftware.richman.coins.200`
  - `com.legendsoftware.richman.coins.500`
- Added a starter one-time product:
  - `com.legendsoftware.richman.bundle.starter`
- Added subscription products:
  - `premium_basic_monthly`
  - `premium_plus_monthly`
  - `premium_pro_monthly`
- Kept legacy `premium_monthly` support for existing testers.
- Implemented multi-product one-time purchase checkout for the starter bundle.
- Implemented subscription add-on checkout for Basic + Plus + Pro monthly subscriptions.
- Updated purchase fulfillment so every product returned by `Purchase.getProducts()` grants its entitlement.
- Prevented subscription purchases from being consumed while allowing one-time products to be repurchased.

#### Play Console Monetization

- Created and activated the starter bundle one-time product in Play Console.
- Created and activated Basic, Plus, and Pro monthly subscription products.
- Enabled merchandising and purchase recommendations for the active one-time products.
- Marked the one-time products as high priority for merchandising placements.
- Documented Play products and publishing flow in project markdown files.

#### User Interface

- Reduced oversized text across the main menu and purchase screens.
- Reworked the app style with a quieter dark surface, gold/green accents, smaller cards, and tighter spacing.
- Updated the More menu so it only shows bundle purchase options:
  - Starter multi-OTP bundle
  - Basic + Plus + Pro subscription add-on checkout
- Kept regular coin packs in the coin menu and regular subscriptions in the premium menu.

#### Project Operations

- Added `ROLES.md` for Product Manager, Engineer, and Tester responsibilities.
- Added Play publishing documentation.
- Added Play product setup documentation.
- Added Play listing image assets.
- Added this weekly change log space so future weekly changes can be committed with code.

#### Validation

- Ran debug builds after code and resource changes.
- Built signed release App Bundles.
- Uploaded accepted Play Alpha releases through Gradle Play Publisher.

#### Known Follow-Ups

- Capture fresh app screenshots from Android Studio or a local emulator outside the sandbox, because sandboxed `adb` cannot start here.
- Continue closed testing until Play production access requirements are satisfied.
- Add product icons in Play Console to improve one-time product merchandising conversion.
