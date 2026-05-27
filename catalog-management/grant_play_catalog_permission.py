#!/usr/bin/env python3
import argparse
import json
import subprocess
import urllib.error
import urllib.parse
from pathlib import Path

import sync_play_catalog


ROOT = Path(__file__).resolve().parents[1]
PACKAGE_NAME = "com.legendsoftware.richman"
SERVICE_ACCOUNT_FILE = ROOT / "service-account.json"
PERMISSIONS = [
    "CAN_VIEW_NON_FINANCIAL_DATA",
    "CAN_MANAGE_PUBLIC_LISTING",
    "CAN_MANAGE_TRACK_APKS",
    "CAN_MANAGE_PUBLIC_APKS",
    "CAN_MANAGE_ORDERS",
]


def adc_token() -> str:
    return subprocess.check_output(
        ["gcloud", "auth", "application-default", "print-access-token"],
        text=True,
    ).strip()


def service_account_email() -> str:
    service_account = json.loads(SERVICE_ACCOUNT_FILE.read_text())
    return service_account["client_email"]


def error_body(exc: urllib.error.HTTPError) -> str:
    return exc.read().decode("utf-8", errors="replace")


def grant_name(developer_id: str, email: str) -> str:
    return f"developers/{developer_id}/users/{email}/grants/{PACKAGE_NAME}"


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Grant Play catalog permissions to the Richman Play API service account."
    )
    parser.add_argument(
        "--developer-id",
        required=True,
        help="Google Play developer account id, the numeric id from the Play Console account URL.",
    )
    parser.add_argument(
        "--target-email",
        default=service_account_email(),
        help="User or service account email to grant app permissions to.",
    )
    args = parser.parse_args()

    token = adc_token()
    developer_id = args.developer_id
    target_email = args.target_email
    parent = f"/developers/{developer_id}"
    body = {
        "name": grant_name(developer_id, target_email),
        "packageName": PACKAGE_NAME,
        "appLevelPermissions": PERMISSIONS,
    }

    try:
        result = sync_play_catalog.post_json(
            token,
            f"{parent}/users/{urllib.parse.quote(target_email, safe='')}/grants",
            body,
        )
        print(json.dumps({"status": "created", "grant": result}, indent=2))
    except urllib.error.HTTPError as create_error:
        if create_error.code != 409:
            print(json.dumps({"status": "error", "operation": "create", "body": error_body(create_error)}, indent=2))
            raise SystemExit(1)

        try:
            result = sync_play_catalog.patch_json(
                token,
                f"/{grant_name(developer_id, urllib.parse.quote(target_email, safe=''))}?updateMask=appLevelPermissions",
                body,
            )
            print(json.dumps({"status": "updated", "grant": result}, indent=2))
        except urllib.error.HTTPError as patch_error:
            print(json.dumps({"status": "error", "operation": "patch", "body": error_body(patch_error)}, indent=2))
            raise SystemExit(1)


if __name__ == "__main__":
    main()
