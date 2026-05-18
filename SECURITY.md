# Security Policy

## Supported versions

Security fixes land on the latest released minor version. Older minors are
patched on a best-effort basis until a clear cadence is established.

| Version | Supported |
|---------|-----------|
| 0.x     | Yes (active development) |

## Reporting a vulnerability

Please **do not** open a public GitHub issue for security reports.

1. Use GitHub's private vulnerability reporting:
   <https://github.com/zeevy/android-calculator/security/advisories/new>
2. Include reproduction steps, affected versions, and the impact you observed.
3. We aim to acknowledge reports within **72 hours** and to publish a fix or
   mitigation within **30 days** for confirmed issues.

If you cannot use GitHub's private advisory flow, open an issue titled
`security: please email me` with a contact channel - do not include any
vulnerability details.

## Scope

In scope:

- The Android app (this repository)
- The build pipeline (CI workflows in `.github/workflows/`)
- Dependencies pinned in `gradle/libs.versions.toml`

Out of scope:

- Third-party services (currency rate APIs) we call - report those to their
  maintainers.
- Local issues that require physical device access and rooted Android.

## Disclosure

Once a fix is released, we credit reporters in the release notes unless they
prefer to remain anonymous.
