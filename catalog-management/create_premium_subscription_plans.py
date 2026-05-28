#!/usr/bin/env python3
import argparse
import copy
import json
import subprocess
import time
import urllib.error
import urllib.parse
from pathlib import Path
from typing import Optional

import sync_play_catalog


ROOT = Path(__file__).resolve().parents[1]
PLAN = ROOT / "catalog-management" / "play-console" / "premium-subscription-plan.json"
RESULT = ROOT / "catalog-management" / "play-console" / "premium-subscription-result.json"
PACKAGE_NAME = "com.legendsoftware.richman"
REGIONS_VERSION = "2025/03"
DEACTIVATE_SAME_PRODUCT_ID_PRODUCTS = ("premium_plus", "premium_pro")

PRODUCTS = [
    {
        "productId": "premium_basic",
        "title": "Basic Premium",
        "description": "Basic premium tools.",
        "monthlyUsd": {"currencyCode": "USD", "nanos": 990000000},
        "yearlyUsd": {"currencyCode": "USD", "units": "9", "nanos": 990000000},
    },
]


def write_json(path: Path, value: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n")


def api_error(exc: urllib.error.HTTPError) -> dict:
    return {
        "status": exc.code,
        "reason": exc.reason,
        "body": exc.read().decode("utf-8", errors="replace"),
    }


def token_for(auth: str) -> str:
    if auth == "gcloud":
        return subprocess.check_output(
            ["gcloud", "auth", "print-access-token"],
            text=True,
        ).strip()
    if auth == "adc":
        return subprocess.check_output(
            ["gcloud", "auth", "application-default", "print-access-token"],
            text=True,
        ).strip()

    service_account = json.loads(sync_play_catalog.service_account_path().read_text())
    return sync_play_catalog.access_token(service_account)


def convert_region_prices(token: str, price: dict) -> dict:
    package = urllib.parse.quote(PACKAGE_NAME, safe="")
    return sync_play_catalog.post_json(
        token,
        f"/applications/{package}/pricing:convertRegionPrices",
        {"price": price},
    )


def regional_configs(converted: dict) -> list[dict]:
    prices = converted.get("convertedRegionPrices", {})
    return [
        {
            "regionCode": region_code,
            "newSubscriberAvailability": True,
            "price": value["price"],
        }
        for region_code, value in sorted(prices.items())
        if isinstance(value, dict) and "price" in value
    ]


def other_regions_config(converted: dict, fallback_usd: dict) -> dict:
    other_regions = converted.get("convertedOtherRegionsPrice", {})
    return {
        "newSubscriberAvailability": True,
        "usdPrice": other_regions.get("usdPrice", fallback_usd),
        "eurPrice": other_regions.get("eurPrice", {"currencyCode": "EUR", "units": "0", "nanos": 990000000}),
    }


def base_plan(token: Optional[str], base_plan_id: str, billing_period: str, usd_price: dict) -> dict:
    if token:
        converted = convert_region_prices(token, usd_price)
        regions = regional_configs(converted)
        other_regions = other_regions_config(converted, usd_price)
    else:
        regions = [
            {
                "regionCode": "US",
                "newSubscriberAvailability": True,
                "price": usd_price,
            }
        ]
        other_regions = {
            "newSubscriberAvailability": True,
            "usdPrice": usd_price,
            "eurPrice": {"currencyCode": "EUR", "units": "0", "nanos": 990000000},
        }

    return {
        "basePlanId": base_plan_id,
        "regionalConfigs": regions,
        "otherRegionsConfig": other_regions,
        "autoRenewingBasePlanType": {
            "billingPeriodDuration": billing_period,
            "gracePeriodDuration": "P7D",
            "resubscribeState": "RESUBSCRIBE_STATE_ACTIVE",
            "prorationMode": "SUBSCRIPTION_PRORATION_MODE_CHARGE_ON_NEXT_BILLING_DATE",
        },
    }


def subscription_body(product: dict, token: Optional[str]) -> dict:
    return {
        "packageName": PACKAGE_NAME,
        "productId": product["productId"],
        "basePlans": [
            base_plan(token, "monthly", "P1M", product["monthlyUsd"]),
            base_plan(token, "yearly", "P1Y", product["yearlyUsd"]),
        ],
        "listings": [
            {
                "languageCode": "en-US",
                "title": product["title"],
                "description": product["description"],
            }
        ],
    }


def get_subscription(token: str, product_id: str) -> Optional[dict]:
    package = urllib.parse.quote(PACKAGE_NAME, safe="")
    quoted_product = urllib.parse.quote(product_id, safe="")
    try:
        return sync_play_catalog.get_json(
            token,
            f"/applications/{package}/subscriptions/{quoted_product}",
        )
    except urllib.error.HTTPError as exc:
        if exc.code == 404:
            return None
        raise


def create_subscription(token: str, subscription: dict) -> dict:
    package = urllib.parse.quote(PACKAGE_NAME, safe="")
    query = urllib.parse.urlencode(
        {
            "productId": subscription["productId"],
            "regionsVersion.version": REGIONS_VERSION,
        }
    )
    return sync_play_catalog.post_json(
        token,
        f"/applications/{package}/subscriptions?{query}",
        subscription,
    )


def patch_subscription_base_plans(token: str, subscription: dict) -> dict:
    package = urllib.parse.quote(PACKAGE_NAME, safe="")
    quoted_product = urllib.parse.quote(subscription["productId"], safe="")
    query = urllib.parse.urlencode(
        {
            "updateMask": "basePlans",
            "regionsVersion.version": REGIONS_VERSION,
            "latencyTolerance": "PRODUCT_UPDATE_LATENCY_TOLERANCE_LATENCY_TOLERANT",
        }
    )
    return sync_play_catalog.patch_json(
        token,
        f"/applications/{package}/subscriptions/{quoted_product}?{query}",
        subscription,
    )


def activate_base_plan(token: str, product_id: str, base_plan_id: str) -> dict:
    package = urllib.parse.quote(PACKAGE_NAME, safe="")
    quoted_product = urllib.parse.quote(product_id, safe="")
    quoted_base_plan = urllib.parse.quote(base_plan_id, safe="")
    body = {
        "latencyTolerance": "PRODUCT_UPDATE_LATENCY_TOLERANCE_LATENCY_TOLERANT",
    }
    return sync_play_catalog.post_json(
        token,
        f"/applications/{package}/subscriptions/{quoted_product}/basePlans/{quoted_base_plan}:activate",
        body,
    )


def deactivate_base_plan(token: str, product_id: str, base_plan_id: str) -> dict:
    package = urllib.parse.quote(PACKAGE_NAME, safe="")
    quoted_product = urllib.parse.quote(product_id, safe="")
    quoted_base_plan = urllib.parse.quote(base_plan_id, safe="")
    body = {
        "latencyTolerance": "PRODUCT_UPDATE_LATENCY_TOLERANCE_LATENCY_TOLERANT",
    }
    return sync_play_catalog.post_json(
        token,
        f"/applications/{package}/subscriptions/{quoted_product}/basePlans/{quoted_base_plan}:deactivate",
        body,
    )


def active_base_plan_ids(subscription: dict) -> set[str]:
    return {
        base_plan.get("basePlanId", "")
        for base_plan in subscription.get("basePlans", [])
        if base_plan.get("state") == "ACTIVE"
    }


def base_plan_ids(subscription: dict) -> set[str]:
    return {base_plan.get("basePlanId", "") for base_plan in subscription.get("basePlans", [])}


def ensure_monthly_legacy_compatible(subscription: dict) -> tuple[dict, bool]:
    updated = copy.deepcopy(subscription)
    changed = False
    for base_plan in updated.get("basePlans", []):
        auto_renewing = base_plan.get("autoRenewingBasePlanType")
        if not auto_renewing:
            continue
        if base_plan.get("basePlanId") == "monthly" and auto_renewing.get("legacyCompatible") is not True:
            auto_renewing["legacyCompatible"] = True
            changed = True
        if base_plan.get("basePlanId") != "monthly" and auto_renewing.get("legacyCompatible") is True:
            auto_renewing["legacyCompatible"] = False
            changed = True
    return updated, changed


def main() -> None:
    parser = argparse.ArgumentParser(description="Configure Richman's Basic Premium subscription product.")
    parser.add_argument("--apply", action="store_true", help="Apply the product catalog changes in Play Console.")
    parser.add_argument(
        "--auth",
        choices=("service-account", "gcloud", "adc"),
        default="service-account",
        help="API credential source to use when applying changes.",
    )
    args = parser.parse_args()

    token = token_for(args.auth) if args.apply else None
    planned = [subscription_body(product, token) for product in PRODUCTS]
    plan = {
        "packageName": PACKAGE_NAME,
        "createdAtEpochSeconds": int(time.time()),
        "mode": "apply" if args.apply else "dry-run",
        "regionsVersion": REGIONS_VERSION,
        "subscriptions": planned,
        "deactivateSameProductIdProducts": list(DEACTIVATE_SAME_PRODUCT_ID_PRODUCTS),
    }
    write_json(PLAN, plan)

    if not args.apply:
        print(f"Wrote dry-run plan to {PLAN.relative_to(ROOT)}")
        print("Planned Basic Premium monthly/yearly setup and Plus/Pro same-product-ID deactivation.")
        return

    results = {
        "packageName": PACKAGE_NAME,
        "appliedAtEpochSeconds": int(time.time()),
        "subscriptions": [],
    }

    for subscription in planned:
        product_id = subscription["productId"]
        entry = {"productId": product_id, "basePlans": []}
        try:
            existing = get_subscription(token, product_id)
            if existing:
                entry["status"] = "already_exists"
                subscription_after_create = existing
            else:
                monthly_only_subscription = copy.deepcopy(subscription)
                monthly_only_subscription["basePlans"] = [
                    base_plan
                    for base_plan in monthly_only_subscription["basePlans"]
                    if base_plan.get("basePlanId") == "monthly"
                ]
                subscription_after_create = create_subscription(token, monthly_only_subscription)
                entry["status"] = "created"

            if "yearly" not in base_plan_ids(subscription_after_create):
                subscription_after_create = patch_subscription_base_plans(token, copy.deepcopy(subscription))
                entry["yearlyBasePlan"] = "added"

            legacy_subscription, legacy_changed = ensure_monthly_legacy_compatible(subscription_after_create)
            if legacy_changed:
                subscription_after_create = patch_subscription_base_plans(token, legacy_subscription)
                entry["monthlyLegacyCompatible"] = "updated"

            active_plans = active_base_plan_ids(subscription_after_create)
            for base_plan_id in ("monthly", "yearly"):
                if base_plan_id in active_plans:
                    entry["basePlans"].append({"basePlanId": base_plan_id, "status": "already_active"})
                    continue
                try:
                    response = activate_base_plan(token, product_id, base_plan_id)
                    state = [
                        plan.get("state")
                        for plan in response.get("basePlans", [])
                        if plan.get("basePlanId") == base_plan_id
                    ]
                    entry["basePlans"].append({"basePlanId": base_plan_id, "status": "activated", "responseState": state})
                except urllib.error.HTTPError as exc:
                    entry["basePlans"].append({"basePlanId": base_plan_id, "status": "error", "error": api_error(exc)})
        except urllib.error.HTTPError as exc:
            entry["status"] = "error"
            entry["error"] = api_error(exc)
        results["subscriptions"].append(entry)
        print(f"{product_id}: {entry['status']}")

    for product_id in DEACTIVATE_SAME_PRODUCT_ID_PRODUCTS:
        entry = {"productId": product_id, "status": "not_found", "basePlans": []}
        try:
            existing = get_subscription(token, product_id)
            if existing:
                entry["status"] = "found"
                for base_plan_id in active_base_plan_ids(existing):
                    try:
                        deactivate_base_plan(token, product_id, base_plan_id)
                        entry["basePlans"].append({"basePlanId": base_plan_id, "status": "deactivated"})
                    except urllib.error.HTTPError as exc:
                        entry["basePlans"].append({"basePlanId": base_plan_id, "status": "error", "error": api_error(exc)})
        except urllib.error.HTTPError as exc:
            entry["status"] = "error"
            entry["error"] = api_error(exc)
        results["subscriptions"].append(entry)
        print(f"{product_id}: {entry['status']}")

    write_json(RESULT, results)
    print(f"Wrote apply result to {RESULT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
