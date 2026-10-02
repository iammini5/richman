"""Apply a reviewed half-price plan from a captured live catalog; no FX repricing."""
import argparse
import copy
from decimal import Decimal, ROUND_HALF_UP
import json
from pathlib import Path
import subprocess
import urllib.error
import urllib.parse
import sync_play_catalog as api

directory = Path(__file__).parent / 'play-console'
before = json.loads((directory / 'current-catalog-before-half-price.json').read_text())
zero_decimal = {'JPY', 'KRW', 'CLP', 'PYG', 'VND', 'XAF', 'XOF'}
changes = []

def halve(value, path):
    if isinstance(value, dict):
        if 'currencyCode' in value and ('units' in value or 'nanos' in value):
            currency = value['currencyCode']
            original = Decimal(value.get('units', '0')) + Decimal(value.get('nanos', 0)) / Decimal(1000000000)
            precision = Decimal('1') if currency in zero_decimal else Decimal('.01')
            price = (original / 2).quantize(precision, rounding=ROUND_HALF_UP)
            changes.append({'path': path, 'currency': currency, 'before': str(original), 'exactHalf': str(original / 2), 'roundedHalf': str(price)})
            units = int(price)
            return {'currencyCode': currency, 'units': str(units), 'nanos': int((price - units) * 1000000000)}
        return {k: halve(v, path + '/' + k) for k, v in value.items()}
    if isinstance(value, list):
        return [halve(v, path + '/' + str(i)) for i, v in enumerate(value)]
    return value

desired = {'oneTimeProducts': [], 'subscriptions': []}
for kind in desired:
    desired[kind] = [halve(copy.deepcopy(product), kind + '/' + product['productId']) for product in before[kind]]
plan = {'priceScale': 0.5, 'changes': changes, 'catalog': desired}
(directory / 'half-price-plan.json').write_text(json.dumps(plan, indent=2) + '\n')
print('Plan:', len(changes), 'price fields, preserving each regional currency and availability.')
parser = argparse.ArgumentParser()
parser.add_argument('--apply-first-product', action='store_true')
parser.add_argument('--apply-all', action='store_true')
parser.add_argument('--apply-pending-uae-minimum', action='store_true')
args = parser.parse_args()
if args.apply_first_product or args.apply_all or args.apply_pending_uae_minimum:
    token = subprocess.check_output(['gcloud', 'auth', 'application-default', 'print-access-token'], text=True).strip()
    results = []
    products = [('oneTimeProducts', p) for p in desired['oneTimeProducts']]
    if args.apply_all:
        products += [('subscriptions', p) for p in desired['subscriptions']]
    elif args.apply_pending_uae_minimum:
        products = [('oneTimeProducts', p) for p in desired['oneTimeProducts'] if p['productId'] == 'com.legendsoftware.richman.coins.50']
        products += [('subscriptions', p) for p in desired['subscriptions'] if p['productId'] == 'premium_basic_monthly']
        for kind, product in products:
            groups = product['purchaseOptions'] if kind == 'oneTimeProducts' else product['basePlans']
            regional_key = 'regionalPricingAndAvailabilityConfigs' if kind == 'oneTimeProducts' else 'regionalConfigs'
            for group in groups:
                for region in group.get(regional_key, []):
                    if region['regionCode'] == 'AE':
                        region['price'] = {'currencyCode': 'AED', 'nanos': 300000000}
    else:
        products = products[:1]
    for kind, product in products:
        version = product.get('regionsVersion', {}).get('version', before['oneTimeProducts'][0]['regionsVersion']['version'])
        mask = 'purchaseOptions' if kind == 'oneTimeProducts' else 'basePlans'
        endpoint = 'onetimeproducts' if kind == 'oneTimeProducts' else 'subscriptions'
        query = urllib.parse.urlencode({'updateMask': mask, 'regionsVersion.version': version, 'latencyTolerance': 'PRODUCT_UPDATE_LATENCY_TOLERANCE_LATENCY_TOLERANT'})
        base = '/applications/com.legendsoftware.richman/' + endpoint + '/' + product['productId']
        result = {'productId': product['productId'], 'kind': kind}
        patch_succeeded = False
        try:
            updated = api.request_json(token, 'PATCH', base + '?' + query, product)
            patch_succeeded = True
            verified = api.get_json(token, base.replace('/onetimeproducts/', '/oneTimeProducts/'))
            result.update({'status': 'updated', 'response': updated, 'verified': verified})
            expected_prices = []
            actual_prices = []
            def collect(value, prices):
                if isinstance(value, dict):
                    if 'currencyCode' in value:
                        prices.append((value['currencyCode'], int(value.get('units', 0)), int(value.get('nanos', 0))))
                    else:
                        for v in value.values(): collect(v, prices)
                elif isinstance(value, list):
                    for v in value: collect(v, prices)
            collect(product[mask], expected_prices)
            collect(verified[mask], actual_prices)
            result['pricesVerified'] = expected_prices == actual_prices
            print(product['productId'], 'UPDATED', 'prices verified:', result['pricesVerified'], flush=True)
        except urllib.error.HTTPError as error:
            result.update({'status': 'updated_unverified' if patch_succeeded else 'failed', 'httpStatus': error.code, 'error': error.read().decode()})
            print(product['productId'], json.dumps(result), flush=True)
        results.append(result)
        result_file = 'half-price-pending-results.json' if args.apply_pending_uae_minimum else 'half-price-apply-results.json'
        (directory / result_file).write_text(json.dumps(results, indent=2) + '\n')
