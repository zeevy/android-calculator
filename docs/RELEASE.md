# Release procedure

The Phase 11 cutover from "feature-complete on `main`" to "live on
Production track on Play". This doc is the runbook.

## Versioning

| Field | Strategy |
|---|---|
| `versionName` | Semantic version: `MAJOR.MINOR.PATCH`. e.g. `1.0.0`. |
| `versionCode` | Monotonically increasing integer. Convention: `MAJOR * 10000 + MINOR * 100 + PATCH`, so `1.0.0` → `10000`, `1.2.3` → `10203`, `2.0.0` → `20000`. |

Both live in [`app/build.gradle.kts`](../app/build.gradle.kts) under
`defaultConfig`. Bump them in the same commit as the release tag.

## Pre-flight checklist

Before pushing a `v*` tag:

- [ ] `main` is green on the [CI workflow](../.github/workflows/ci.yml).
- [ ] `:app:lintRelease` reports no errors.
- [ ] `./gradlew test` (all 250+ unit tests) green.
- [ ] `./gradlew :app:bundleRelease` builds. **AAB under 15 MB** (the
      most recent run came in at 5.1 MB - 3× headroom).
- [ ] `LoanCopyTest` passes (Play personal-loans wording guard).
- [ ] `CHANGELOG.md` has a section for the new version with the
      user-visible changes since the previous release.
- [ ] Privacy policy at [`PRIVACY.md`](../PRIVACY.md) reflects the
      shipping behaviour.
- [ ] Play Console listing copy at
      [`docs/PLAY_LISTING.md`](PLAY_LISTING.md) is up to date.

## Cutting the release

1. Bump `versionName` / `versionCode` in `app/build.gradle.kts`.
2. Add a `## [vX.Y.Z] - YYYY-MM-DD` block to `CHANGELOG.md`.
3. Commit as `chore(release): vX.Y.Z`. `main` is protected (PR plus one
   approving review), so this lands through a short-lived `release/vX.Y.Z`
   branch and a PR, not a direct push.
4. Tag: `git tag -s vX.Y.Z -m "Release vX.Y.Z"`. This needs a signing
   key in git config (`user.signingkey`); without one, use `-a` for a
   plain annotated tag.
5. Once the PR is merged, tag the merge commit on `main` and push the tag
   alone: `git push origin vX.Y.Z`. The tag push is what triggers the
   workflow.
6. The [Release workflow](../.github/workflows/release.yml) picks
   the tag up, builds the AAB + APK, and creates a draft GitHub
   release with both files attached.
7. Edit the draft release on GitHub: pull the
   "User-visible changes" bullets from `CHANGELOG.md`, attach the
   icon/feature graphic if it changed, publish.

## Play Console steps

(Manual - the Play Console API requires elevated credentials we
don't have in CI yet.)

1. Download the AAB from the GitHub release. It must be signed with the
   upload key first - see [Signing](#signing).
2. Play Console → **Internal testing** → **Create new release**.
3. Upload the AAB. Internal testing is gated to the Google account
   list on the testing track.
4. Smoke-test on at least one real device (Pixel 6a is the reference
   hardware).
5. Promote to **Closed beta** when comfortable. Closed beta has its
   own testers list - we run a 10-person beta for at least a week
   before promoting.
6. Promote to **Production** with a staged rollout: **10 % → 50 % →
   100 %**. Pause if the Play Console crash dashboard shows any
   crash rate above 0.5 %.

## Signing

**Release artifacts are currently UNSIGNED.** The `release` build type in
`app/build.gradle.kts` declares no `signingConfig`, so the workflow produces
`app-release-unsigned.apk` and an unsigned AAB. Two consequences:

- The APK **cannot be installed** on a device, so the sideload path below is
  not live yet.
- The AAB **cannot be uploaded to Play**, which rejects unsigned bundles. The
  Play Console steps above are blocked until an upload key is wired in.

So enrolling Play App Signing is the prerequisite for the first real
distribution, not a later nicety:

1. Generate the upload keystore locally (`keytool -genkey -v -keystore
   upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias
   upload`).
2. Add the keystore + alias + passwords as GitHub Actions secrets
   (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
   `KEY_PASSWORD`).
3. Update `app/build.gradle.kts` `release { signingConfig = ... }` to
   pull from these secrets when `CI=true`.
4. Upload the upload-keystore certificate to Play Console under
   **Setup → App signing**.

## Pre-launch checks Play runs automatically

The Play Console's pre-launch report runs every uploaded build on a
small device matrix. It catches:

- Native crashes within 60 s of cold start.
- Accessibility violations against AndroidX's framework.
- Policy-string issues (loan copy, sensitive permissions, etc.).

Treat any **error** result as a release blocker; **warning** results
are usually fine but worth eyeballing before promotion.

## Rollback

Play Console → **Releases overview** → **Halt rollout**. The previous
production version stays live for users who haven't picked up the new
one yet. A patch release (`vX.Y.(Z+1)`) reusing the previous tag's
codepath plus the fix is the recovery path.

## GitHub release for sideloaders

Every tagged release also attaches an APK to the GitHub release page, built
from the same commit as the AAB, so users who can't (or don't want to) install
from Play can sideload directly.

**Not live yet:** until signing is set up (see below) that asset is
`app-release-unsigned.apk`, and Android refuses to install an unsigned APK. If
you publish a release before then, say so in the notes rather than leaving
people to discover it.
