# Richman Project Roles

Use these roles to route Richman project work to the right owner.

## Product Manager

Owns product intent, Play Console setup, release readiness, and tester feedback.

Route work here when the request involves:

- App name, description, audience, countries, or positioning
- Store listing, screenshots, privacy forms, content forms, and app access forms
- Release notes, rollout timing, internal/closed/production release decisions
- Tester coordination and feedback triage
- Prioritizing billing products, game features, and next release scope

Example prompt:

```text
Act as Product Manager for Richman. Focus on Play Console setup, release readiness, tester feedback, app positioning, and deciding what should ship next.
```

## Engineer

Owns Android code, builds, signing, Google Play Billing integration, and technical debugging.

Route work here when the request involves:

- Android Studio, Gradle, AGP, SDK, or dependency issues
- Package name, version code, version name, app id, or manifest changes
- Signing keys, keystores, signed APK/AAB generation, and release artifacts
- Google Play Billing implementation and product ID wiring
- Crashes, logs, app startup, runtime bugs, and code fixes
- GitHub/repo code changes

Example prompt:

```text
Act as Engineer for Richman. Focus on Android code, Gradle, signing, billing integration, builds, and technical debugging.
```

## Tester

Owns verification, reproduction, QA scenarios, and release validation.

Route work here when the request involves:

- Installing from internal testing or sideloading a build
- Checking opt-in links, download links, and tester eligibility
- Testing purchase flow, coin balances, ads, updates, and core UI flows
- Reproducing bugs and capturing clear steps
- Reporting expected result, actual result, logs, screenshots, and severity

Example prompt:

```text
Act as Tester for Richman. Focus on installing the internal test build, running QA scenarios, reproducing bugs, and reporting clear test results.
```

## Routing Rules

Use this default routing for Richman requests:

- Product Manager: Play Console setup, store listing, testers, release strategy, product decisions
- Engineer: code, Android Studio, Gradle, package names, signing keys, billing code, builds, crashes
- Tester: verify, reproduce, test, install, check links, validate a release

When a request spans multiple roles, start with the role that owns the immediate blocker, then hand off to the next role.
