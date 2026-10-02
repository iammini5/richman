import json
import argparse
import subprocess
import time
import urllib.parse
import urllib.request
from pathlib import Path
import sync_play_catalog as api

parser = argparse.ArgumentParser(description='Read the fully paginated live Play catalog using existing ADC.')
parser.add_argument('--output', default='current-live-catalog.json')
args = parser.parse_args()
token = subprocess.check_output(['gcloud', 'auth', 'application-default', 'print-access-token'], text=True).strip()
root = '/applications/com.legendsoftware.richman'

def pages(path, key):
    result = []
    page_token = None
    while True:
        suffix = '?pageSize=100'
        if page_token:
            suffix += '&pageToken=' + urllib.parse.quote(page_token, safe='')
        request = urllib.request.Request(api.API_ROOT + root + path + suffix, headers={'Authorization': 'Bearer ' + token, 'X-Goog-User-Project': api.QUOTA_PROJECT})
        with urllib.request.urlopen(request, timeout=30) as response:
            raw = response.read()
            data = json.loads(raw) if raw.strip() else {}
        result.extend(data.get(key, []))
        page_token = data.get('nextPageToken')
        if not page_token:
            return result

snapshot = {'packageName': 'com.legendsoftware.richman', 'capturedAt': int(time.time())}
snapshot['oneTimeProducts'] = pages('/oneTimeProducts', 'oneTimeProducts')
snapshot['subscriptions'] = pages('/subscriptions', 'subscriptions')
snapshot['oneTimeOffers'] = pages('/oneTimeProducts/-/purchaseOptions/-/offers', 'oneTimeProductOffers')
snapshot['subscriptionOffers'] = []
for product in snapshot['subscriptions']:
    for plan in product.get('basePlans', []):
        snapshot['subscriptionOffers'].extend(pages('/subscriptions/' + product['productId'] + '/basePlans/' + plan['basePlanId'] + '/offers', 'subscriptionOffers'))
output = Path(__file__).parent / 'play-console' / args.output
if output.exists():
    raise SystemExit('Refusing to overwrite existing audit snapshot: ' + output.name)
output.write_text(json.dumps(snapshot, indent=2) + '\n')
print('Saved live catalog snapshot:', output.name)
for kind in ('oneTimeProducts', 'subscriptions', 'oneTimeOffers', 'subscriptionOffers'):
    print(kind, len(snapshot[kind]))
for product in snapshot['subscriptions']:
    print(product['productId'], [(p['basePlanId'], p.get('state'), next((r.get('price') for r in p.get('regionalConfigs', []) if r.get('regionCode') == 'US'), None)) for p in product.get('basePlans', [])])
