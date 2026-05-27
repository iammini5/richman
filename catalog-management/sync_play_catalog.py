#!/usr/bin/env python3
import base64
import json
import subprocess
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "catalog-management" / "play-console" / "catalog.json"
PACKAGE_NAME = "com.legendsoftware.richman"
TOKEN_URL = "https://oauth2.googleapis.com/token"
API_ROOT = "https://androidpublisher.googleapis.com/androidpublisher/v3"
SCOPE = "https://www.googleapis.com/auth/androidpublisher"


def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).decode("utf-8").rstrip("=")


def load_local_properties() -> dict[str, str]:
    path = ROOT / "local.properties"
    if not path.exists():
        return {}

    values: dict[str, str] = {}
    for line in path.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    return values


def service_account_path() -> Path:
    local_properties = load_local_properties()
    configured = local_properties.get("PLAY_SERVICE_ACCOUNT_FILE")
    if configured:
        candidate = Path(configured)
        return candidate if candidate.is_absolute() else ROOT / candidate
    return ROOT / "service-account.json"


def sign_with_openssl(message: bytes, private_key: str) -> bytes:
    with tempfile.NamedTemporaryFile("w", delete=True) as key_file:
        key_file.write(private_key)
        key_file.flush()
        result = subprocess.run(
            ["openssl", "dgst", "-sha256", "-sign", key_file.name],
            input=message,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=True,
        )
    return result.stdout


def access_token(service_account: dict[str, str]) -> str:
    now = int(time.time())
    header = {"alg": "RS256", "typ": "JWT"}
    claim = {
        "iss": service_account["client_email"],
        "scope": SCOPE,
        "aud": TOKEN_URL,
        "iat": now,
        "exp": now + 3600,
    }
    signing_input = (
        b64url(json.dumps(header, separators=(",", ":")).encode("utf-8"))
        + "."
        + b64url(json.dumps(claim, separators=(",", ":")).encode("utf-8"))
    ).encode("utf-8")
    signature = sign_with_openssl(signing_input, service_account["private_key"])
    assertion = signing_input.decode("utf-8") + "." + b64url(signature)

    body = urllib.parse.urlencode(
        {
            "grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer",
            "assertion": assertion,
        }
    ).encode("utf-8")
    request = urllib.request.Request(
        TOKEN_URL,
        data=body,
        headers={"Content-Type": "application/x-www-form-urlencoded"},
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        token_response = json.loads(response.read().decode("utf-8"))
    return token_response["access_token"]


def get_json(token: str, path: str) -> dict:
    request = urllib.request.Request(
        f"{API_ROOT}{path}",
        headers={"Authorization": f"Bearer {token}", "Accept": "application/json"},
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.loads(response.read().decode("utf-8"))


def try_get_json(token: str, path: str) -> dict:
    try:
        return get_json(token, path)
    except urllib.error.HTTPError as exc:
        return {
            "error": {
                "status": exc.code,
                "reason": exc.reason,
                "body": exc.read().decode("utf-8", errors="replace"),
            }
        }


def main() -> None:
    service_account_file = service_account_path()
    if not service_account_file.exists():
        raise SystemExit(f"Service account file not found: {service_account_file}")

    service_account = json.loads(service_account_file.read_text())
    token = access_token(service_account)
    package = urllib.parse.quote(PACKAGE_NAME, safe="")

    snapshot = {
        "packageName": PACKAGE_NAME,
        "syncedAtEpochSeconds": int(time.time()),
        "source": "Google Play Developer API",
        "catalog": {
            "legacyInAppProducts": try_get_json(
                token,
                f"/applications/{package}/inappproducts",
            ),
            "oneTimeProducts": try_get_json(
                token,
                f"/applications/{package}/oneTimeProducts",
            ),
            "subscriptions": try_get_json(
                token,
                f"/applications/{package}/subscriptions",
            ),
        },
    }

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(snapshot, indent=2, sort_keys=True) + "\n")
    print(f"Wrote {OUTPUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
