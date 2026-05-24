#!/usr/bin/env bash
set -euo pipefail

SERVICE_NAME="${SERVICE_NAME:-richman-backend}"
REGION="${REGION:-us-west1}"
PROJECT_ID="${PROJECT_ID:-}"
ALLOW_UNVERIFIED_PLAY_PURCHASES="${ALLOW_UNVERIFIED_PLAY_PURCHASES:-false}"
ALLOW_UNAUTHENTICATED="${ALLOW_UNAUTHENTICATED:-false}"

if [[ -z "$PROJECT_ID" ]]; then
  echo "PROJECT_ID is required."
  echo "Example: PROJECT_ID=my-gcp-project REGION=us-west1 ./scripts/deploy-backend-cloud-run.sh"
  exit 1
fi

gcloud config set project "$PROJECT_ID" >/dev/null

gcloud services enable \
  run.googleapis.com \
  cloudbuild.googleapis.com \
  artifactregistry.googleapis.com \
  pubsub.googleapis.com \
  secretmanager.googleapis.com \
  androidpublisher.googleapis.com \
  --project "$PROJECT_ID"

AUTH_FLAG="--no-allow-unauthenticated"
if [[ "$ALLOW_UNAUTHENTICATED" == "true" ]]; then
  AUTH_FLAG="--allow-unauthenticated"
fi

gcloud run deploy "$SERVICE_NAME" \
  --source . \
  --project "$PROJECT_ID" \
  --region "$REGION" \
  "$AUTH_FLAG" \
  --set-env-vars "RICHMAN_ALLOW_UNVERIFIED_PLAY_PURCHASES=$ALLOW_UNVERIFIED_PLAY_PURCHASES" \
  --quiet

SERVICE_URL="$(
  gcloud run services describe "$SERVICE_NAME" \
    --project "$PROJECT_ID" \
    --region "$REGION" \
    --format='value(status.url)'
)"

echo "Deployed $SERVICE_NAME to $SERVICE_URL"
echo "Health check: curl $SERVICE_URL/health"
