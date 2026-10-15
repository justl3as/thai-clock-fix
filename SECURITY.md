# Security Policy

## Supported releases

Only the latest GitHub Release is supported.

## Reporting a vulnerability

Do not open a public issue for a security vulnerability or include secrets, signing material, device identifiers, or private logs in an issue.

Use GitHub's private vulnerability reporting feature when it is available for this repository. If private reporting is unavailable, contact the repository owner privately through GitHub before sharing technical details.

## Repository security rules

- Production signing keys and passwords must never be committed.
- Release credentials live outside the repository and are injected through GitHub Actions Secrets.
- GitHub Actions must be pinned to immutable commit SHAs.
- Pull requests and release tags must pass formatting, unit tests, Android lint, and release/R8 builds.
- Public release APKs must be signed by the production key and verified with `apksigner`.
- Never publish debug APKs as releases.
- Do not add analytics, network services, SDKs, or permissions without reviewing their security and privacy impact first.
