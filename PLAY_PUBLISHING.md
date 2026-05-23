# Play Store Publishing

This project uses Gradle Play Publisher to upload release App Bundles to Google Play.

## Local setup

Do not commit service account JSON files. Put the key somewhere local, then add this to `local.properties`:

```properties
PLAY_SERVICE_ACCOUNT_FILE=/absolute/path/to/play-service-account.json
PLAY_TRACK=alpha
```

If `PLAY_SERVICE_ACCOUNT_FILE` is not set, the build checks for `service-account.json` in the repo root. You can also leave both unset and provide credentials through your environment, for example with `ANDROID_PUBLISHER_CREDENTIALS`.

## Upload

Build and publish the release bundle:

```sh
./gradlew publishReleaseBundle
```

The default track is `alpha`, matching the current closed testing track. Production publishing still depends on Google Play granting production access.
