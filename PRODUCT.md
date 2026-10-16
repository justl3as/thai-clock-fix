# Product specification

## Product summary

Thai Clock Fix is a focused Android utility for changing the owner user's SystemUI locale and 24-hour time through Shizuku.

The product exists because some Android builds format Thai SystemUI time with `น.` after the clock. Android exposes per-app locale controls, but applying them to SystemUI manually requires shell commands. Thai Clock Fix provides a small UI for the supported settings, verifies the resulting system state, and keeps the preview separate from measured device state.

## Target users

Primary users are Thai-speaking Android users who:

- use Android 13 or newer
- can install and start Shizuku
- want to change SystemUI clock/date formatting without root
- are using the Android owner profile (`user 0`)

The product assumes the user is comfortable sideloading an APK from GitHub Releases.

## User outcomes

A user should be able to:

1. understand whether Shizuku is installed, running, and authorized
2. choose one supported SystemUI locale mode
3. apply the mode and 24-hour time
4. see whether Android readback confirms the requested state
5. restore the SystemUI locale override to the system default
6. inspect equivalent ADB commands when manual operation is preferred

## Supported modes

| Mode | Locale tag | Calendar/display intent |
| --- | --- | --- |
| Thai Buddhist | `th-TH-u-ca-buddhist` | Thai locale with Buddhist calendar |
| English | `en-US` | US English locale with Gregorian calendar |
| Restore | empty override | Return SystemUI locale to the Android system default |

Applying either locale mode also writes `time_12_24=24`.

Restore clears only the SystemUI locale override. It does not restore the previous 12/24-hour setting.

## Product behavior

### Startup

On launch, the app:

1. initializes the Shizuku connection manager
2. loads the saved locale selection
3. observes Shizuku status
4. reads current SystemUI locale and 12/24-hour state when Shizuku is ready

Persisted preference state initializes the session. Once the user chooses a mode, a delayed persistence emission must not overwrite that live session choice.

### Apply

When the user applies a locale mode, the app:

1. verifies that Shizuku is ready
2. serializes the operation with other mutations and readbacks
3. writes 24-hour time
4. writes the selected SystemUI locale
5. reads the SystemUI locale back
6. reads the 12/24-hour setting back
7. reports success only when both values match the requested state

A partially completed or cancelled operation is treated as unverified until a later readback.

### Restore

Restore:

1. clears the SystemUI app-locale override
2. reads the locale back
3. reports success only when the override is confirmed empty

Restore does not change the current 12/24-hour setting.

### Readback

Readback and mutation share one operation queue. An older readback must not overwrite the result of a newer apply or restore operation.

## Product principles

### Measured state and preview are separate

The large clock in the app is a formatting preview. It must never be presented as proof of what the Android status bar is currently rendering.

Verified state comes from Android command readback.

### Success must be earned

The UI should only show an applied/verified state when the requested SystemUI locale matches and 24-hour time is confirmed.

Failures, cancellation, permission loss, and partial operations must clear verified state rather than simulating success.

### User interaction wins over stale persistence

A user's current-session selection takes precedence over delayed DataStore emissions. Persistence exists to restore the selection on the next launch, not to fight current UI interaction.

### Keep the scope narrow

The app changes only the settings required for its stated purpose. It does not attempt to manage unrelated SystemUI configuration.

### Use direct language

The interface should remain calm and explicit. Technical details should be available where useful, but normal operation should not require the user to understand ADB or Android locale internals.

## Non-goals

Thai Clock Fix is not intended to:

- root the device
- patch or replace SystemUI
- edit status-bar strings, fonts, or resources
- support arbitrary user-entered locale tags
- manage Android secondary users or work profiles
- restore the previous 12/24-hour value
- restart SystemUI or reboot the device automatically
- guarantee identical rendering across OEMs
- guarantee that an OEM will preserve the override after reboot or system update
- treat the in-app preview as visual verification of the real status bar

## Compatibility

Operational support starts at Android 13 because the product relies on Android's per-app locale command.

The APK currently has `minSdk = 26`, but versions below Android 13 are not part of the supported runtime product surface for SystemUI locale changes.

Shizuku v11 or newer is the intended integration baseline. The project currently builds against Shizuku 13.1.5.

OEM behavior may differ. A device can accept the commands but render SystemUI differently, or it can reset the locale override after reboot/update.

## Privacy and data handling

The product has no `INTERNET` permission and no analytics or telemetry SDK.

Local DataStore preferences keep only state needed for app behavior and diagnostics. Application backup is disabled.

See [SECURITY.md](SECURITY.md) for the full security boundary and release integrity policy.

## Accessibility

The UI should:

- preserve Compose accessibility semantics
- provide explicit Thai labels for interactive controls
- keep verification and error messages readable
- avoid communicating success or failure through color alone
- keep touch targets and Material components usable with standard Android accessibility services

## Release quality bar

A release is ready when:

- formatting checks pass
- unit tests pass
- Android release lint passes
- the minified/resource-shrunk release build succeeds
- CodeQL Actions and Java/Kotlin analysis pass
- a production-signed release candidate is built from `main`
- `apksigner` verifies the release-candidate APK and its signing identity
- the exact release-candidate APK installs and completes the supported Shizuku apply/readback flow on a real supported device
- the final release promotes that same verified APK without rebuilding it
- a SHA-256 checksum and provenance manifest are published with the APK

## Engineering constraints and technical debt

### Shizuku process API

Shell execution currently reaches the deprecated `Shizuku.newProcess` path through reflection. A future Shizuku major-version migration should move this responsibility to the supported UserService path before the legacy API disappears.

### Binder cancellation

The process runner avoids blocking on `Process.waitFor()`, limits output, and destroys the process on cancellation. Some synchronous Binder IPC involved in process creation may still be outside coroutine interruption guarantees.

### OEM persistence

SystemUI locale behavior is not a stable cross-OEM product contract. Reboot, firmware updates, or vendor code can reset the override.

### Dependency provenance

The project pins the Gradle Wrapper checksum and GitHub Actions SHAs. Full Gradle dependency-verification metadata for Maven artifacts is not yet enabled.

## Change checklist

A product change should update this document when it changes:

- supported Android versions
- supported locale modes
- owner-user scope
- success/readback criteria
- restore behavior
- privacy or network behavior
- Shizuku requirements
- release verification expectations
