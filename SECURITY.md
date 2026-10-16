# Security policy

Thai Clock Fix changes privileged Android settings through Shizuku. This document defines the supported security boundary, vulnerability reporting process, release integrity rules, and repository controls.

## Supported versions

Only the latest GitHub Release receives security support. Older APKs should be upgraded before a security issue is investigated unless the issue also affects the latest release.

## Reporting a vulnerability

Use [GitHub private vulnerability reporting](https://github.com/justl3as/thai-clock-fix/security/advisories/new) for suspected vulnerabilities.

Do not open a public issue containing exploit details, signing material, access tokens, device identifiers, private logs, or other sensitive data.

A useful report includes:

- affected Thai Clock Fix version
- Android version and OEM/device family
- Shizuku version and startup method
- steps to reproduce
- expected and observed behavior
- security impact
- redacted logs or proof of concept when needed

Public disclosure should wait until a fix or mitigation is available.

## Security model

Thai Clock Fix does not require root. It relies on a running Shizuku service for the privilege needed to execute the supported Android shell commands.

The app currently operates on Android owner user `0` and changes:

- `system/time_12_24`
- the per-app locale override for `com.android.systemui`

Locale values come from the fixed `LocaleMode` enum. The normal UI does not accept arbitrary shell commands from the user.

A successful apply operation requires readback to confirm both the selected SystemUI locale and 24-hour time. The app does not present the in-app clock preview as proof of status-bar state.

## Data and network behavior

The Android manifest does not request the `INTERNET` permission.

The app stores a small local preference record using Android DataStore, including the selected locale, the last applied locale, success state, timestamp, last verified SystemUI locale, and the most recent error message.

Application backup and device-transfer backup are disabled. Cleartext network traffic is also disabled at the application level.

The project does not include analytics, advertising, Firebase, Gemini, or another remote telemetry SDK.

## Shizuku boundary

The exported `ShizukuProvider` is required for Shizuku integration and is protected by `android.permission.INTERACT_ACROSS_USERS_FULL`.

Thai Clock Fix treats the Shizuku service and Android system services as trusted dependencies. A compromised Shizuku installation, rooted operating system, modified framework, or malicious firmware is outside the app's security boundary.

## Process execution controls

Shell execution is bounded by the application code:

- supported commands are generated internally
- stdout and stderr are limited to 64 KiB per stream
- process execution has a 10-second application timeout
- cancellation destroys the process and closes its pipes
- mutation and readback operations are serialized to avoid stale state overwriting a newer result

Some synchronous Binder IPC performed by Shizuku may not be interruptible at every point by a coroutine timeout. The application therefore treats interrupted or partially completed operations as unverified until a later readback.

## Release integrity

Production releases are distributed through [GitHub Releases](https://github.com/justl3as/thai-clock-fix/releases).

Each release must include:

- a production-signed APK
- an APK SHA-256 checksum file
- a release provenance manifest tying the APK to its source commit and release-candidate run
- a successful APK signature verification
- a signing certificate SHA-256 match against the repository policy before any release asset is published

Production APKs are built once by the **Release Candidate** workflow. The final **Publish Release** workflow downloads that exact Actions artifact and verifies its successful RC run, source commit, version metadata, APK SHA-256, checksum, and signing certificate before publication. Final publication does not rebuild the APK.

The enforced certificate policy is stored in `.github/release-signing-cert.sha256`. Publication stops if the APK is unsigned, invalidly signed, signed by more than one certificate, signed by a certificate whose SHA-256 digest does not match that policy, or if its provenance does not match the selected successful RC run.

Production signing certificate SHA-256:

```text
753ffe59729248f199fa9ff6789a136c62e87ed963ef9798eb4a34534503e5c2
```

Users can verify an APK with Android Build Tools:

```sh
apksigner verify --verbose --print-certs ThaiClockFix-vX.Y.Z.apk
```

The signer certificate must match the fingerprint above for an official update to the existing `com.thaiclockfix` application identity.

## Signing key handling

Production signing material must never be committed to the repository.

Release credentials are supplied to the Release Candidate workflow through repository secrets. The workflow writes the keystore to the runner's temporary directory with restricted permissions and removes it in an `always()` cleanup step. The final publication workflow does not need the signing key because it promotes the already signed RC artifact without rebuilding it.

Maintainers must keep an independent encrypted backup of the production signing key. GitHub Actions Secrets are not a signing-key backup.

## Repository and supply-chain controls

The repository uses the following controls:

- protected `main` branch with required CI verification
- GitHub Actions pinned to immutable commit SHAs
- read-only default GitHub Actions token permissions
- CodeQL analysis for GitHub Actions and Java/Kotlin
- Dependabot dependency monitoring
- GitHub secret scanning and push protection
- R8/resource-shrunk release builds
- stable Gradle Wrapper with `distributionSha256Sum`

Gradle dependency verification metadata for the complete Maven dependency graph is not currently committed. Dependency provenance and checksum metadata should be established from trusted upstream sources before that control is enabled.

## Changes that require security review

Review security and privacy impact before merging a change that adds or modifies:

- Android permissions
- exported activities, services, receivers, or providers
- shell commands or command arguments
- network access or remote SDKs
- analytics or telemetry
- Shizuku integration
- signing or release automation
- GitHub Actions permissions
- dependency repositories or artifact sources

Debug-signed APKs must never be published as production releases.

## Out of scope

The following are not security guarantees provided by this project:

- behavior of a compromised or modified Android operating system
- behavior of a compromised Shizuku installation
- availability or reputation decisions made by Google Play Protect
- OEM compatibility with undocumented SystemUI behavior
- persistence of locale changes across reboot or system updates
- APKs redistributed or modified by third-party mirrors

For product behavior and compatibility boundaries, see [PRODUCT.md](PRODUCT.md).
