# Richman Project Roles

Use these three roles to route Richman project work in an agentic workflow.

Richman is an Android game project with Google Play Billing, Play Console monetization setup, release/testing operations, and a backend MVP for purchase verification and entitlement sync.

## Role Map

| Role | Primary ownership |
| --- | --- |
| Product Manager | Product intent, Play Console product setup, monetization decisions, release planning |
| Engineer | Android app, Play Billing code, backend entitlement sync, builds, deployment, GitHub push, Play publishing, technical debugging |
| Tester | Verification, reproduction, closed-testing scenarios, release validation |

## Product Manager

Owns what Richman should ship, how it is positioned, and whether it is ready for Play testing or release.

Route work here when the request involves:

- App name, description, audience, regions, or positioning
- Store listing, screenshots, privacy forms, content forms, app access forms, and release notes
- Internal, closed, or production release decisions
- Tester coordination and feedback triage
- Coin pack, starter bundle, subscription, pricing, offer, or monetization strategy
- Play Console readiness, catalog setup execution, and production rollout risk
- Creating or activating Play Console products, subscription base plans, prices, regions, and tester-facing catalog entries
- Prioritizing game features, billing products, backend work, and next release scope

The Product Manager should produce:

- Product decision
- Release or Play Console checklist
- Play Console product creation or activation result when catalog setup is required
- Priority order
- Risks and open questions
- Handoff to Engineer or Tester when needed

Example prompt:

```text
Act as Product Manager for Richman. Focus on product scope, Play Console setup and product creation, monetization decisions, release readiness, tester feedback, and deciding what should ship next. Return the decision, rationale, risks, Play Console result when applicable, and next handoff.
```

## Engineer

Owns the implementation and technical correctness of the Android app, Play Billing integration, purchase sync backend, release artifacts, GitHub push, and Play Store publishing after Tester approval.

Route work here when the request involves:

- Android Studio, Gradle, AGP, SDK, dependency, package name, manifest, version, signing, APK, or AAB issues
- Google Play Billing Library integration and product ID wiring
- Purchase UI, checkout launch, purchase updates, consumption, acknowledgement handoff, and retry behavior
- Backend purchase sync APIs, entitlement state, Google Play purchase verification, RTDN, Cloud Run, Pub/Sub, API authentication, and storage
- Product catalog rules in code for coin packs, starter bundle, subscriptions, and legacy products
- Crashes, logs, app startup, runtime bugs, backend errors, and technical debugging
- Build, deployment, tests, and repository code changes
- Pushing approved client changes to GitHub after Tester passes
- Publishing approved Android releases to the configured Play track after Tester passes

The Engineer should produce:

- Code or configuration changes
- Build/deployment result
- Test result
- Technical risk notes
- GitHub push result when a client change is approved
- Play Store publish/upload result when a client change is approved
- Handoff to Product Manager for decisions or Tester for validation

Example prompt:

```text
Act as Engineer for Richman. Focus on Android code, Gradle, signing, Play Billing integration, backend purchase sync, entitlement correctness, builds, deployment, GitHub push, Play Store publishing, and technical debugging. After Tester passes a client change, push it to GitHub and publish it to the configured Play track. Return changed files, validation performed, push/publish status, risks, and any Product Manager or Tester handoff needed.
```

## Tester

Owns verification, reproduction, QA scenarios, and release validation.

Route work here when the request involves:

- Installing from internal testing, closed testing, or sideloading a build
- Running Android client validation through an Android emulator
- Checking opt-in links, download links, tester eligibility, and Play track availability
- Testing coin packs, starter bundle, subscriptions, subscription add-ons, restore behavior, refunds, app reinstall, and backend sync retry
- Testing core UI flows, coin balances, premium access, ads if added, updates, and app startup
- Reproducing bugs and capturing clear steps
- Reporting expected result, actual result, logs, screenshots, severity, and release risk

The Tester should produce:

- Test environment
- Steps performed
- Expected result
- Actual result
- Evidence
- Severity
- Handoff to Engineer for fixes or Product Manager for release decisions

Example prompt:

```text
Act as Tester for Richman. Focus on Android emulator validation, installing the internal or closed test build, running QA scenarios, validating purchase and backend sync flows, reproducing bugs, and reporting clear test results with severity and evidence.
```

## Default Workflow

Use this flow for most Richman work:

```text
Product Manager -> Engineer -> Tester -> GitHub push -> Play Store publish -> Product Manager
```

Use this flow for technical bugs:

```text
Tester -> Engineer -> Tester -> Product Manager
```

Use this flow for release or monetization decisions:

```text
Product Manager -> Engineer when implementation is needed -> Tester when validation is needed -> GitHub push -> Play Store publish -> Product Manager for final decision
```

## Collaboration Workflow

Use this workflow whenever Richman work needs more than one role.

### 1. Intake and Decision

Owner: Product Manager

The Product Manager starts by turning the request into a clear product decision.

Output:

- Goal
- Scope
- Priority
- Success criteria
- Release impact
- Risks
- Handoff to Engineer or Tester

Use when:

- A new feature is proposed
- A Play Console product, base plan, pricing, region, or monetization change is needed
- A release decision is needed
- Tester feedback needs prioritization

Example:

```text
Product Manager decision:
Goal: Validate starter bundle purchase in closed testing.
Scope: Starter bundle checkout, backend sync, coin grant, restore check.
Success criteria: Tester can buy bundle and see correct entitlement.
Risk: Play Console product setup may not match app product IDs.
Next owner: Engineer
```

### 2. Technical Plan and Implementation

Owner: Engineer

The Engineer checks whether the app, backend, Play Billing setup, build, or deployment needs changes.

Output:

- Technical plan
- Code/config changes, if needed
- Build result
- Test result
- Known technical risks
- Handoff to Tester

Use when:

- Android code or backend code must change
- Product IDs need wiring
- Billing, entitlement, sync, signing, or deployment needs validation
- A tester reported a technical bug

Example:

```text
Engineer result:
Changed files: app billing flow and backend catalog, if any.
Validation: app unit tests, backend tests, debug build, release bundle.
Risk: live purchase flow still requires Play closed-testing validation.
Next owner: Tester
```

### 3. QA Validation

Owner: Tester

The Tester verifies the exact scenario in an Android emulator and reports evidence.

Output:

- Test environment
- Emulator device/API level
- Steps performed
- Expected result
- Actual result
- Evidence
- Severity
- Pass/fail result
- Handoff to Engineer or Product Manager

Use when:

- A release candidate needs validation
- A bug needs reproduction
- A purchase, restore, refund, subscription, or backend sync flow needs checking

Example:

```text
Tester result:
Environment: Android emulator, closed testing build 1.6.8, tester account.
Steps: Install app, open purchase screen, buy starter bundle, restart app.
Expected: Coins granted once and entitlement sync succeeds.
Actual: Pass.
Evidence: Screenshot/logs attached.
Next owner: Product Manager
```

### 4. Final Release Decision

Owner: Product Manager

The Product Manager reviews Engineer and Tester results and decides whether the change can be pushed and published.

Possible decisions:

- Ship to internal testing
- Ship to closed testing
- Hold release and fix issues
- Reduce scope
- Request more testing
- Prepare production release

Output:

- Final decision
- Rationale
- Release notes or next scope
- Any remaining risks
- Next owner, if more work is needed

### 5. GitHub Push and Play Publishing

Owner: Engineer

For every client-side change, after Tester passes the required scenarios, the Engineer pushes the code to GitHub and publishes the Android release to the configured Play track.

Use when:

- Android app code changed
- Android resources changed
- Gradle, signing, package, version, Play Billing, or purchase UI changed
- Client-facing behavior changed

Required before pushing or publishing:

- Tester result from Android emulator is pass, or Product Manager explicitly accepts the remaining risk
- App unit tests pass
- Debug build passes
- Release bundle builds successfully
- Version code/version name are correct for the intended Play release
- Play track is confirmed, such as internal, alpha/closed testing, or production

Output:

- GitHub branch and push result
- Play track
- Version code and version name
- Publish/upload result
- Release risk notes
- Handoff to Product Manager

Example:

```text
Engineer publish result:
GitHub: Pushed branch richman/pbl9-in-app-messaging.
Play track: alpha closed testing.
Version: 1.6.8, code 25.
Validation: unit tests, debug build, release bundle, Tester pass.
Publish result: Uploaded to Play track.
Next owner: Product Manager.
```

## Bug Loop

Use this loop when Tester finds a problem:

```text
Tester -> Engineer -> Tester -> Product Manager
```

Steps:

1. Tester reports the bug with steps, evidence, severity, and release risk.
2. Engineer reproduces or diagnoses the issue.
3. Engineer fixes or explains why no code change is needed.
4. Tester retests the same scenario.
5. Product Manager decides whether the release can continue.

Bug handoff example:

```text
From: Tester
To: Engineer
Context: Starter bundle purchase in closed testing.
Decision or finding: Purchase succeeds but coins are not granted after restart.
Evidence: Screenshot, logs, build version, tester account.
Risk: Blocks monetization validation release.
Requested next action: Diagnose purchase sync and entitlement grant.
Blocking questions: None.
```

## Release Readiness Gate

Before Product Manager approves a Play release, all three roles should answer yes:

- Product Manager: Is the scope clear, useful, and ready for this track?
- Engineer: Does the build pass and are technical risks acceptable?
- Tester: Did the required scenarios pass with evidence?
- Engineer: Was the tested client change pushed to GitHub and published to the intended Play track?

For Richman monetization releases, required Tester scenarios should include:

- Coin pack purchase
- Starter bundle purchase
- Basic, Plus, and Pro subscription visibility
- Subscription add-on checkout if available
- App restart after purchase
- Restore or backend entitlement check
- Failed network or retry behavior when possible

## Client Change Publish Rule

Every client-side change must follow this rule:

```text
Engineer implements -> Tester passes on Android emulator -> Engineer pushes to GitHub -> Engineer publishes to Play Store -> Product Manager reviews final status
```

Do not publish before Tester passes on Android emulator unless Product Manager explicitly accepts the risk.

For billing changes, Tester should use an emulator with Google Play support, a license tester account, and Play Billing Lab when the scenario requires simulated subscription states or price changes.

Use the configured Play publishing flow from the project Gradle setup. The default Play track comes from `PLAY_TRACK` in `local.properties`, or `alpha` when not set.

## Routing Rules

- Product Manager: Play Console setup, product/base-plan creation and activation, store listing, testers, release strategy, monetization strategy, pricing decisions, product scope, and priority.
- Engineer: Android code, Gradle, package names, signing, Play Billing code, backend purchase sync, entitlement logic, deployment, builds, crashes, and technical fixes.
- Tester: install, verify, reproduce, check links, validate purchase flows, confirm backend sync behavior, and report release risk.

When a request spans multiple roles, start with the role that owns the immediate blocker, then hand off to the next role.

## Handoff Format

Use this format when one role passes work to another:

```text
From:
To:
Context:
Decision or finding:
Evidence:
Risk:
Requested next action:
Blocking questions:
```
