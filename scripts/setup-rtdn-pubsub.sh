#!/usr/bin/env bash
set -euo pipefail

PROJECT_ID="${PROJECT_ID:-}"
SERVICE_URL="${SERVICE_URL:-}"
TOPIC_NAME="${TOPIC_NAME:-richman-play-rtdn}"
SUBSCRIPTION_NAME="${SUBSCRIPTION_NAME:-richman-play-rtdn-push}"
PLAY_PUBLISHER_SERVICE_ACCOUNT="${PLAY_PUBLISHER_SERVICE_ACCOUNT:-google-play-developer-notifications@system.gserviceaccount.com}"
CLOUD_RUN_SERVICE="${CLOUD_RUN_SERVICE:-richman-backend}"
CLOUD_RUN_REGION="${CLOUD_RUN_REGION:-us-west1}"
PUSH_SERVICE_ACCOUNT_ID="${PUSH_SERVICE_ACCOUNT_ID:-richman-rtdn-push}"

if [[ -z "$PROJECT_ID" || -z "$SERVICE_URL" ]]; then
  echo "PROJECT_ID and SERVICE_URL are required."
  echo "Example: PROJECT_ID=my-gcp-project SERVICE_URL=https://richman-backend-abc.run.app ./scripts/setup-rtdn-pubsub.sh"
  exit 1
fi

PUSH_ENDPOINT="${SERVICE_URL%/}/v1/play/notifications"
PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"
PUSH_SERVICE_ACCOUNT_EMAIL="$PUSH_SERVICE_ACCOUNT_ID@$PROJECT_ID.iam.gserviceaccount.com"
PUBSUB_SERVICE_AGENT="service-$PROJECT_NUMBER@gcp-sa-pubsub.iam.gserviceaccount.com"

gcloud pubsub topics describe "$TOPIC_NAME" --project "$PROJECT_ID" >/dev/null 2>&1 ||
  gcloud pubsub topics create "$TOPIC_NAME" --project "$PROJECT_ID"

gcloud iam service-accounts describe "$PUSH_SERVICE_ACCOUNT_EMAIL" --project "$PROJECT_ID" >/dev/null 2>&1 ||
  gcloud iam service-accounts create "$PUSH_SERVICE_ACCOUNT_ID" \
    --project "$PROJECT_ID" \
    --display-name "Richman RTDN Pub/Sub push"

gcloud run services add-iam-policy-binding "$CLOUD_RUN_SERVICE" \
  --project "$PROJECT_ID" \
  --region "$CLOUD_RUN_REGION" \
  --member "serviceAccount:$PUSH_SERVICE_ACCOUNT_EMAIL" \
  --role "roles/run.invoker" >/dev/null

gcloud iam service-accounts add-iam-policy-binding "$PUSH_SERVICE_ACCOUNT_EMAIL" \
  --project "$PROJECT_ID" \
  --member "serviceAccount:$PUBSUB_SERVICE_AGENT" \
  --role "roles/iam.serviceAccountTokenCreator" >/dev/null

gcloud pubsub subscriptions describe "$SUBSCRIPTION_NAME" --project "$PROJECT_ID" >/dev/null 2>&1 ||
  gcloud pubsub subscriptions create "$SUBSCRIPTION_NAME" \
    --project "$PROJECT_ID" \
    --topic "$TOPIC_NAME" \
    --push-endpoint "$PUSH_ENDPOINT" \
    --push-auth-service-account "$PUSH_SERVICE_ACCOUNT_EMAIL"

gcloud pubsub subscriptions update "$SUBSCRIPTION_NAME" \
  --project "$PROJECT_ID" \
  --push-endpoint "$PUSH_ENDPOINT" \
  --push-auth-service-account "$PUSH_SERVICE_ACCOUNT_EMAIL" >/dev/null

gcloud pubsub topics add-iam-policy-binding "$TOPIC_NAME" \
  --project "$PROJECT_ID" \
  --member "serviceAccount:$PLAY_PUBLISHER_SERVICE_ACCOUNT" \
  --role "roles/pubsub.publisher" >/dev/null

echo "RTDN Pub/Sub topic: projects/$PROJECT_ID/topics/$TOPIC_NAME"
echo "Push endpoint: $PUSH_ENDPOINT"
echo "Google Play publisher: $PLAY_PUBLISHER_SERVICE_ACCOUNT"
echo "Pub/Sub push service account: $PUSH_SERVICE_ACCOUNT_EMAIL"
echo "Configure this topic in Play Console > Monetization setup > Real-time developer notifications."
