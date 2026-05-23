# Play / Android Monetization Daily Summary (Richman)

This log tracks **official** Google Play / Android developer monetization updates that could affect Richman (Play Billing, subscriptions, one‑time products, offers/pricing, Play Console monetization tools, and monetization‑relevant policy).

---

## 2026-05-18 (America/Los_Angeles)

### Key updates (official)

- **Play Billing Library: latest is `8.3.0` (2025-12-23)**  
  Adds client APIs/classes to support **external payments** flows (for eligible programs).  
  Source: [Google Play Billing Library release notes](https://developer.android.com/google/play/billing/release-notes?hl=en)

- **Google Play Developer API (Play Billing): updates through 2026-02-18**  
  - **2026-01-27:** `purchases.subscriptionsv2.defer` now supports **subscriptions with add-ons**; `OfferPhase` added to `SubscriptionPurchaseV2` to expose current offer phase (trial/intro/base/prorated).
  - **2025-06-30:** `ProductPurchaseV2` API introduced to support **multiple purchase options/offers for one-time products**.
  Source: [Google Play Developer API release notes](https://developer.android.com/google/play/billing/play-developer-apis-release-notes?hl=en)

- **One-time products guidance updated (2026-04-27)**  
  Official docs now emphasize **multiple purchase options and offers** for one-time products (e.g., discount offers; pre-order offers; buy vs rent options) and require `queryProductDetailsAsync()` + selecting an `offerToken` (via `getOneTimePurchaseOfferDetailsList()`).
  Sources: [One-time products](https://developer.android.com/google/play/billing/one-time-products?hl=en), [Multiple purchase options and offers](https://developer.android.com/google/play/billing/one-time-product-multi-purchase-options-offers)

- **US policy/program reminder (deadline passed): Dec 9, 2025 announcement**  
  Google clarified the Payments policy, expanded **alternative billing programs** for eligible developers serving **U.S. users**, and launched the **external content links program** for the U.S. (compliance date was **2026-01-28** for developers choosing to use those programs).
  Source: [Policy announcement: December 9, 2025](https://support.google.com/googleplay/android-developer/answer/16671517)

- **Play policy updates (Apr 15, 2026): not monetization-focused but relevant to store compliance**  
  New policies include **Account Transfer**; contacts permission policy; plus other clarifications/reminders.
  Sources: [Policy announcement: April 15, 2026](https://support.google.com/googleplay/android-developer/answer/16926792), [Google Play Policies timeline](https://developer.android.com/distribute/play-policies)

### Relevance to Richman

- Richman already pins **Play Billing `8.3.0`** (good; no urgent library upgrade indicated by the official release notes).
- If Richman is planning **discounting/experimentation** for coin packs or other one-time products, the newer **multiple-offer** model matters (eligibility, offer token selection, and UI to choose between offers).
- If Richman adds **subscriptions** (VIP pass, season pass, etc.) and uses a backend for entitlement/CS tooling, the newer `OfferPhase` + improved deferral support are helpful for analytics and support workflows.
- If Richman ever considers **linking out** to purchase digital goods or offering **alternative billing** (U.S. or other regions), the Dec 2025 policy/program expansion is the governing starting point.

### Recommended actions

1. **Confirm product strategy:** Decide whether Richman will stay Play Billing–only (simplest) vs. pursuing any external/alternative billing programs (adds policy + UX + reporting complexity).
2. **One-time products:** If planning multiple offers (regional promos, targeted discounts), ensure the purchase UI/logic selects the correct **`offerToken`** and handles `NO_ELIGIBLE_OFFER` outcomes.
3. **Subscriptions (if applicable):** Plan to surface offer phase (trial/intro/base) in internal tooling and analytics using `SubscriptionPurchaseV2.OfferPhase` via backend verification.
4. **Play Console hygiene:** Review April 2026 policy announcement items for any accidental scope hits (permissions/forms) that could affect publishing and monetization continuity.

---

## 2026-05-19 (America/Los_Angeles)

### Key updates (official)

- **No new monetization-specific updates found since 2026-05-18**  
  No new Play Billing Library releases (latest remains `8.3.0`, dated 2025-12-23) and no new Google Play Developer API (Play Billing) release notes beyond the most recent updates already captured.
  Sources: [Google Play Billing Library release notes](https://developer.android.com/google/play/billing/release-notes?hl=en), [Google Play Developer API release notes](https://developer.android.com/google/play/billing/play-developer-apis-release-notes?hl=en)

- **Upcoming policy timeline item (affects publishing continuity, indirectly monetization): Account Transfer (May 27, 2026)**  
  Google Play policy timeline now calls out an **Account Transfer** policy requiring use of the official **“Transfer ownership”** workflow in Play Console for account ownership changes.
  Sources: [Google Play Policies timeline](https://developer.android.com/distribute/play-policies), [Android Developers Blog: Apr 15, 2026 policy updates](https://developer.android.com/blog/posts/boosting-user-privacy-and-business-protection-with-updated-play-policies)

### Relevance to Richman

- No immediate billing code/product catalog changes are implied today.
- If we ever need to **move the app** between developer accounts (e.g., company restructuring), we should assume Play Console’s official transfer workflow is the only supported path after **2026-05-27**.

### Recommended actions

1. **No action required** for Play Billing today; continue monitoring for a new Billing Library release.
2. **Document account ownership** (who controls Play Console + payment profiles) and avoid ad-hoc “transfer” plans; use the official Play Console transfer process if ever needed.

---

## 2026-05-20 (America/Los_Angeles)

### Key updates (official)

- **Play Billing Library: `9.0.0` released (2026-05-19)**  
  Highlights called out in the official release notes include:
  - **In-app message for opt-in subscription price increases:** you can show an in-app message that lets users **confirm a pending opt-in price increase without leaving the app** (shown from the first day the user can accept; at most once every 7 days).  
  - **Developer-provided billing details nullability change:** `DeveloperProvidedBillingDetails.getLinkUri()` is now `@Nullable` to support cases where an external-payments deep link isn’t available at payment selection time.  
  - **External offers API cleanup:** `BillingClient.Builder.enableExternalOffer` is deprecated (and related older external-offer APIs were previously deprecated).  
  Sources: [Google Play Billing Library release notes](https://developer.android.com/google/play/billing/release-notes), [Google Play Billing Library release notes (ES-419)](https://developer.android.com/google/play/billing/release-notes?hl=es-419)

- **Google I/O 2026: Play monetization + Play Console changes announced (2026-05-19)**  
  Monetization-relevant announcements include:
  - “Coming soon” **in-app subscription management API** for flexible retention flows (change plan / accept downgrade offer at cancel time), plus tooling that automates prorated refunds via replacement modes.
  - **Extended recovery periods** (default account recovery period reportedly moving **30 → 60 days**) to reduce involuntary churn.
  - Play Console roadmap items that could affect monetization ops: **agentic catalog management** for one-time products (bulk price changes, SKU import, metadata) and new monetization reporting/insights (e.g., **cart conversion rates**, churn reasons, interactive Q&A and proactive insights).
  Source: [I/O 2026: What’s new in Google Play](https://developer.android.com/blog/posts/i-o-2026-what-s-new-in-google-play)

- **Play policy timeline unchanged from yesterday; next deadline: 2026-05-27 (Account Transfer)**  
  Source: [Google Play Policies timeline](https://developer.android.com/distribute/play-policies)

### Relevance to Richman

- Richman currently pins **Play Billing `8.3.0`**; **`9.0.0` is now available**. If we ship subscriptions (now or later), the **in-app price-increase confirmation** could reduce churn and support load during price adjustments.
- If Richman ever uses **external payments / external offers programs**, `9.0.0` contains API changes/cleanup that may require code updates and more defensive null-handling.
- The I/O announcements suggest **Play Console monetization workflows are becoming more “bulk/agentic”**; this matters if Richman’s IAP catalog expands (multiple coin packs, regional pricing, promos).

### Recommended actions

1. **Engineering: evaluate PBL upgrade to `9.0.0`** (review migration guide; verify any new dependency minimums such as `androidx.core` 1.9+ per release notes; run purchase flow regression).
2. **PM: record a “future capability” note** to revisit once Richman has subscriptions: leverage the upcoming **in-app subscription management API** (downgrade offers at cancel) and consider where it fits in retention strategy.
3. **Ops: prep for richer Console insights** by defining a baseline dashboard checklist (cart conversion, churn reasons, purchase funnel) so we can quickly adopt the new metrics when they land.

---

## 2026-05-21 (America/Los_Angeles)

### Key updates (official)

- **No new Play Billing Library release since `9.0.0` (2026-05-19)**  
  The latest remains `9.0.0`; today’s “new” items are additional official docs worth actioning:
  - **Migration guide for PBL v9** (updated 2026-05-19 UTC) to support upgrading from v7/v8 → v9.
  - Release-note details to ensure we handle edge cases correctly (e.g., blocked Play Store activity now yields `BILLING_UNAVAILABLE` instead of `ERROR`; `androidx.core` 1.9+ requirement for the in-app price-increase confirmation message).  
  Sources: [PBL release notes](https://developer.android.com/google/play/billing/release-notes), [Migrate to PBL v9](https://developer.android.com/google/play/billing/migrate-gpblv9)

- **Google Play Developer API: release notes updated (2026-05-19)**  
  For subscriptions, `SubscriptionPurchaseV2` now exposes additional context fields when a subscription enters `ON_HOLD` or `IN_GRACE_PERIOD` due to a declined renewal payment, including pending/failed order IDs. Release notes also point to **May 2026 subscription API deprecations** and the deprecation timeline doc (last updated 2026-04-27 UTC).  
  Sources: [Google Play Developer API release notes](https://developer.android.com/google/play/billing/play-developer-apis-release-notes?hl=en), [Developer API deprecations](https://developer.android.com/google/play/billing/play-developer-apis-deprecations)

- **Play policy timeline unchanged; next deadline remains 2026-05-27 (Account Transfer)**  
  Source: [Google Play Policies timeline](https://developer.android.com/distribute/play-policies)

### Relevance to Richman

- If Richman adds **server-side subscription management** (grace/on-hold handling, dunning), the new `SubscriptionPurchaseV2` context fields can improve diagnostics and customer support workflows.
- If/when we upgrade to **PBL `9.0.0`**, the migration guide + edge-case response-code behavior (`BILLING_UNAVAILABLE` when Play Store is blocked) should be reflected in our purchase-flow error handling to avoid misclassifying incidents.

### Recommended actions

1. **Engineering:** when upgrading to PBL `9.0.0`, add a test case for `BILLING_UNAVAILABLE` + “Play Store is blocked” and confirm our UI messaging is user-friendly.
2. **PM/Ops:** if we plan subscriptions soon, note the new `SubscriptionPurchaseV2` context fields as a “supportability win” for on-hold/grace cases (CS scripts + dashboards).

---

## 2026-05-22 (America/Los_Angeles)

### Key updates (official)

- **No meaningful monetization updates found since 2026-05-21**  
  Checked for new items across: Play Billing Library release notes (latest remains `9.0.0`, 2026-05-19), Google Play Developer API release notes (latest remains 2026-05-19), and recent Google Play announcements.  
  Sources: [Google Play Billing Library release notes](https://developer.android.com/google/play/billing/release-notes?hl=en), [Google Play Developer API release notes](https://developer.android.com/google/play/billing/play-developer-apis-release-notes?hl=en), [I/O 2026: What’s new in Google Play](https://developer.android.com/blog/posts/i-o-2026-what-s-new-in-google-play)

### Relevance to Richman

- No new compliance or feature changes that alter Richman’s current monetization plan today.

### Recommended actions

1. Continue planned evaluation/upgrade work for **PBL `9.0.0`** and keep monitoring for new release notes or policy deadlines.

---

## 2026-05-23 (America/Los_Angeles)

### Key updates (official)

- **No new Play Billing / subscriptions monetization release notes since 2026-05-22**  
  - Play Billing Library: latest remains `9.0.0` (2026-05-19).  
  - Google Play Developer API (Play Billing): latest release-notes entry remains 2026-05-19.  
  Sources: [Google Play Billing Library release notes](https://developer.android.com/google/play/billing/release-notes?hl=en), [Google Play Developer API release notes](https://developer.android.com/google/play/billing/play-developer-apis-release-notes?hl=en)

- **(New-to-this-log) Play Console “Protected with Play” dashboard called out as covering monetization defenses**  
  Google’s I/O 2026 Play update highlights a new **Protected with Play** dashboard to monitor/configure integrity, distribution, and **monetization defenses** in one place.  
  Source: [I/O 2026: What’s new in Google Play](https://developer.android.com/blog/posts/i-o-2026-what-s-new-in-google-play)

- **Upcoming policy deadline unchanged: Account Transfer (2026-05-27)**  
  Source: [Google Play Policies timeline](https://developer.android.com/distribute/play-policies)

### Relevance to Richman

- No immediate billing-code change required today.
- The **Protected with Play** dashboard is relevant to Richman’s revenue protection (fraud/abuse monitoring) once it appears in our Play Console account.

### Recommended actions

1. **PM/Ops:** In Play Console, look for **Protected with Play** and document what controls/alerts are available for monetization defense; add it to the weekly release/readiness checklist.
2. **PM:** Ensure any ownership/organization changes (if planned) use the official **Transfer ownership** workflow before **2026-05-27** to avoid publishing disruptions.
