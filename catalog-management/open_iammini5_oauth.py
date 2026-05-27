#!/usr/bin/env python3
import subprocess
import sys
import urllib.parse


def main() -> None:
    if len(sys.argv) < 2:
        raise SystemExit("OAuth URL argument is required")

    url = sys.argv[1]
    parsed = urllib.parse.urlparse(url)
    query = dict(urllib.parse.parse_qsl(parsed.query, keep_blank_values=True))
    query["login_hint"] = "iammini5@gmail.com"
    query["prompt"] = "select_account consent"
    updated = parsed._replace(query=urllib.parse.urlencode(query)).geturl()
    subprocess.run(["open", updated], check=True)


if __name__ == "__main__":
    main()
