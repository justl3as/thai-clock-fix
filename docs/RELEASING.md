# Releasing Thai Clock Fix

Thai Clock Fix uses a two-stage release process so the APK tested on a physical device is the exact APK published to users.

## Release model

```text
main
  ↓
Release Candidate workflow
  ↓
production-signed APK
  ↓
signature + certificate verification
  ↓
Actions artifact
  ↓
physical-device E2E
  ↓
Publish Release workflow
  ↓
provenance + checksum + certificate verification
  ↓
Git tag + GitHub Release
```

The publish stage does not rebuild the APK.

## 1. Prepare main

Before creating a release candidate:

- intended changes are merged into `main`
- CI on `main` is green
- CodeQL on `main` is green
- code-scanning alerts have been reviewed
- the target version follows semantic versioning

For a patch release after `v1.0.0`, use `1.0.1`.

The version resolver maps:

```text
1.0.1 → versionName 1.0.1
      → versionCode 1000001
      → tag v1.0.1
```

## 2. Build a signed release candidate

Open **Actions → Release Candidate → Run workflow**.

Run it from:

```text
Branch: main
Version: 1.0.1
```

The workflow will:

1. verify that it is running from `main`
2. require successful `verify`, CodeQL Actions, and CodeQL Java/Kotlin checks for that exact `main` commit
3. resolve `versionName`, `versionCode`, and tag name
4. build with the production signing key
5. run formatting checks, unit tests, release lint, and the minified release build
6. read package name, `versionName`, and `versionCode` back from the APK
7. verify the APK signature
8. verify the production signing-certificate SHA-256
9. generate an APK SHA-256 checksum
10. generate `release-manifest.json`
11. upload the files as an Actions artifact

The artifact name is:

```text
ThaiClockFix-v1.0.1-rc
```

Release-candidate artifacts are retained for 14 days.

Record the **RC workflow run ID**. It is required for final publication.

## 3. Test the exact RC artifact

Download the artifact from the successful Release Candidate workflow run. Do not use a local rebuild for release approval.

Verify the downloaded files:

```text
ThaiClockFix-v1.0.1.apk
ThaiClockFix-v1.0.1.apk.sha256
release-manifest.json
```

Verify SHA-256:

```sh
sha256sum -c ThaiClockFix-v1.0.1.apk.sha256
```

On macOS:

```sh
shasum -a 256 ThaiClockFix-v1.0.1.apk
```

Compare the result with both the checksum file and `release-manifest.json`.

### Physical-device E2E checklist

Test the downloaded production-signed APK on a supported physical Android device.

- [ ] install or update from the current public release
- [ ] cold launch succeeds
- [ ] Shizuku installed but service stopped is shown as not ready
- [ ] permission flow while Shizuku is stopped does not crash or destabilize Shizuku
- [ ] start Shizuku and return to Thai Clock Fix
- [ ] permission-required state appears
- [ ] permission can be granted normally
- [ ] app reaches Shizuku ready state
- [ ] apply Thai Buddhist mode and verify SystemUI locale + 24-hour readback
- [ ] apply English mode and verify SystemUI locale + 24-hour readback
- [ ] restore SystemUI locale and verify the override is cleared
- [ ] stop/restart Shizuku once and verify the app recovers its connection state
- [ ] update path from the previous public APK succeeds without signing mismatch
- [ ] APK signer certificate matches the fingerprint in `SECURITY.md`

Do not publish if any release-blocking E2E item fails.

## 4. Publish the verified RC

After E2E passes, open **Actions → Publish Release → Run workflow**.

Run it from `main` and enter:

```text
Version: 1.0.1
RC run ID: <successful Release Candidate run ID>
E2E confirmed: checked
```

The E2E checkbox is an explicit maintainer acknowledgement that the physical-device checklist passed on the exact selected RC artifact. The publish workflow does not rebuild the app.

It verifies:

- the selected run is a successful **Release Candidate** workflow
- the RC was dispatched from `main`
- the requested version matches the RC manifest
- the manifest source commit matches the RC workflow source commit
- the RC source commit is still in `main` history
- APK SHA-256 matches the manifest and checksum file
- signing certificate matches `.github/release-signing-cert.sha256`
- the version tag and GitHub Release do not already exist

If all checks pass, it creates:

```text
v1.0.1
```

and publishes:

```text
ThaiClockFix-v1.0.1.apk
ThaiClockFix-v1.0.1.apk.sha256
release-manifest.json
```

The GitHub Release targets the exact source commit recorded by the RC workflow.

## 5. Post-release verification

Download the APK from the GitHub Release page and verify it again.

- [ ] published APK SHA-256 matches the approved RC
- [ ] published `release-manifest.json` matches the approved RC
- [ ] `apksigner verify --verbose --print-certs` succeeds
- [ ] signing certificate SHA-256 matches `SECURITY.md`
- [ ] versionName and versionCode are correct
- [ ] installation/update succeeds on the physical test device
- [ ] one final Thai → English → Restore smoke test passes

If the published bytes differ from the approved RC, treat it as a release failure.

## Release immutability

Final version tags and GitHub Releases are treated as immutable.

If a released APK is wrong, do not overwrite the existing version. Fix the issue and publish a new patch version instead.
