# Release procedure

How a version goes from `main` to Google Play. The one-time Play setup comes first; after that every release is a tag, and the [Release workflow](../.github/workflows/release.yml) builds, signs and uploads it.

## Versioning

| Field | Strategy |
|---|---|
| `versionName` | Semantic version: `MAJOR.MINOR.PATCH`. e.g. `1.0.0`. |
| `versionCode` | Monotonically increasing integer. Convention: `MAJOR * 10000 + MINOR * 100 + PATCH`, so `1.0.0` → `10000`, `1.2.3` → `10203`, `2.0.0` → `20000`. |

Both live in [`app/build.gradle.kts`](../app/build.gradle.kts) under `defaultConfig`. Bump them in the same commit as the release tag. The workflow refuses a tag that does not match them.

## One-time setup for Google Play

| Step | Where | Result |
|---|---|---|
| 1. Make the upload key | Terminal | `calculator-upload.jks`, kept outside the repo |
| 2. Put the key in GitHub secrets | Terminal | The Release workflow signs the AAB and APK |
| 3. Create the app | Play Console | An empty app for `io.github.zeevy.calculator` |
| 4. App content and store listing | Play Console | Every policy form answered, listing ready |
| 5. First upload, by hand | Play Console | Play App Signing on, first release in review |
| 6. Production access | Play Console | Only for new personal developer accounts |
| 7. Publishing from CI | Google Cloud, Play Console, Terminal | Each tag uploads to Play by itself |
| 8. Play stats key (owner, optional) | Google Cloud, Play Console | Local stats and review replies |

### 1. Make the upload key

Run this once, outside the repo. `keytool` asks for a password and a name; with the default PKCS12 format the key password is the same as the store password.

```bash
mkdir -p ~/keys && cd ~/keys
keytool -genkeypair -v -keystore calculator-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

Keep a copy of the file and its password in a password manager. If the upload key is lost, Play support can reset it, but that takes a few days. The `.gitignore` already ignores `*.jks` and `*.keystore`.

### 2. Put the key in GitHub secrets

Each `gh secret set` without `--body` asks for the value at a hidden prompt.

```bash
REPO=zeevy/android-calculator
base64 -i ~/keys/calculator-upload.jks | gh secret set RELEASE_KEYSTORE_BASE64 -R $REPO
gh secret set RELEASE_KEYSTORE_PASSWORD -R $REPO
gh secret set RELEASE_KEY_PASSWORD -R $REPO
gh secret set RELEASE_KEY_ALIAS -R $REPO --body upload
gh secret list -R $REPO
```

[`app/build.gradle.kts`](../app/build.gradle.kts) signs the release build only when `RELEASE_KEYSTORE_PATH` is set, so local builds and forks stay unsigned and need no key. To sign on your own machine, set the same four `RELEASE_*` variables (with `RELEASE_KEYSTORE_PATH` pointing at the `.jks` file) and run `./gradlew :app:bundleRelease`.

### 3. Create the app

Play Console, Home, **Create app**:

| Field | Value |
|---|---|
| App name | `Calculator & Unit Converter` |
| Package name | `io.github.zeevy.calculator`, then **Check availability** |
| Default language | English (United States) - en-US |
| App or game | App |
| Free or paid | Free |
| Declarations | Tick the Developer Program Policies and US export laws boxes |

### 4. App content and store listing

Copy every answer from [PLAY_LISTING.md](PLAY_LISTING.md):

- **Policy and programs, App content**: the section "App content answers", one row per form. The Advertising ID and foreground service forms show up only after the first upload in step 5. The foreground service form needs a link to a short demo video. Record it on a phone or emulator with `adb shell screenrecord /sdcard/fgs.mp4` (Settings, Open over other apps, use the window over another app, pull down the notification, close with X), then upload it to YouTube as Unlisted.
- **Grow users, Store presence, Main store listing**: title, short and full description, icon, feature graphic and the 8 phone screenshots from [play/](play/), with the alt text from the table.
- **Store settings**: category, tags and contact details.

### 5. First upload, by hand

Google takes the first upload of a new app only through the Play Console. The Play upload job in the Release workflow stays off until step 7 sets `PLAY_TRACK`.

1. Cut the release as in [Cutting a release](#cutting-a-release). The workflow builds the signed AAB and attaches it to a draft GitHub release.
2. Download `app-release.aab` from that draft release.
3. Play Console, **Test and release**, **Testing**: open **Closed testing** if step 6 applies to your account, else **Internal testing**. Use the default track (the closed one is **Alpha**, which CI calls `alpha`; do not create a new one), add testers (an email list; press Enter after each address, and each one must be a Google account, the one signed in to Play on the tester's phone), then **Create new release**.
4. Play App Signing: keep **Use a Google-generated key**. The key that signed this AAB becomes the upload key.
5. Upload the AAB. Paste the release notes from `CHANGELOG.md` (max 500 characters). **Next**, then **Save and publish**. The first review can take a few days.

### 6. Production access (new personal accounts only)

Personal developer accounts made after 13 November 2023 must run a closed test before the first production release: at least 12 testers opted in for 14 days in a row. The Dashboard shows this as a checklist when it applies.

1. Share the closed test's opt-in link with at least 12 people. Each one opts in and installs from Play.
2. Keep them opted in for 14 days. A tester who leaves and comes back starts the 14 days again.
3. Dashboard, **Apply for production**, answer the questions about the test.
4. Once approved: **Production**, **Create new release**, or promote the closed testing release.

Organisation accounts and older personal accounts skip this: go to **Production**, **Create new release**.

### 7. Publishing from CI

1. Google Cloud console, the project used for Play (any project of yours): **APIs and services**, enable **Google Play Android Developer API**.
2. **IAM and admin**, **Service accounts**, **Create service account**: name `calculator-play-publish`, no roles. Open it, **Keys**, **Add key**, **JSON**. A key file downloads.
3. Play Console, **Users and permissions**, **Invite new users**: paste the service account email. Under **App permissions** add this app and tick **Release to production, exclude devices, and use Play App Signing** and **Release apps to testing tracks**. Invite.
4. Store the key and pick the track, then delete the downloaded file:

   ```bash
   REPO=zeevy/android-calculator
   gh secret set PLAY_SERVICE_ACCOUNT_JSON -R $REPO < ~/Downloads/<key-file>.json
   gh variable set PLAY_TRACK -R $REPO --body production
   rm ~/Downloads/<key-file>.json
   ```

`PLAY_TRACK` takes `internal`, `alpha` (the default closed testing track), `beta` (open testing) or `production`. Use `alpha` while the closed test of step 6 runs. With `production`, each release goes to all users once Google's review passes, the same as GrblController. Delete the variable to turn the upload off again.

If the Play account already has a publish service account for another app, skip 1 and 2: give that account access to this app in step 3 and add a new JSON key to it.

### 8. Play stats key (owner, optional)

A local script reads installs, daily users, crash and ANR rates, ratings and reviews, and posts review replies. It and its key live in `.config/`, which git ignores.

1. Google Cloud console, same project: enable **Google Play Developer Reporting API** too.
2. Create a service account `play-reader` with a JSON key, as in step 7.
3. Play Console, **Users and permissions**: invite it with **View app information and download bulk reports (read-only)** and **Reply to reviews**, for this app.
4. Save the key as `.config/play-reader.json` and run `chmod 600` on it.
5. Play Console, **Download reports**, **Statistics**, **Copy Cloud Storage URI**: the `pubsite_prod_...` part is the `BUCKET` in `.config/play_stats.py`.

A new service account can take up to 48 hours before the reports bucket lets it in.

## Pre-flight checklist

Before pushing a `v*` tag:

- [ ] `main` is green on the [CI workflow](../.github/workflows/ci.yml).
- [ ] `:app:lintRelease` reports no errors.
- [ ] `./gradlew test` (all unit tests) green.
- [ ] `./gradlew :app:bundleRelease` builds. **AAB under 15 MB** (the most recent run came in at 5.1 MB - 3× headroom).
- [ ] `LoanCopyTest` passes (Play personal-loans wording guard).
- [ ] If the Room schema version changed: the new `app/schemas/.../<version>.json` is committed, a `Migration` for it is in `CalculatorDatabase.MIGRATIONS`, and `CalculatorDatabaseMigrationTest` passes.
- [ ] `CHANGELOG.md` has a section for the new version with the user-visible changes since the previous release.
- [ ] Privacy policy at [`PRIVACY.md`](../PRIVACY.md) reflects the shipping behaviour.
- [ ] Play Console listing copy at [`docs/PLAY_LISTING.md`](PLAY_LISTING.md) and the images in [`docs/play/`](play/) are up to date.

## Cutting a release

1. Bump `versionName` / `versionCode` in `app/build.gradle.kts`.
2. Add a `## [X.Y.Z] - YYYY-MM-DD` block to `CHANGELOG.md`.
3. Commit as `chore(release): vX.Y.Z`. `main` is protected (PR plus one approving review), so this lands through a short-lived `release/vX.Y.Z` branch and a PR, not a direct push.
4. Once the PR is merged, tag the merge commit on `main`: `git tag -s vX.Y.Z -m "Release vX.Y.Z"`. This needs a signing key in git config (`user.signingkey`); without one, use `-a` for a plain annotated tag.
5. Push the tag alone: `git push origin vX.Y.Z`. The tag push starts the [Release workflow](../.github/workflows/release.yml):
   - `build-artifacts` builds the APK and AAB, signed with the upload key.
   - `publish-release` makes a draft GitHub release with both files.
   - `publish-play` uploads the AAB and the R8 mapping file to the `PLAY_TRACK` track, when that variable is set.
6. Edit the draft GitHub release: replace the generated commit list with the user-visible bullets from `CHANGELOG.md`, then publish it.

## After the upload to Play

1. Play Console, **Releases overview**: check the new release is in the right track and in review or live.
2. Watch the crash rate in **Android vitals** for a day. If it goes above 0.5 %, see [Rollback](#rollback). For a slow rollout instead, set `PLAY_TRACK` to `beta`, then promote the release to production by hand with a staged rollout (**10 % → 50 % → 100 %**).
3. Add release notes in the Play Console if the release needs them; the workflow uploads none.

## Pre-launch checks Play runs automatically

The Play Console's pre-launch report runs every uploaded build on a small device matrix. It catches:

- Native crashes within 60 s of cold start.
- Accessibility violations against AndroidX's framework.
- Policy-string issues (loan copy, sensitive permissions, etc.).

Treat any **error** result as a release blocker; **warning** results are usually fine but worth eyeballing before promotion.

## Rollback

Play Console → **Releases overview** → **Halt rollout**. The previous production version stays live for users who haven't picked up the new one yet. A patch release (`vX.Y.(Z+1)`) reusing the previous tag's codepath plus the fix is the recovery path.

## GitHub release for sideloaders

Every tagged release also attaches an APK to the GitHub release page, built from the same commit as the AAB, so users who can't (or don't want to) install from Play can sideload it.

That APK is signed with the upload key, while Play installs are signed with Google's app signing key. So a GitHub APK cannot update a Play install, and the other way round: moving between them needs an uninstall first, which deletes the app's history and settings. Say so in the release notes.
