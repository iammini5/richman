#!/usr/bin/env bash
set -euo pipefail

PROJECT_ID="${PROJECT_ID:-}"
SERVICE_URL="${SERVICE_URL:-}"
TOPIC_NAME="${TOPIC_NAME:-richman-play-rtdn}"
SUBSCRIPTION_NAME="${SUBSCRIPTION_NAME:-richman-play-rtdn-push}"

if [[ -z "$PROJECT_ID" || -z "$SERVICE_URL" ]]; then
  echo "PROJECT_ID and SERVICE_URL are required."
  echo "Example: PROJECT_ID=my-gcp-project SERVICE_URL=https://richman-backend-abc.run.app ./scripts/setup-rtdn-pubsub.sh"
  exit 1
fi

PUSH_ENDPOINT="${SERVICE_URL%/}/v1/play/notifications"

gcloud pubsub topics describe "$TOPIC_NAME" --project "$PROJECT_ID" >/dev/null 2>&1 ||
  gcloud pubsub topics create "$TOPIC_NAME" --project "$PROJECT_ID"

gcloud pubsub subscriptions describe "$SUBSCRIPTION_NAME" --project "$PROJECT_ID" >/dev/null 2>&1 ||
  gcloud pubsub subscriptions create "$SUBSCRIPTION_NAME" \
    --project "$PROJECT_ID" \
    --topic "$TOPIC_NAME" \
    --push-endpoint "$PUSH_ENDPOINT"

echo "RTDN Pub/Sub topic: projects/$PROJECT_ID/topics/$TOPIC_NAME"
echo "Push endpoint: $PUSH_ENDPOINT"
echo "Configure this topic in Play Console > Monetization setup > Real-time developer notifications."
