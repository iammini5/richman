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
./scripts/setup-rtdn-pubsub.sh
```

The script prints a topic name like:

```text
projects/YOUR_PROJECT_ID/topics/richman-play-rtdn
```

Configure that topic in Play Console:

```text
Play Console > Monetization setup > Real-time developer notifications
```

## Environment Variables

- `PORT`: set by Cloud Run automatically.
- `RICHMAN_ALLOW_UNVERIFIED_PLAY_PURCHASES`: use `false` for production.
- Future: Google Play credential and database variables will be added when the production verifier and database repository are implemented.

## Next Production Steps

1. Add durable PostgreSQL storage.
2. Add Google Play Developer API verification.
3. Store Google credentials in Secret Manager.
4. Add app authentication for purchase sync.
5. Add authenticated Pub/Sub push handling.
