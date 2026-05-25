# Deploy Richman Backend

## Recommended Host

Use Google Cloud Run for the backend service. It gives the app an HTTPS endpoint, works with Cloud Pub/Sub for Real-time Developer Notifications, and can build this repo from source with the included `Dockerfile`.

## Current Deployment State

Cloud Run service:

```text
Project: api-6551333220726747549-208518
Region: us-west1
Service: richman-backend
URL: https://richman-backend-kfy6nq5mia-uw.a.run.app
Access: authenticated
```

The backend can be deployed as a Cloud Run service, but it is still an MVP:

- Persistence is in-memory only.
- Google Play verification is a pluggable interface, but the production Google Play Developer API implementation is not added yet.
- App/API authentication is not added yet.
- Pub/Sub push authentication is not added yet.

For a real production release, do not enable `RICHMAN_ALLOW_UNVERIFIED_PLAY_PURCHASES`.

The Google Play service account used for purchase verification is:

```text
iammini5@api-6551333220726747549-208518.iam.gserviceaccount.com
```

In Play Console, grant this service account the billing API permissions documented by Google:

- View financial data, orders, and cancellation survey responses
- Manage orders and subscriptions

Google documents these permissions as required for Google Play Billing API access.

## Prerequisites

- Google Cloud project with billing enabled.
- `gcloud` installed and logged in.
- Permission to enable APIs and deploy Cloud Run services.
- Play Console access to configure Real-time Developer Notifications.

Login and select a project:

```sh
gcloud auth login
gcloud config set project YOUR_PROJECT_ID
```

## Deploy Cloud Run

From the repo root:

```sh
PROJECT_ID=YOUR_PROJECT_ID REGION=us-west1 ./scripts/deploy-backend-cloud-run.sh
```

The script enables required APIs and deploys `richman-backend` from source. By default, the Cloud Run service is authenticated and not public.

This repo includes `.gcloudignore` so Cloud Run source deployment does not upload local Gradle caches or generated build output.

If you intentionally want a public HTTPS endpoint, set:

```sh
ALLOW_UNAUTHENTICATED=true
```

For temporary closed-testing smoke tests only, you can allow mock purchase verification:

```sh
PROJECT_ID=YOUR_PROJECT_ID \
REGION=us-west1 \
ALLOW_UNVERIFIED_PLAY_PURCHASES=true \
ALLOW_UNAUTHENTICATED=true \
./scripts/deploy-backend-cloud-run.sh
```

After deployment, test health:

```sh
curl https://YOUR_CLOUD_RUN_URL/health
```

## Configure RTDN Pub/Sub

After Cloud Run is deployed, create the Pub/Sub topic and push subscription:

```sh
PROJECT_ID=YOUR_PROJECT_ID \
SERVICE_URL=https://YOUR_CLOUD_RUN_URL \
CLOUD_RUN_SERVICE=richman-backend \
CLOUD_RUN_REGION=us-west1 \
./scripts/setup-rtdn-pubsub.sh
```

The script prints a topic name like:

```text
projects/YOUR_PROJECT_ID/topics/richman-play-rtdn
```

It also grants Google Play permission to publish RTDN messages to the topic through:

```text
google-play-developer-notifications@system.gserviceaccount.com
```

For private Cloud Run services, the script also creates or updates the Pub/Sub push service account and grants it Cloud Run invoker access.

Configure that topic in Play Console:

```text
Play Console > Monetization setup > Real-time developer notifications
```

Enable real-time notifications and choose `Subscriptions, voided purchases, and all one-time products` if one-time products should trigger backend syncs.

## Environment Variables

- `PORT`: set by Cloud Run automatically.
- `RICHMAN_ALLOW_UNVERIFIED_PLAY_PURCHASES`: use `false` for production.
- `PLAY_SERVICE_ACCOUNT_JSON`: Secret Manager value containing the Play service account JSON.
- `RICHMAN_APP_API_KEY`: Secret Manager value required by the Android app in `X-Richman-Api-Key`.

## Production Verification Deploy

Create secrets:

```sh
gcloud secrets create richman-play-service-account-json \
  --project YOUR_PROJECT_ID \
  --data-file service-account.json

printf 'YOUR_APP_API_KEY' | gcloud secrets create richman-app-api-key \
  --project YOUR_PROJECT_ID \
  --data-file -
```

Deploy with real Google Play verification:

```sh
PROJECT_ID=YOUR_PROJECT_ID \
REGION=us-west1 \
PLAY_SERVICE_ACCOUNT_SECRET=richman-play-service-account-json \
RICHMAN_APP_API_KEY_SECRET=richman-app-api-key \
ALLOW_UNAUTHENTICATED=true \
./scripts/deploy-backend-cloud-run.sh
```

## Next Production Steps

1. Add durable PostgreSQL storage.
2. Add stronger app authentication for purchase sync.
3. Add Pub/Sub push authentication.
4. Move entitlement storage from memory to PostgreSQL.
