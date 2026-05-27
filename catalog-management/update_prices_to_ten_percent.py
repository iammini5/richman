#!/usr/bin/env python3
import argparse
import copy
import json
import subprocess
import time
import urllib.error
import urllib.parse
from pathlib import Path
from typing import List

import sync_play_catalog


ROOT = Path(__file__).resolve().parents[1]
SNAPSHOT = ROOT / "catalog-management" / "play-console" / "catalog.json"
PLAN = ROOT / "catalog-management" / "play-console" / "ten-percent-price-plan.json"
RESULT = ROOT / "catalog-management" / "play-console" / "ten-percent-price-result.json"
PACKAGE_NAME = "com.legendsoftware.richman"


def money_to_nanos(price: dict) -> int:
    return int(price.get("units", "0")) * 1_000_000_000 + int(price.get("nanos", 0))


def nanos_to_money(total_nanos: int, currency_code: str) -> dict:
    units, nanos = divmod(total_nanos, 1_000_000_000)
    price = {"currencyCode": currency_code}
    if units:
        price["units"] = str(units)
    if nanos:
        price["nanos"] = nanos
    return price


def ten_percent_price(price: dict) -> dict:
    current = money_to_nanos(price)
    updated = max(1, round(current * 0.10))
    return nanos_to_money(updated, price["currencyCode"])


def scale_price_field(container: dict, key: str, changes: List[dict], product_id: str, location: str) -> None:
    price = container.get(key)
    if not price:
        return
    new_price = ten_percent_price(price)
    changes.append(
        {
            "productId": product_id,
            "location": location,
            "oldPrice": price,
            "newPrice": new_price,
        }
    )
    container[key] = new_price


def scale_one_time_product(product: dict, changes: List[dict]) -> dict:
    updated = copy.deepcopy(product)
    product_id = updated["productId"]
    for option in updated.get("purchaseOptions", []):
        option_id = option.get("purchaseOptionId", "unknown-option")
        new_regions = option.get("newRegionsConfig", {})
        scale_price_field(new_regions, "usdPrice", changes, product_id, f"{option_id}.newRegions.usd")
        scale_price_field(new_regions, "eurPrice", changes, product_id, f"{option_id}.newRegions.eur")
        for regional_config in option.get("regionalPricingAndAvailabilityConfigs", []):
            if "price" not in regional_config:
                continue
            old_price = regional_config["price"]
            new_price = ten_percent_price(old_price)
            changes.append(
                {
                    "productId": product_id,
                    "location": f"{option_id}.region.{regional_config.get('regionCode')}",
                    "oldPrice": old_price,
                    "newPrice": new_price,
                }
            )
            regional_config["price"] = new_price
    return updated


def scale_subscription(subscription: dict, changes: List[dict]) -> dict:
    updated = copy.deepcopy(subscription)
    product_id = updated["productId"]
    for base_plan in updated.get("basePlans", []):
        base_plan_id = base_plan.get("basePlanId", "unknown-base-plan")
        other_regions = base_plan.get("otherRegionsConfig", {})
        scale_price_field(other_regions, "usdPrice", changes, product_id, f"{base_plan_id}.otherRegions.usd")
        scale_price_field(other_regions, "eurPrice", changes, product_id, f"{base_plan_id}.otherRegions.eur")
        for regional_config in base_plan.get("regionalConfigs", []):
            if "price" not in regional_config:
                continue
            old_price = regional_config["price"]
            new_price = ten_percent_price(old_price)
            changes.append(
                {
                    "productId": product_id,
                    "location": f"{base_plan_id}.region.{regional_config.get('regionCode')}",
                    "oldPrice": old_price,
                    "newPrice": new_price,
                }
            )
            regional_config["price"] = new_price
    return updated


def write_json(path: Path, value: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n")


def api_error(exc: urllib.error.HTTPError) -> dict:
    return {
        "status": exc.code,
        "reason": exc.reason,
        "body": exc.read().decode("utf-8", errors="replace"),
    }


def patch_one_time_product(token: str, product: dict) -> dict:
    package = urllib.parse.quote(PACKAGE_NAME, safe="")
    product_id = urllib.parse.quote(product["productId"], safe="")
    regions_version = product.get("regionsVersion", {}).get("version", "2025/03")
    query = urllib.parse.urlencode(
        {
            "updateMask": "purchaseOptions",
            "regionsVersion.version": regions_version,
            "latencyTolerance": "PRODUCT_UPDATE_LATENCY_TOLERANCE_LATENCY_TOLERANT",
        }
    )
    return sync_play_catalog.request_json(
        token,
        "PATCH",
        f"/applications/{package}/onetimeproducts/{product_id}?{query}",
        product,
    )


def patch_subscription(token: str, subscription: dict, regions_version: str) -> dict:
    package = urllib.parse.quote(PACKAGE_NAME, safe="")
    product_id = urllib.parse.quote(subscription["productId"], safe="")
    query = urllib.parse.urlencode(
        {
            "updateMask": "basePlans",
            "regionsVersion.version": regions_version,
            "latencyTolerance": "PRODUCT_UPDATE_LATENCY_TOLERANCE_LATENCY_TOLERANT",
        }
    )
    return sync_play_catalog.request_json(
        token,
        "PATCH",
        f"/applications/{package}/subscriptions/{product_id}?{query}",
        subscription,
    )


def main() -> None:
    parser = argparse.ArgumentParser(description="Set Google Play product prices to 10% of the synced snapshot.")
    parser.add_argument("--apply", action="store_true", help="Apply the generated plan through the Play Developer API.")
    parser.add_argument(
        "--auth",
        choices=("service-account", "gcloud", "adc"),
        default="service-account",
        help="API credential source to use when applying changes.",
    )
    args = parser.parse_args()

    snapshot = json.loads(SNAPSHOT.read_text())
    one_time_products = snapshot["catalog"]["oneTimeProducts"].get("oneTimeProducts", [])
    subscriptions = snapshot["catalog"]["subscriptions"].get("subscriptions", [])
    regions_version = next(
        (
            product.get("regionsVersion", {}).get("version")
            for product in one_time_products
            if product.get("regionsVersion", {}).get("version")
        ),
        "2025/03",
    )

    changes: List[dict] = []
    updated_one_time = [scale_one_time_product(product, changes) for product in one_time_products]
    updated_subscriptions = [scale_subscription(subscription, changes) for subscription in subscriptions]
    plan = {
        "packageName": PACKAGE_NAME,
        "createdAtEpochSeconds": int(time.time()),
        "mode": "apply" if args.apply else "dry-run",
        "priceScale": 0.10,
        "regionsVersion": regions_version,
        "changeCount": len(changes),
        "changes": changes,
        "catalog": {
            "oneTimeProducts": updated_one_time,
            "subscriptions": updated_subscriptions,
        },
    }
    write_json(PLAN, plan)

    if not args.apply:
        print(f"Wrote dry-run plan to {PLAN.relative_to(ROOT)}")
        print(f"Planned {len(changes)} price changes across {len(updated_one_time)} one-time products and {len(updated_subscriptions)} subscriptions.")
        return

    if args.auth == "gcloud":
        token = subprocess.check_output(
            ["gcloud", "auth", "print-access-token"],
            text=True,
        ).strip()
    elif args.auth == "adc":
        token = subprocess.check_output(
            ["gcloud", "auth", "application-default", "print-access-token"],
            text=True,
        ).strip()
    else:
        service_account = json.loads(sync_play_catalog.service_account_path().read_text())
        token = sync_play_catalog.access_token(service_account)
    results = {
        "packageName": PACKAGE_NAME,
        "appliedAtEpochSeconds": int(time.time()),
        "priceScale": 0.10,
        "oneTimeProducts": [],
        "subscriptions": [],
    }

    for product in updated_one_time:
        try:
            response = patch_one_time_product(token, product)
            results["oneTimeProducts"].append({"productId": product["productId"], "status": "updated", "response": response})
            print(f"Updated one-time product {product['productId']}")
        except urllib.error.HTTPError as exc:
            results["oneTimeProducts"].append({"productId": product["productId"], "status": "error", "error": api_error(exc)})
            print(f"Failed one-time product {product['productId']}: HTTP {exc.code}")

    for subscription in updated_subscriptions:
        try:
            response = patch_subscription(token, subscription, regions_version)
            results["subscriptions"].append({"productId": subscription["productId"], "status": "updated", "response": response})
            print(f"Updated subscription {subscription['productId']}")
        except urllib.error.HTTPError as exc:
            results["subscriptions"].append({"productId": subscription["productId"], "status": "error", "error": api_error(exc)})
            print(f"Failed subscription {subscription['productId']}: HTTP {exc.code}")

    write_json(RESULT, results)
    print(f"Wrote apply result to {RESULT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
